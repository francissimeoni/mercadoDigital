import { test, before, after, afterEach } from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs/promises'
import path from 'node:path'
import { pathToFileURL } from 'node:url'
import { build } from 'esbuild'
import { JSDOM } from 'jsdom'

// React, router, hooks y DOM reales. Transporte, tenant y navegación externa simulados.
const dom = new JSDOM('<!doctype html><html><body><main id="root"></main></body></html>', { url: 'http://local.test.invalid/producto/p1?asesorId=ajeno&asesor=Falso&phone=123' })
for (const key of ['window', 'document', 'Element', 'HTMLElement', 'HTMLDialogElement', 'Event', 'MouseEvent']) Object.defineProperty(globalThis, key, { value: dom.window[key], configurable: true })
Object.defineProperty(globalThis, 'navigator', { value: dom.window.navigator, configurable: true })
window.scrollTo = () => {}
globalThis.requestAnimationFrame = callback => setTimeout(callback, 0)
globalThis.cancelAnimationFrame = clearTimeout
HTMLElement.prototype.scrollTo = () => {}
HTMLDialogElement.prototype.showModal = function () { this.open = true; this.querySelector('button')?.focus() }
HTMLDialogElement.prototype.close = function () { this.open = false }
globalThis.IS_REACT_ACT_ENVIRONMENT = true
const React = await import('react')
const { act } = React
const { createRoot } = await import('react-dom/client')
const { QueryClient, QueryClientProvider } = await import('@tanstack/react-query')
const { MemoryRouter, Routes, Route } = await import('react-router-dom')
let fixture, root, client, calls, navigation, handler, warnings = []
const originalConsoleError = console.error
const target = path.resolve('node_modules/.cache/catalogo-publico-tests/fixture.mjs')

before(async () => {
  console.error = (...args) => warnings.push(args.map(String).join(' '))
  await fs.mkdir(path.dirname(target), { recursive: true })
  await build({
    stdin: { contents: `export {default as ContactButton} from './src/storefront/components/CatalogoContactButton'; export {default as Card} from './src/storefront/components/StorefrontProductCard'; export {default as Detail} from './src/storefront/pages/StorefrontProductDetailPage'; export {default as Catalog} from './src/storefront/pages/StorefrontCatalogPage'; export {default as Gate} from './src/storefront/components/CatalogoCompraGate'; export * from './src/storefront/utils/catalogoWhatsApp'; export * from './src/storefront/utils/catalogoPublico';`, resolveDir: process.cwd(), loader: 'ts' },
    outfile: target, bundle: true, format: 'esm', platform: 'node', packages: 'external', jsx: 'automatic',
    loader: { '.jpeg': 'dataurl', '.png': 'dataurl', '.svg': 'dataurl' }, define: { 'import.meta.env': '{}' },
    plugins: [{ name: 'aislamiento-local', setup(b) {
      b.onResolve({ filter: /axiosClient$/ }, () => ({ path: 'transport', namespace: 'test' }))
      b.onResolve({ filter: /useTenant$/ }, () => ({ path: 'tenant', namespace: 'test' }))
      b.onResolve({ filter: /storefrontMetricsApi$/ }, () => ({ path: 'metrics', namespace: 'test' }))
      b.onLoad({ filter: /.*/, namespace: 'test' }, ({ path }) => ({ contents: path === 'tenant' ? 'export const useTenant=()=>globalThis.__tenant;' : path === 'metrics' ? 'export const trackStorefrontEvent=(...args)=>globalThis.__metrics.push(args); export const trackProductView=()=>{};' : 'export default {get:async(u,o)=>({data:await globalThis.__transport("get",u,o?.params,undefined,o)})};' }))
      b.onLoad({ filter: /catalogoWhatsApp\.ts$/ }, async ({ path }) => ({ contents: (await fs.readFile(path, 'utf8')).replace('window.location.assign(url)', 'globalThis.__navigate(url)'), loader: 'ts' }))
    } }],
  })
  fixture = await import(pathToFileURL(target).href)
})
afterEach(async () => {
  if (root) await act(async () => root.unmount())
  client?.clear(); root = undefined
  document.getElementById('root').replaceChildren()
  assert.deepEqual(warnings, [], 'Sin errores o advertencias React')
  warnings = []
})
after(() => { console.error = originalConsoleError; dom.window.close() })
const tick = () => new Promise(resolve => setTimeout(resolve, 15))
async function until(check) { for (let i = 0; i < 100; i++) { await act(async () => { await tick() }); if (check()) return } assert.fail(document.body.textContent) }
async function click(element) { assert.ok(element, 'Existe el control'); await act(async () => element.click()) }
const button = text => [...document.querySelectorAll('button')].find(b => b.textContent.includes(text))
const responsable = { id: 'responsable', nombre: 'Tienda del Sur', responsable: true, whatsapp: '+5491112345678' }
const asesor = { id: 'a1', nombre: 'Ana & equipo', responsable: false, whatsapp: '+5491112345679' }
const producto = { productoId: 'p1', nombre: 'Cafetera & café', sku: 'C-01', precioPublicoArs: '128000.25', condicionesEntrega: 'Coordinar entrega con la tienda.', storefrontHost: 'tienda.test.invalid', contactos: [responsable, asesor] }
const item = { productoId: 'p1', productoCanonicoId: null, nombrePublico: producto.nombre, categoriaProducto: 'Cocina', mejorPrecioFinalEstimado: 128000.25, stockDisponible: null, demoraEstimadaDias: null, imagenPrincipal: null, promocion: false, novedad: false, modoCatalogo: true, precioPublicoArs: producto.precioPublicoArs }
const tenant = () => ({ status: 'resolved', isLoading: false, host: 'tienda.test.invalid', revendedorId: 'r1', tipoStorefront: 'REVENDEDOR', storefront: { tipoStorefront: 'REVENDEDOR', revendedorId: 'r1', modoCatalogo: true, mercadoPagoHabilitado: true, mercadoPagoRecargoPorcentaje: 20 }, branding: { nombreComercial: 'Tienda del Sur', colorPrimario: '#0f172a', telefonoComercial: '+5491112345678', portadas: [] } })
async function mount(Component, props = {}, url = '/producto/p1?asesorId=ajeno&phone=123', tenantOverride = {}) {
  calls = []; navigation = []; globalThis.__metrics = []
  globalThis.__tenant = { ...tenant(), ...tenantOverride }
  globalThis.__transport = async (...args) => { calls.push(args); return handler(...args) }
  globalThis.__navigate = url => navigation.push(url)
  client = new QueryClient({ defaultOptions: { queries: { retry: false, gcTime: 0 }, mutations: { retry: false, gcTime: 0 } } })
  root = createRoot(document.getElementById('root'))
  const element = React.createElement(Component, props)
  await act(async () => root.render(React.createElement(QueryClientProvider, { client }, React.createElement(MemoryRouter, { initialEntries: [url] }, React.createElement(Routes, null,
    React.createElement(Route, { path: Component === fixture.Gate ? '/producto/:productoCanonicoId/checkout' : '/producto/:productoCanonicoId/*', element }),
    Component === fixture.Gate ? React.createElement(Route, { path: '/producto/:productoCanonicoId', element: React.createElement('p', null, 'Ficha pública') }) : null,
    React.createElement(Route, { path: '/catalogo', element }))))))
}
async function openContacts() {
  await mount(fixture.ContactButton, { productoId: 'p1', host: 'tienda.test.invalid', precioMostrado: producto.precioPublicoArs })
  await click(button('Comprar por WhatsApp'))
  await until(() => !document.body.textContent.includes('Actualizando'))
}

test('formatea y ordena importes grandes conservando centavos exactos', () => {
  assert.equal(fixture.formatPrecioArs('99999999999999999.99'), '$ 99.999.999.999.999.999,99')
  assert.equal(fixture.compararPrecios({ ...item, precioPublicoArs: '99999999999999999.98' }, { ...item, precioPublicoArs: '99999999999999999.99' }), -1)
})
test('mensaje conserva producto, SKU, precio y vista previa, sin destinatario de la URL', () => {
  const url = new URL(fixture.buildCatalogoWhatsAppUrl(producto, asesor))
  assert.equal(url.pathname, '/5491112345679')
  const message = url.searchParams.get('text')
  for (const part of [producto.nombre, 'C-01', '$ 128.000,25 ARS', 'medios de pago', 'entrega', 'storefrontHost=tienda.test.invalid', 'asesorId=a1', 'v=og2-128000-25']) assert.ok(message.includes(part), part)
  assert.ok(!message.includes('ajeno')); assert.ok(!message.includes('phone=123'))
  assert.throws(() => fixture.buildCatalogoWhatsAppUrl(producto, { ...asesor, whatsapp: '+5491112345680' }))
  assert.ok(!decodeURIComponent(fixture.buildCatalogoWhatsAppUrl(producto, responsable)).includes('asesorId'))
})
test('varios contactos: responsable primero, elección expresa y una sola apertura', async () => {
  handler = async () => structuredClone(producto)
  await openContacts()
  assert.equal(navigation.length, 0)
  const group = document.querySelector('[role="group"]')
  assert.match(group.querySelector('button').textContent, /Tienda del Sur.*Responsable de la tienda/)
  await click(button('Ana & equipo'))
  await until(() => navigation.length === 1)
  await click(button('Ana & equipo'))
  assert.equal(navigation.length, 1)
  assert.match(navigation[0], /^https:\/\/wa.me\/5491112345679\?/)
  assert.equal(globalThis.__metrics.length, 1)
  assert.ok(calls.every(c => c[0] === 'get' && c[1].endsWith('/contacto')))
  assert.ok(calls.every(c => JSON.stringify(c[2]) === JSON.stringify({ host: 'tienda.test.invalid' })))
})
test('un contacto funciona sin formulario ni autenticación', async () => {
  handler = async () => ({ ...producto, contactos: [responsable] })
  await openContacts()
  assert.equal(document.querySelectorAll('[role="group"] button').length, 1)
  assert.equal(document.querySelectorAll('input, form').length, 0)
  await click(button('Tienda del Sur'))
  await until(() => navigation.length === 1)
})
test('teclado recorre el diálogo en ambos sentidos sin enfocar controles del fondo', async () => {
  handler = async () => structuredClone(producto)
  await openContacts()
  const controls = [...document.querySelectorAll('dialog button')]
  controls.at(-1).focus()
  await act(async () => controls.at(-1).dispatchEvent(new window.KeyboardEvent('keydown', { key: 'Tab', bubbles: true, cancelable: true })))
  assert.equal(document.activeElement, controls[0])
  await act(async () => controls[0].dispatchEvent(new window.KeyboardEvent('keydown', { key: 'Tab', shiftKey: true, bubbles: true, cancelable: true })))
  assert.equal(document.activeElement, controls.at(-1))
})
test('cero contactos muestra estado claro y no abre ningún destino', async () => {
  handler = async () => ({ ...producto, contactos: [] })
  await openContacts()
  assert.match(document.body.textContent, /no tiene contactos/)
  assert.equal(document.querySelector('[role="group"]'), null)
  assert.equal(navigation.length, 0)
})
test('cambio de precio al elegir exige revisar y repetir la elección', async () => {
  let count = 0
  handler = async () => ({ ...producto, precioPublicoArs: ++count === 1 ? '128000.25' : '140000.00' })
  await openContacts(); await click(button('Ana & equipo'))
  await until(() => document.body.textContent.includes('El precio o el contacto cambió'))
  assert.equal(navigation.length, 0); assert.match(document.body.textContent, /140\.000,00/)
  await click(button('Ana & equipo')); await until(() => navigation.length === 1)
  assert.match(new URL(navigation[0]).searchParams.get('text'), /140\.000,00/)
})
test('asesor desactivado mientras se elige desaparece sin redirigir al propietario', async () => {
  let count = 0
  handler = async () => ({ ...producto, contactos: ++count === 1 ? producto.contactos : [responsable] })
  await openContacts(); await click(button('Ana & equipo'))
  await until(() => document.body.textContent.includes('El precio o el contacto cambió'))
  assert.equal(navigation.length, 0); assert.equal(button('Ana & equipo'), undefined)
})
test('error de transporte ofrece reintento y cerrar restaura foco y scroll', async () => {
  let count = 0
  handler = async () => { if (++count === 1) throw Error('sin red'); return producto }
  await openContacts()
  assert.match(document.body.textContent, /No pudimos cargar/)
  await click(button('Reintentar')); await until(() => Boolean(button('Ana & equipo')))
  await click(document.querySelector('[aria-label="Cerrar selector de contactos"]'))
  assert.equal(document.querySelector('dialog'), null)
  assert.equal(document.activeElement, button('Comprar por WhatsApp'))
  assert.equal(document.body.style.overflow, '')
})
test('cerrar durante una carga cancela la solicitud y no aplica respuesta atrasada', async () => {
  let resolve, signal
  handler = (...args) => { signal = args[4].signal; return new Promise(r => { resolve = r }) }
  await mount(fixture.ContactButton, { productoId: 'p1', host: 'tienda.test.invalid', precioMostrado: producto.precioPublicoArs })
  await click(button('Comprar por WhatsApp'))
  await until(() => Boolean(resolve))
  await click(document.querySelector('[aria-label="Cerrar selector de contactos"]'))
  assert.equal(signal.aborted, true)
  await act(async () => resolve(producto))
  assert.equal(document.querySelector('dialog'), null); assert.equal(navigation.length, 0)
})
test('tarjeta de catálogo con stock nulo muestra precio único y contacto sin checkout', async () => {
  handler = async () => producto
  await mount(fixture.Card, { item, tipoStorefront: 'REVENDEDOR' })
  assert.match(document.body.textContent, /128\.000,25/)
  assert.doesNotMatch(document.body.textContent, /Stock|Disponible|Sin stock/)
  assert.equal(document.querySelector('[href*="checkout"]'), null)
  await click(button('Comprar')); await until(() => Boolean(document.querySelector('dialog')))
})
test('ficha utiliza el mismo precio, condiciones y no muestra precio tarjeta ni stock', async () => {
  handler = async (method, url) => url.endsWith('/p1') ? { ...item, descripcion: 'Descripción', imagenes: [], condicionesEntrega: producto.condicionesEntrega } : [item]
  await mount(fixture.Detail)
  await until(() => document.body.textContent.includes('128.000,25'))
  assert.doesNotMatch(document.body.textContent, /tarjeta de crédito|Disponibilidad|En stock|Sin stock|contado o transferencia/)
  assert.match(document.body.textContent, /Coordinar entrega/)
  assert.equal(document.querySelector('[href*="checkout"],[href*="resumen-compra"]'), null)
  assert.ok(calls.every(c => c[2].host === 'tienda.test.invalid' && !('revendedorId' in c[2])))
})
test('filtro y orden de stock antiguos no afectan al catálogo', async () => {
  handler = async () => [item]
  await mount(fixture.Catalog, {}, '/catalogo?stock=1&sort=stock-desc')
  await until(() => document.body.textContent.includes(producto.nombre))
  assert.doesNotMatch(document.body.textContent, /Solo disponibles|Mayor disponibilidad|Sin stock/)
  assert.equal(document.querySelector('[value="stock-desc"]'), null)
})
test('modo desactivado conserva tarjeta anterior y ruta de checkout', async () => {
  handler = async () => []
  await mount(fixture.Card, { item: { ...item, modoCatalogo: false, stockDisponible: 3 }, tipoStorefront: 'REVENDEDOR' }, undefined, { storefront: { modoCatalogo: false } })
  assert.match(document.body.textContent, /Stock 3/)
  assert.ok(document.querySelector('[href*="checkout"]'))
})
test('gate de catálogo impide montar efectos del checkout', async () => {
  let montado = 0
  const Child = () => { montado++; return React.createElement('p', null, 'checkout legado') }
  handler = async () => []
  await mount(fixture.Gate, { children: React.createElement(Child) }, '/producto/p1/checkout')
  assert.equal(montado, 0)
  assert.doesNotMatch(document.body.textContent, /checkout legado/)
})
test('gate desactivado permite el recorrido anterior', async () => {
  handler = async () => ({ tipoStorefront: 'REVENDEDOR', revendedorId: 'r1', modoCatalogo: false })
  await mount(fixture.Gate, { children: React.createElement('p', null, 'checkout legado') }, '/producto/p1/checkout', { storefront: { modoCatalogo: false } })
  await until(() => document.body.textContent.includes('checkout legado'))
  assert.match(document.body.textContent, /checkout legado/)
})
