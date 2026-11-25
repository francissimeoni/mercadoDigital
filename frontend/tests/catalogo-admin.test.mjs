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
globalThis.IS_REACT_ACT_ENVIRONMENT = true
const React = await import('react')
const { act } = React
const { createRoot } = await import('react-dom/client')
const { QueryClient, QueryClientProvider } = await import('@tanstack/react-query')
let fixture, root, client, calls, handler
let reactWarnings = []
const originalConsoleError = console.error
const target = path.resolve('node_modules/.cache/catalogo-tests/fixture.mjs')
before(async () => {
  console.error = (...args) => { reactWarnings.push(args.map(String).join(' ')) }
  await fs.mkdir(path.dirname(target), { recursive: true })
  await build({
    stdin: { contents: `export { default as ProductEditor } from './src/storefront/components/CatalogoProductPriceEditor'; export { default as AdminSection } from './src/storefront/components/CatalogoAdminSection'; export { confirmCatalogoLeave } from './src/storefront/hooks/useCatalogoDraft';`, resolveDir: process.cwd(), loader: 'ts' },
    outfile: target, bundle: true, format: 'esm', platform: 'node', packages: 'external', jsx: 'automatic',
    plugins: [{ name: 'local-transport', setup(b) {
      b.onResolve({ filter: /storefrontOwnerClient$/ }, () => ({ path: 'local-transport', namespace: 'test' }))
      b.onLoad({ filter: /.*/, namespace: 'test' }, () => ({ contents: `const call = async (method,url,data,options) => ({data:await globalThis.__catalogoTransport(method,url,data,options)}); export default {get:(u,o)=>call('get',u,undefined,o),post:(u,d,o)=>call('post',u,d,o),put:(u,d,o)=>call('put',u,d,o),delete:(u,o)=>call('delete',u,undefined,o)};` }))
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
const button = text => [...document.querySelectorAll('button')].find(el => el.textContent.includes(text))
async function click(element) { assert.ok(element); await act(async () => element.click()) }
const price = (total = 120000) => ({ estado: 'LISTO', pendientes: [], monedaOriginal: 'USD', precioOriginal: 100, cotizacionUsd: 1200, baseArs: total, envioArs: 0, precioPublicoArs: total })
const product = { productoId: 'p1', nombre: 'Auriculares', sku: 'A1', moneda: 'USD', precioOriginal: 100, incluyeEnvio: true, categoriaEnvioId: null, version: 0, calculo: price() }
const productProps = extra => ({ id: 't1', product, categories: [], configVersion: 0, refresh: async () => {}, notify: () => {}, ...extra })

test('vista previa usa backend, presenta desglose y guarda con token sin stock', async () => {
  handler = async method => method === 'post' ? { calculo: price(), token: 'preview-1' } : product
  await mount(fixture.ProductEditor, productProps())
  await until(() => document.body.textContent.includes('120.000'))
  assert.match(document.body.textContent, /Cotización de la tienda/)
  assert.match(document.body.textContent, /Envío adicional/)
  assert.equal(document.querySelectorAll('input').length, 1)
  assert.equal(document.querySelector('[name="stock"]'), null)
  await click(button('Guardar precio'))
  await until(() => calls.some(c => c[0] === 'put'))
  const saved = calls.find(c => c[0] === 'put')[2]
  assert.equal(saved.previewToken, 'preview-1'); assert.equal(saved.precioOriginal, '100')
  assert.equal(saved.stock, undefined); assert.equal(saved.precioPublicoArs, undefined)
})

test('una respuesta atrasada no reemplaza el precio del borrador nuevo', async () => {
  let resolveFirst
  handler = async (method, url, data) => data.precioOriginal === '100' ? new Promise(resolve => { resolveFirst = resolve }) : { calculo: price(240000), token: 'nuevo' }
  await mount(fixture.ProductEditor, productProps())
  await until(() => Boolean(resolveFirst))
  await change(document.querySelector('input[type="number"]'), '200')
  await until(() => document.body.textContent.includes('240.000'))
  await act(async () => { resolveFirst({ calculo: price(), token: 'viejo' }); await tick() })
  assert.match(document.body.textContent, /240\.000/)
  assert.doesNotMatch(document.body.textContent, /120\.000/)
})

test('envío obligatorio al excluirlo; volver a incluido descarta la categoría', async () => {
  handler = async () => ({ calculo: price(), token: 'preview' })
  await mount(fixture.ProductEditor, productProps({ categories: [{ id: 'c1', nombre: 'Mediano', costoArs: 8000, version: 0 }] }))
  await until(() => calls.length > 0)
  await change(document.querySelectorAll('select')[1], 'false')
  const category = document.querySelectorAll('select')[2]
  assert.equal(category.required, true); assert.equal(button('Guardar precio').disabled, true)
  await change(category, 'c1')
  await until(() => calls.some(c => c[2]?.categoriaEnvioId === 'c1'))
  await change(document.querySelectorAll('select')[1], 'true')
  await until(() => calls.at(-1)?.[2]?.incluyeEnvio === true)
  assert.equal(document.querySelectorAll('select').length, 2)
  assert.equal(calls.at(-1)[2].categoriaEnvioId, null)
})

test('datos pendientes no muestran precio ficticio ni permiten guardar', async () => {
  handler = async () => { throw Error('No debe consultarse un borrador incompleto') }
  await mount(fixture.ProductEditor, productProps({ product: { ...product, moneda: null, precioOriginal: null, incluyeEnvio: null, version: -1 } }))
  assert.match(document.body.textContent, /Completá moneda, precio y envío/)
  assert.equal(button('Guardar precio').disabled, true); assert.equal(calls.length, 0)
})

test('cambio de cotización exige revisar impacto y una edición invalida la confirmación', async () => {
  const config = { cotizacionUsd: 1000, condicionesEntrega: 'Zona local', telefonoComercial: '5491112345678', version: 0, modo: 'PREPARACION', pendientes: [] }
  handler = async (method, url, data) => {
    if (method === 'get') return url.endsWith('/productos') ? [] : url.endsWith('/categorias') ? [] : config
    if (method === 'post') return { productos: [{ productoId: 'p1', nombre: 'Auriculares', anterior: price(100000), propuesto: price(120000) }], token: 'impacto-' + data.cotizacionUsd }
    return { ...config, cotizacionUsd: 1200, version: 1 }
  }
  await mount(fixture.AdminSection, { id: 't1', notify: () => {} })
  await until(() => document.querySelector('input[aria-label="Pesos por 1 USD"]'))
  await change(document.querySelector('input[aria-label="Pesos por 1 USD"]'), '1100')
  await click(button('Revisar cambios'))
  await until(() => button('Confirmar y guardar configuración'))
  assert.equal(calls.filter(c => c[0] === 'put').length, 0)
  await change(document.querySelector('input[aria-label="Pesos por 1 USD"]'), '1200')
  assert.equal(button('Confirmar y guardar configuración'), undefined)
  await click(button('Revisar cambios'))
  await until(() => button('Confirmar y guardar configuración'))
  await click(button('Confirmar y guardar configuración'))
  await until(() => calls.some(c => c[0] === 'put'))
  assert.equal(calls.find(c => c[0] === 'put')[2].impactoToken, 'impacto-1200.000000')
})

test('el borrador protege la salida y elimina la protección al desmontarse', async () => {
  handler = async () => ({ calculo: price(), token: 'preview' })
  await mount(fixture.ProductEditor, productProps())
  await change(document.querySelector('input[type="number"]'), '200')
  window.confirm = () => false
  assert.equal(fixture.confirmCatalogoLeave(), false)
  window.confirm = () => true
  assert.equal(fixture.confirmCatalogoLeave(), true)
  await act(async () => root.unmount()); root = undefined
  window.confirm = () => { throw Error('No debe preguntar sin borradores') }
  assert.equal(fixture.confirmCatalogoLeave(), true)
})

test('cotización se muestra con dos decimales y conserva la precisión guardada cuando no se modifica', async () => {
  const config = { cotizacionUsd: '1730.123456', condicionesEntrega: 'Zona local', telefonoComercial: '5491112345678', version: 0, modo: 'PREPARACION', pendientes: [] }
  handler = async (method, url, data) => method === 'get' ? url.endsWith('/productos') || url.endsWith('/categorias') ? [] : config : method === 'post' ? { productos: [], token: 'revision' } : { ...config, ...data, version: 1 }
  await mount(fixture.AdminSection, { id: 't1', notify: () => {}, view: 'pricing' })
  await until(() => document.querySelector('input[aria-label="Pesos por 1 USD"]'))
  assert.equal(document.querySelector('input[aria-label="Pesos por 1 USD"]').value, '1730.12')
  await click(button('Revisar cambios'))
  await until(() => button('Confirmar y guardar configuración'))
  assert.equal(calls.find(c => c[0] === 'post')[2].cotizacionUsd, '1730.123456')
  await click(button('Confirmar y guardar configuración'))
  await until(() => calls.some(c => c[0] === 'put'))
  assert.equal(calls.find(c => c[0] === 'put')[2].cotizacionUsd, '1730.123456')
  await until(() => document.querySelector('input[aria-label="Pesos por 1 USD"]').value === '1730.12')
})

test('cotización nueva admite coma con dos decimales y rechaza precisión adicional antes de consultar', async () => {
  const config = { cotizacionUsd: '1730.000000', condicionesEntrega: '', telefonoComercial: '', version: 0, modo: 'PREPARACION', pendientes: [] }
  handler = async (method, url, data) => method === 'get' ? url.endsWith('/productos') || url.endsWith('/categorias') ? [] : config : method === 'post' ? { productos: [], token: 'revision' } : { ...config, ...data, version: 1 }
  await mount(fixture.AdminSection, { id: 't1', notify: () => {}, view: 'pricing' })
  await until(() => document.querySelector('input[aria-label="Pesos por 1 USD"]'))
  const input = document.querySelector('input[aria-label="Pesos por 1 USD"]')
  assert.equal(input.value, '1730.00')
  await change(input, '1730.125'); await click(button('Revisar cambios'))
  await until(() => document.body.textContent.includes('hasta 2 decimales'))
  assert.equal(calls.filter(c => c[0] === 'post').length, 0)
  await change(input, '1730,25'); await click(button('Revisar cambios'))
  await until(() => button('Confirmar y guardar configuración'))
  assert.equal(calls.find(c => c[0] === 'post')[2].cotizacionUsd, '1730.250000')
  await click(button('Confirmar y guardar configuración'))
  await until(() => input.value === '1730.25')
})

test('recibir datos nuevos conserva el borrador y su versión original', async () => {
  handler = async () => ({ calculo: price(), token: 'preview' })
  await mount(fixture.ProductEditor, productProps())
  await change(document.querySelector('input[type="number"]'), '200')
  await act(async () => root.render(React.createElement(QueryClientProvider, { client }, React.createElement(fixture.ProductEditor,
    productProps({ product: { ...product, precioOriginal: 300, version: 1 } })))))
  assert.equal(document.querySelector('input[type="number"]').value, '200')
  await until(() => calls.some(c => c[2]?.precioOriginal === '200'))
  assert.equal(calls.at(-1)[2].version, 0)
})
