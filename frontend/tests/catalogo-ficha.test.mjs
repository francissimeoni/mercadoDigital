import { test, before, after, afterEach } from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs/promises'
import path from 'node:path'
import { pathToFileURL } from 'node:url'
import { build } from 'esbuild'
import { JSDOM } from 'jsdom'

// DOM y hooks reales; se simula exclusivamente el transporte de la API privada.
// No establece conexiones HTTP ni utiliza sesiones o servicios reales.
const dom = new JSDOM('<!doctype html><html><body><div id="root"></div></body></html>', { url: 'http://catalogo.test.invalid/' })
for (const key of ['window', 'document', 'Element', 'HTMLElement', 'HTMLAnchorElement', 'HTMLInputElement', 'HTMLSelectElement', 'Event', 'MouseEvent', 'BeforeUnloadEvent']) {
  if (dom.window[key]) Object.defineProperty(globalThis, key, { value: dom.window[key], configurable: true })
}
Object.defineProperty(globalThis, 'navigator', { value: dom.window.navigator, configurable: true })
window.requestAnimationFrame = callback => window.setTimeout(callback, 0)
HTMLElement.prototype.scrollIntoView = () => {}
globalThis.IS_REACT_ACT_ENVIRONMENT = true
const React = await import('react')
const { act } = React
const { createRoot } = await import('react-dom/client')
const { QueryClient, QueryClientProvider } = await import('@tanstack/react-query')
let fixture, root, client, calls, handler
let reactWarnings = []
const originalConsoleError = console.error
const target = path.resolve('node_modules/.cache/catalogo-ficha-tests/fixture.mjs')
before(async () => {
  console.error = (...args) => { reactWarnings.push(args.map(String).join(' ')) }
  await fs.mkdir(path.dirname(target), { recursive: true })
  await build({
    stdin: { contents: `export { default as Editor } from './src/storefront/components/CatalogoProductEditor'; export { default as Section } from './src/storefront/components/CatalogoAdminSection'; export { decimalCatalogo, dineroCatalogo } from './src/storefront/utils/catalogoDecimal';`, resolveDir: process.cwd(), loader: 'ts' },
    outfile: target, bundle: true, format: 'esm', platform: 'node', packages: 'external', jsx: 'automatic',
    plugins: [{ name: 'local-transport', setup(b) {
      b.onResolve({ filter: /storefrontOwnerClient$/ }, () => ({ path: 'local-transport', namespace: 'test' }))
      b.onLoad({ filter: /.*/, namespace: 'test' }, () => ({ contents: `const call = async (method,url,data,options) => ({data:await globalThis.__catalogoTransport(method,url,data,options)}); export default {get:(u,o)=>call('get',u,undefined,o),post:(u,d,o)=>call('post',u,d,o),put:(u,d,o)=>call('put',u,d,o),delete:(u,o)=>call('delete',u,undefined,o),request:config=>call(config.method,config.url,config.data,config)};` }))
    } }],
  })
  fixture = await import(pathToFileURL(target).href)
})
afterEach(async () => {
  if (root) await act(async () => root.unmount())
  client?.clear(); root = undefined
  document.getElementById('root').replaceChildren()
  assert.deepEqual(reactWarnings, [], 'El renderizado no debe emitir errores o advertencias de React')
  reactWarnings = []
})
after(() => { console.error = originalConsoleError; dom.window.close() })

const tick = () => new Promise(resolve => setTimeout(resolve, 20))
async function until(check) {
  for (let i = 0; i < 100; i++) { await act(async () => { await tick() }); if (check()) return }
  assert.fail('No se alcanzó el estado esperado: ' + document.body.textContent)
}
async function mount(Component, props) {
  calls = []
  globalThis.__catalogoTransport = async (...args) => { calls.push(args); return handler(...args) }
  client = new QueryClient({ defaultOptions: { queries: { retry: false, gcTime: 0 }, mutations: { retry: false, gcTime: 0 } } })
  root = createRoot(document.getElementById('root'))
  await act(async () => root.render(React.createElement(QueryClientProvider, { client }, React.createElement(Component, props))))
}
async function change(element, value) {
  assert.ok(element, 'Existe el campo')
  await act(async () => {
    const proto = element.tagName === 'SELECT' ? HTMLSelectElement.prototype : HTMLInputElement.prototype
    Object.getOwnPropertyDescriptor(proto, 'value').set.call(element, value)
    element.dispatchEvent(new Event(element.tagName === 'SELECT' ? 'change' : 'input', { bubbles: true }))
  })
}
const saveButton = () => [...document.querySelectorAll('button')].find(el => /^(Crear producto|Guardar cambios)$/.test(el.textContent))
const button = text => [...document.querySelectorAll('button')].find(el => el.textContent.includes(text))
async function click(element) { assert.ok(element); await act(async () => element.click()) }

const price = (total = '128000.00') => ({ estado: 'LISTO', pendientes: [], monedaOriginal: 'USD', precioOriginal: '100.00', cotizacionUsd: '1200.000000', baseArs: '120000.00', envioArs: '8000.00', precioPublicoArs: total })
const config = { cotizacionUsd: '1200.000000', condicionesEntrega: 'Zona local', telefonoComercial: '+5491100000000', version: 0, modo: 'PREPARACION', pendientes: [] }
const category = { id: 'c1', nombre: 'Mediano', costoArs: '8000.00', version: 0 }
const datos = { id: 'p1', sku: 'SKU-1', nombre: 'Artículo', descripcion: '', categoriaProductoId: null, activo: true, promocion: false, novedad: false, media: [], precioVenta: null, stock: null, categoriaPaquete: null }
const producto = { productoId: 'p1', nombre: 'Artículo', sku: 'SKU-1', moneda: 'USD', precioOriginal: '100.00', incluyeEnvio: false, categoriaEnvioId: 'c1', version: 0, calculo: price(), activo: true }
const props = extra => ({ id: 't1', categories: [category], productCategories: [], config, refresh: async () => {}, notify: () => {}, onNew: () => {}, ...extra })
const input = text => [...document.querySelectorAll('label')].find(e => e.textContent.startsWith(text))?.querySelector('input,select')
async function complete() {
  await change(input('SKU'), 'SKU-NUEVO'); await change(input('Nombre del producto'), 'Artículo nuevo')
  await change(input('Moneda del artículo'), 'USD'); await change(input('Precio de venta'), '100,00')
  await change(input('¿El precio ya incluye'), 'false'); await change(input('Categoría de envío'), 'c1')
  await until(() => document.body.textContent.includes('128.000'))
}

test('alta unificada envía precio exacto, envío explícito y una sola operación sin valores heredados', async () => {
  let body
  handler = async (method, url, data) => {
    if (data instanceof FormData) { body = JSON.parse(await data.get('ficha').text()); return { datos, precio: producto, versionFicha: 0 } }
    return { calculo: price(), token: 'ficha-revisada' }
  }
  await mount(fixture.Editor, props()); await complete()
  assert.equal(document.body.textContent.includes('Precio anterior'), false)
  await click(saveButton())
  await until(() => Boolean(body))
  assert.equal(body.precio.precioOriginal, '100.00'); assert.equal(body.precio.categoriaEnvioId, 'c1')
  assert.equal(body.previewToken, 'ficha-revisada'); assert.equal(body.precioVenta, undefined); assert.equal(body.stock, undefined)
  assert.equal(body.categoriaPaquete, undefined); assert.equal(body.versionFicha, -1)
  assert.equal(calls.filter(c => c[2] instanceof FormData).length, 1)
})

test('fallo de guardado conserva borrador y clave de alta para un reintento seguro', async () => {
  const submissions = []
  handler = async (method, url, data) => {
    if (data instanceof FormData) { submissions.push(JSON.parse(await data.get('ficha').text())); throw Error('Conexión interrumpida') }
    return { calculo: price(), token: 'revisado' }
  }
  await mount(fixture.Editor, props()); await complete(); await click(saveButton())
  await until(() => document.body.textContent.includes('Conexión interrumpida'))
  assert.equal(input('SKU').value, 'SKU-NUEVO'); assert.equal(input('Precio de venta').value, '100,00')
  await click(saveButton()); await until(() => submissions.length === 2)
  assert.equal(submissions[0].operacionId, submissions[1].operacionId)
})

test('cambiar a envío incluido descarta vínculo y una respuesta vieja no permite guardar', async () => {
  let resolveOld
  handler = async (method, url, data) => data.precio.incluyeEnvio ? { calculo: { ...price('120000.00'), envioArs: '0.00' }, token: 'incluido' }
    : new Promise(resolve => { resolveOld = resolve })
  await mount(fixture.Editor, props({ initial: { datos, precio: producto, versionFicha: 5 } }))
  await until(() => Boolean(resolveOld))
  assert.equal(saveButton().disabled, true)
  await change(input('¿El precio ya incluye'), 'true')
  await until(() => document.body.textContent.includes('120.000'))
  await act(async () => { resolveOld({ calculo: price(), token: 'viejo' }); await tick() })
  assert.doesNotMatch(document.body.textContent, /128\.000/)
  assert.equal(calls.at(-1)[2].precio.categoriaEnvioId, null)
})

test('recarga de categorías y cotización conserva texto y versión de ficha', async () => {
  handler = async () => ({ calculo: price(), token: 'revisado' })
  await mount(fixture.Editor, props({ initial: { datos, precio: producto, versionFicha: 5 } }))
  await change(input('Nombre del producto'), 'Borrador conservado')
  await act(async () => root.render(React.createElement(QueryClientProvider, { client }, React.createElement(fixture.Editor,
    props({ initial: { datos: { ...datos, nombre: 'Edición remota' }, precio: producto, versionFicha: 6 }, config: { ...config, version: 1 } })))))
  assert.equal(input('Nombre del producto').value, 'Borrador conservado')
  await until(() => calls.some(c => c[2]?.nombre === 'Borrador conservado'))
  assert.equal(calls.at(-1)[2].versionFicha, 5)
})

test('cotización pendiente impide guardar y ofrece acceso a su configuración', async () => {
  handler = async () => ({ calculo: { ...price(), estado: 'PENDIENTE', pendientes: ['Configurá la cotización del dólar de esta tienda.'], precioPublicoArs: null }, token: 'pendiente' })
  await mount(fixture.Editor, props({ initial: { datos, precio: producto, versionFicha: 0 }, config: { ...config, cotizacionUsd: null } }))
  await until(() => document.body.textContent.includes('Configurá la cotización del dólar'))
  assert.equal(saveButton().disabled, true)
  assert.ok(button('Administrar cotización'))
})

test('fallo al refrescar después de guardar no informa un guardado fallido ni repite alta', async () => {
  handler = async (method, url, data) => data instanceof FormData ? { datos, precio: producto, versionFicha: 0 } : { calculo: price(), token: 'revisado' }
  await mount(fixture.Editor, props({ refresh: async () => { throw Error('Sin conexión') } }))
  await complete(); await click(saveButton())
  await until(() => document.body.textContent.includes('El producto quedó guardado'))
  assert.equal(calls.filter(c => c[2] instanceof FormData).length, 1)
})

test('importes argentinos mantienen centavos exactos y rechazan agrupadores ambiguos', () => {
  assert.equal(fixture.decimalCatalogo('99999999999999999,99'), '99999999999999999.99')
  assert.equal(fixture.dineroCatalogo('99999999999999999.99'), '$ 99.999.999.999.999.999,99')
  assert.equal(fixture.decimalCatalogo('1.200,50'), null)
  assert.equal(fixture.decimalCatalogo('1.200'), null)
  assert.equal(fixture.decimalCatalogo('1200,123456', 6), '1200.123456')
})

test('editar categoría propia muestra vinculados, revisa incremento y conserva el producto en preparación', async () => {
  let cost = '8000.00', changed = false
  handler = async (method, url, data) => {
    if (method === 'get') return url.endsWith('/catalogo-config') ? config : url.endsWith('/productos') ? [producto]
      : url.endsWith('/categorias') ? [{ ...category, costoArs: cost, version: changed ? 1 : 0 }] : []
    if (url.includes('/fichas')) return { calculo: price(changed ? '130000.00' : '128000.00'), token: 'ficha' }
    if (method === 'post') return { productos: [{ productoId: 'p1', nombre: 'Artículo', anterior: price(), propuesto: price('130000.00') }], token: 'impacto-envio' }
    assert.equal(data.impactoToken, 'impacto-envio'); assert.equal(data.costoArs, '10000.00')
    cost = data.costoArs; changed = true; return { ...category, costoArs: cost, version: 1 }
  }
  await mount(fixture.Section, { id: 't1', notify: () => {} })
  await until(() => document.body.textContent.includes('1 producto(s) vinculados'))
  await click(button('Agregar producto')); await until(() => Boolean(input('SKU')))
  await complete()
  window.confirm = () => { throw Error('La edición de la categoría no debe descartar el producto') }
  await click([...document.querySelectorAll('button')].find(b => b.textContent === 'Editar'))
  await change(document.querySelector('[aria-label="Costo en ARS"]'), '10000,00')
  await click(button('Revisar impacto en precios'))
  await until(() => Boolean(button('Confirmar y guardar categoría')))
  assert.match(document.body.textContent, /128\.000/); assert.match(document.body.textContent, /130\.000/)
  await click(button('Confirmar y guardar categoría'))
  await until(() => changed && document.querySelector('#catalogo-ficha').textContent.includes('130.000'))
  assert.equal(input('SKU').value, 'SKU-NUEVO'); assert.equal(input('Precio de venta').value, '100,00')
})
