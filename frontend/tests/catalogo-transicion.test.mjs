import { test, before, after, afterEach } from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs/promises'
import path from 'node:path'
import { pathToFileURL } from 'node:url'
import { build } from 'esbuild'
import { JSDOM } from 'jsdom'

const dom = new JSDOM('<!doctype html><html><body><main id="root"></main></body></html>', { url: 'http://tienda.test.invalid/' })
for (const key of ['window', 'document', 'Element', 'HTMLElement', 'Event', 'MouseEvent']) Object.defineProperty(globalThis, key, { value: dom.window[key], configurable: true })
Object.defineProperty(globalThis, 'navigator', { value: dom.window.navigator, configurable: true })
window.scrollTo = () => {}; HTMLElement.prototype.scrollTo = () => {}; HTMLElement.prototype.scrollIntoView = () => {}
window.requestAnimationFrame = callback => window.setTimeout(callback, 0)
HTMLElement.prototype.scrollIntoView = () => {}
globalThis.IS_REACT_ACT_ENVIRONMENT = true
const React = await import('react'), { act } = React
const { createRoot } = await import('react-dom/client')
const { QueryClient, QueryClientProvider } = await import('@tanstack/react-query')
const { MemoryRouter, Routes, Route, useLocation } = await import('react-router-dom')
let fixture, root, client, calls, handler, warnings = []
const originalError = console.error
const baseTenant = (mode = true) => ({ status: 'resolved', isLoading: false, host: 'tienda.test.invalid', tipoStorefront: 'REVENDEDOR', revendedorId: 'r1', proveedorId: null, storefront: { tipoStorefront: 'REVENDEDOR', revendedorId: 'r1', modoCatalogo: mode }, branding: { nombreComercial: 'Tienda del Sur', colorPrimario: '#0f5f53', portadas: [], quienesGaleria: [] }, refresh: async () => {} })
const order = { pedidoId: 'pedido-anterior', trackingToken: 'tracking-anterior', estadoPedido: 'CREADO', estadoPago: 'PENDIENTE', metodoPago: 'PSP', totalFinal: 123.45, items: [], envio: { estadoEnvio: 'CREADO' } }
const customer = { clienteId: 'c1', roles: ['CLIENTE'], nombre: 'Cliente', email: 'cliente@test.invalid' }

before(async () => {
  console.error = (...args) => warnings.push(args.map(String).join(' '))
  const target = path.resolve('node_modules/.cache/catalogo-transicion-tests/fixture.mjs')
  await fs.mkdir(path.dirname(target), { recursive: true })
  await build({ stdin: { contents: `export {default as OperationGate} from './src/storefront/components/CatalogoOperacionGate'; export {default as Gate} from './src/storefront/components/CatalogoCompraGate'; export {default as Tracking} from './src/storefront/pages/StorefrontPedidoTrackingPage'; export {default as Return} from './src/storefront/pages/StorefrontPagoRetornoPage'; export {default as Historical} from './src/storefront/components/StorefrontHistoricalPayment'; export {default as Account} from './src/storefront/pages/StorefrontCustomerAccountPage'; export {default as Owner} from './src/storefront/pages/StorefrontOwnerPage'; export * from './src/storefront/utils/storefrontCheckoutState'; export {normalizeCommercialPhone,contactStatus} from './src/storefront/dashboard/whatsapp'; export {default as AdminConfig} from './src/pages/admin/AdminConfiguracionPage'; export {default as CheckoutResult} from './src/storefront/components/StorefrontCheckoutResult';`, resolveDir: process.cwd(), loader: 'ts' }, outfile: target, bundle: true, format: 'esm', platform: 'node', packages: 'external', jsx: 'automatic', loader: { '.jpeg': 'dataurl', '.png': 'dataurl', '.svg': 'dataurl' }, define: { 'import.meta.env': '{}' }, plugins: [{ name: 'transporte-aislado', setup(b) {
    b.onResolve({ filter: /(?:axiosClient|storefrontOwnerClient)$/ }, () => ({ path: 'transport', namespace: 'test' }))
    b.onResolve({ filter: /useTenant$/ }, () => ({ path: 'tenant', namespace: 'test' }))
    b.onResolve({ filter: /(?:useStorefrontAuth|useAuth)$/ }, () => ({ path: 'auth', namespace: 'test' }))
    b.onResolve({ filter: /storefrontMetricsApi$/ }, () => ({ path: 'metrics', namespace: 'test' }))
    b.onLoad({ filter: /.*/, namespace: 'test' }, ({ path }) => ({ contents: path === 'tenant' ? 'export const useTenant=()=>globalThis.__tenant;' : path === 'auth' ? 'export const useStorefrontAuth=()=>({session:globalThis.__session,logout:()=>{}}); export const useAuth=useStorefrontAuth;' : path === 'metrics' ? 'export const trackStorefrontEvent=()=>{}; export const trackProductView=()=>{};' : 'export default Object.fromEntries(["get","post","put","delete"].map(method=>[method,async(url,data,options)=>({data:await globalThis.__transport(method,url,method==="get"?data?.params:options?.params,method==="get"?undefined:data)})]));' }))
  } }] })
  fixture = await import(pathToFileURL(target).href)
})
afterEach(async () => {
  if (root) await act(async () => root.unmount())
  client?.clear(); root = null; window.sessionStorage.clear(); window.localStorage.clear()
  assert.deepEqual(warnings, [], 'Sin advertencias de React'); warnings = []
})
after(() => { console.error = originalError; dom.window.close() })
const tick = () => new Promise(r => setTimeout(r, 15))
async function until(check) { for (let i = 0; i < 100; i++) { await act(async () => { await tick() }); if (check()) return } assert.fail(document.body.textContent) }
const button = text => [...document.querySelectorAll('button')].find(b => b.textContent.includes(text))
async function click(el) { assert.ok(el); await act(async () => el.click()) }
const key = (store = 'r1', product = 'p1') => `storefront-checkout:REVENDEDOR:${store}:${product}`
const saved = (value = order) => JSON.stringify({ pedidoCreado: value, pagoPreparado: { checkoutUrl: 'https://no-confiable.invalid/' }, updatedAt: 1 })
function DetailDestination() { const location = useLocation(); return React.createElement('p', null, 'Ficha pública ', JSON.stringify(location.state)) }
async function mount(Component, { url = '/producto/p1/checkout', tenant = baseTenant(), props = {}, session = null } = {}) {
  globalThis.__tenant = tenant; globalThis.__session = session; calls = []
  globalThis.__transport = async (...args) => { calls.push(args); return handler(...args) }
  client = new QueryClient({ defaultOptions: { queries: { retry: false, gcTime: 0 }, mutations: { retry: false, gcTime: 0 } } })
  root = createRoot(document.getElementById('root'))
  await act(async () => root.render(React.createElement(QueryClientProvider, { client }, React.createElement(MemoryRouter, { initialEntries: [url] }, React.createElement(Routes, null,
    React.createElement(Route, { path: '/producto/:productoCanonicoId/checkout', element: React.createElement(Component, props) }),
    React.createElement(Route, { path: '/producto/:productoCanonicoId/resumen-compra', element: React.createElement(Component, props) }),
    React.createElement(Route, { path: '/producto/:productoCanonicoId', element: React.createElement(DetailDestination) }),
    React.createElement(Route, { path: '/pedido/:trackingToken', element: React.createElement(fixture.Tracking) }),
    React.createElement(Route, { path: '/mi-cuenta', element: React.createElement(fixture.Account) }),
    React.createElement(Route, { path: '/mi-tienda', element: React.createElement(fixture.Owner) }),
    React.createElement(Route, { path: '/pago/retorno', element: React.createElement(fixture.Return) }),
    React.createElement(Route, { path: '*', element: React.createElement(Component, props) })
  )))))
}
const child = () => React.createElement('p', null, 'CHECKOUT MONTADO')
const childProps = () => ({ children: React.createElement(child) })

test('borrador de checkout y resumen vuelven a ficha sin montar efectos ni borrar otras tiendas', async () => {
  window.sessionStorage.setItem(key(), JSON.stringify({ pedidoCreado: null, updatedAt: 1 }))
  window.sessionStorage.setItem(key('r2'), saved()); window.localStorage.setItem('sesion-ajena', 'conservar')
  handler = async () => { throw Error('No debe consultar compras') }
  await mount(fixture.Gate, { url: '/producto/p1/resumen-compra?seguro=1', props: childProps() })
  await until(() => document.body.textContent.includes('Ficha pública'))
  assert.doesNotMatch(document.body.textContent, /CHECKOUT MONTADO/)
  assert.match(document.body.textContent, /compraCatalogo/); assert.equal(calls.length, 0)
  assert.equal(window.sessionStorage.getItem(key('r2')), saved()); assert.equal(window.localStorage.getItem('sesion-ajena'), 'conservar')
})
test('pedido guardado va a seguimiento validado, sin usar URL ni importes guardados', async () => {
  window.sessionStorage.setItem(key(), saved())
  handler = async (method, url, params) => { assert.equal(method, 'get'); assert.equal(url, '/public/pedido/tracking-anterior'); assert.equal(params.storefrontHost, 'tienda.test.invalid'); return order }
  await mount(fixture.Gate, { props: childProps() })
  await until(() => document.body.textContent.includes('ya no procesa pagos'))
  assert.doesNotMatch(document.body.textContent, /CHECKOUT MONTADO/)
  assert.equal(document.querySelector('[href*="no-confiable"]'), null)
  assert.equal(calls.length, 1); assert.equal(window.sessionStorage.getItem(key()), saved())
})
test('token inexistente muestra error de consulta sin crear ni cobrar', async () => {
  window.sessionStorage.setItem(key(), saved())
  handler = async () => { throw Error('Token inválido') }
  await mount(fixture.Gate, { props: childProps() })
  await until(() => document.body.textContent.includes('No se pudo consultar'))
  assert.ok(calls.every(c => c[0] === 'get')); assert.equal(button('Retomar pago pendiente'), undefined)
})
test('pedido sin token y JSON inválido se conservan y no habilitan checkout', async () => {
  window.sessionStorage.setItem(key(), saved({ pedidoId: 'pedido-sin-token' }))
  window.sessionStorage.setItem(key('r2'), 'JSON incompleto')
  handler = async () => order
  await mount(fixture.Gate, { props: childProps() })
  await until(() => document.body.textContent.includes('pedidoSinEnlace'))
  assert.equal(fixture.readCheckoutState(key('r2')), null)
  assert.equal(window.sessionStorage.getItem(key('r2')), 'JSON incompleto')
  assert.equal(calls.length, 0)
})
test('contexto anterior se revalida y el servidor impide montar checkout', async () => {
  handler = async () => baseTenant().storefront
  await mount(fixture.Gate, { tenant: baseTenant(false), props: childProps() })
  await until(() => document.body.textContent.includes('Ficha pública'))
  assert.equal(calls[0][1], '/public/storefront/resolve'); assert.doesNotMatch(document.body.textContent, /CHECKOUT MONTADO/)
})
test('resolución fallida permite reintentar sin habilitar compra mientras tanto', async () => {
  let failed = true
  handler = async () => { if (failed) throw Error('Sin conexión'); return baseTenant(false).storefront }
  await mount(fixture.Gate, { tenant: baseTenant(false), props: childProps() })
  await until(() => Boolean(button('Reintentar')))
  assert.doesNotMatch(document.body.textContent, /CHECKOUT MONTADO/)
  failed = false; await click(button('Reintentar'))
  await until(() => document.body.textContent.includes('CHECKOUT MONTADO'))
})
test('respuesta de otra tienda nunca monta el checkout', async () => {
  handler = async () => ({ ...baseTenant(false).storefront, revendedorId: 'r2' })
  await mount(fixture.Gate, { tenant: baseTenant(false), props: childProps() })
  await until(() => document.body.textContent.includes('No se pudo verificar'))
  assert.doesNotMatch(document.body.textContent, /CHECKOUT MONTADO/)
})
test('proveedor conserva checkout con resolución actual propia', async () => {
  const tenant = { ...baseTenant(false), tipoStorefront: 'PROVEEDOR', revendedorId: null, proveedorId: 'prov1', storefront: { tipoStorefront: 'PROVEEDOR', proveedorId: 'prov1' } }
  handler = async () => tenant.storefront
  await mount(fixture.Gate, { tenant, props: childProps() })
  await until(() => document.body.textContent.includes('CHECKOUT MONTADO'))
})
test('sin tienda resuelta no busca referencias de otras tiendas', async () => {
  window.sessionStorage.setItem(key('r2'), saved())
  assert.equal(fixture.findLatestCheckoutStateWithTracking(null), null)
  handler = async () => { throw Error('No debe consultar') }
  await mount(fixture.Return, { url: '/pago/retorno', tenant: { ...baseTenant(), status: 'loading', revendedorId: null, tipoStorefront: null, storefront: null } })
  await act(async () => { await tick() }); assert.equal(calls.length, 0)
})
test('retorno selecciona solamente la referencia de su tienda', async () => {
  window.sessionStorage.setItem(key(), saved()); window.sessionStorage.setItem(key('r2'), JSON.stringify({ pedidoCreado: { ...order, trackingToken: 'ajeno' }, updatedAt: 999 }))
  assert.equal(fixture.findLatestCheckoutStateWithTracking('REVENDEDOR:r1').pedidoCreado.trackingToken, 'tracking-anterior')
  handler = async () => order
  await mount(fixture.Return, { url: '/pago/retorno' })
  await until(() => document.body.textContent.includes('ya no procesa pagos'))
  assert.ok(calls.every(c => c[1] === '/public/pedido/tracking-anterior')); assert.ok(window.sessionStorage.getItem(key('r2')))
})

for (const [payment, pedido, heading] of [
  ['PENDIENTE', 'CREADO', 'Estamos verificando tu pago'],
  ['CONFIRMADO', 'PAGADO', 'Pago confirmado'],
  ['APROBADO', 'CREADO', 'Estamos verificando tu pago'],
  ['RECHAZADO', 'CREADO', 'Pago no completado'],
  ['REEMBOLSADO', 'PAGADO', 'Pago reembolsado'],
  ['CANCELADO', 'CANCELADO', 'Pago no completado'],
  ['DESCONOCIDO', 'CREADO', 'Estamos verificando tu pago'],
]) {
  test(`retorno anterior usa estado real ${payment} y conserva referencia`, async () => {
    window.sessionStorage.setItem(key(), saved())
    handler = async () => ({ ...order, estadoPago: payment, estadoPedido: pedido })
    await mount(fixture.Return, { url: '/pago/retorno?status=success', tenant: baseTenant(false) })
    await until(() => document.querySelector('h1')?.textContent === heading)
    assert.equal(document.querySelector('h1').textContent, heading)
    assert.equal(window.sessionStorage.getItem(key()), saved())
    assert.ok(calls.every(c => c[0] === 'get'))
  })
}

for (const [payment, expected] of [
  ['RECHAZADO', 'Pago no completado'],
  ['REEMBOLSADO', 'Pago reembolsado'],
  ['CANCELADO', 'Pago no completado'],
]) {
  test(`seguimiento histórico explica ${payment} sin anunciar confirmación`, async () => {
    handler = async () => ({ ...order, estadoPago: payment, estadoPedido: payment === 'CANCELADO' ? 'CANCELADO' : 'PAGADO' })
    await mount(fixture.Tracking, { url: '/pedido/tracking-anterior' })
    await until(() => document.body.textContent.includes(expected))
    assert.doesNotMatch(document.body.textContent, /La tienda recibió la confirmación del pago/)
    assert.ok(calls.every(c => c[0] === 'get'))
  })
}
for (const oldUrl of ['https://www.mercadopago.com/checkout/v1/redirect?pref=existente', 'javascript:alert(1)', null]) {
  test('PSP retirado conserva pedido sin consultar ni abrir enlace: '+oldUrl, async () => {
    handler = async () => { throw Error('La pasarela retirada no debe consultarse') }
    await mount(fixture.Historical, { url: '/test', props: { order: {...order, checkoutUrl: oldUrl}, trackingToken: order.trackingToken, host: 'tienda.test.invalid' } })
    assert.match(document.body.textContent, /ya no procesa pagos/)
    assert.match(document.body.textContent, /Si ya pagaste/)
    assert.equal(document.querySelector('button,a'), null)
    assert.equal(calls.length, 0)
  })
}

test('comprobante histórico sigue accesible y no usa PSP', async () => {
  handler = async () => { throw Error('No se requiere API') }
  await mount(fixture.Historical, { url: '/test', props: { order: { ...order, metodoPago: 'TRANSFERENCIA', transferenciaToken: 'recibo-1' }, trackingToken: order.trackingToken, host: 'tienda.test.invalid' } })
  assert.match(document.querySelector('a').href, /transferencia\/recibo-1/); assert.equal(calls.length, 0)
})
test('pedido cancelado o pago confirmado no ofrece reanudación', async () => {
  handler = async () => { throw Error('No se requiere API') }
  await mount(fixture.Historical, { url: '/test', props: { order: { ...order, estadoPedido: 'CANCELADO' }, trackingToken: order.trackingToken, host: 'tienda.test.invalid' } })
  assert.equal(document.querySelector('button,a'), null); assert.equal(calls.length, 0)
})
test('cuenta de catálogo conserva historial sin prometer registrar ventas de WhatsApp', async () => {
  handler = async (method, url) => url.endsWith('/pedidos') ? [] : { nombre: 'Cliente', email: 'cliente@test.invalid' }
  await mount(fixture.Account, { url: '/mi-cuenta', session: customer })
  await until(() => document.body.textContent.includes('no se registran automáticamente'))
  assert.match(document.body.textContent, /Pedidos anteriores/); assert.doesNotMatch(document.body.textContent, /aparecerá automáticamente/)
  assert.equal(document.querySelector('[href="/catalogo"]').textContent, 'Ver catálogo')
})
test('cuenta anterior conserva sus textos y acceso al catálogo', async () => {
  handler = async (method, url) => url.endsWith('/pedidos') ? [] : { nombre: 'Cliente', email: 'cliente@test.invalid' }
  await mount(fixture.Account, { url: '/mi-cuenta', session: customer, tenant: baseTenant(false) })
  await until(() => document.body.textContent.includes('aparecerá automáticamente'))
  assert.match(document.body.textContent, /Mis compras/); assert.equal(document.querySelector('[href="/catalogo"]').textContent, 'Seguir comprando')
})
test('panel de catálogo ofrece seis secciones y elimina navegación histórica', async () => {
  handler = async (method, url) => url.endsWith('/catalogo-config') ? {cotizacionUsd:'1200.000000',version:0,pendientes:[],modo:'PREPARACION'} : []
  await mount(fixture.Owner, { url: '/mi-tienda', session: { revendedorId: 'r1', roles: ['REVENDEDOR', 'ADMIN_MD'], nombre: 'Responsable' } })
  await until(() => Boolean(button('Agregar producto')))
  assert.equal(button('Pedidos anteriores'), undefined); assert.equal(button('Pagos'), undefined); assert.ok(button('Asesores y contacto')); assert.ok(button('Difusión')); assert.ok(calls.every(c => !/pedidos|preguntas|disponibilidad|mercadopago/.test(c[1])))
  assert.equal(button('Importar productos'), undefined)
  assert.equal(document.querySelector('[name="stock"]'), null)
  assert.equal(document.body.textContent.includes('Precio anterior ARS'), false)
  await click(button('Agregar producto'))
  await until(() => Boolean(button('Crear producto')))
  assert.equal(document.body.textContent.includes('Tipo de paquete'), false)
  assert.equal(document.body.textContent.includes('Precio anterior ARS'), false)
})

test('panel no ofrece confirmar una transferencia cancelada con comprobante', async () => {
  const previous = {
    pedidoId: 'pedido-cancelado', estadoPedido: 'CANCELADO', estadoPago: 'CANCELADO',
    estadoEnvio: 'CANCELADO', metodoPago: 'TRANSFERENCIA', comprobanteCargado: true,
    fechaCreacionPedido: '2026-09-20T10:00:00', totalFinal: 100, totalBruto: 100,
    totalEnvio: 0, totalSeguro: 0, totalDescuento: 0, items: [],
  }
  handler = async (method, url) => url.endsWith('/pedidos') ? [previous] : []
  await mount(fixture.Owner, { url: '/mi-tienda?tab=pedidos', tenant: baseTenant(false), session: { revendedorId: 'r1', roles: ['REVENDEDOR'], nombre: 'Responsable' } })
  await until(() => document.body.textContent.includes('Bandeja de pedidos'))
  const filter = document.querySelector('select[aria-label="Estado de envío"]')
  assert.ok(filter)
  await act(async () => { filter.value = 'cancelados'; filter.dispatchEvent(new Event('change', { bubbles: true })) })
  await until(() => document.body.textContent.includes('PEDIDO-C'))
  assert.equal(button('Confirmar transferencia'), undefined)
  assert.equal(button('Imprimir etiqueta de envío'), undefined)
})

test('acceso privado antiguo muestra catálogo y conserva enlace a pedidos anteriores', async () => {
  handler = async () => ({ revendedorId: 'r1', modoCatalogo: true })
  await mount(fixture.OperationGate, { url: '/pricing', session: { revendedorId: 'r1', roles: ['REVENDEDOR'] }, props: childProps() })
  await until(() => document.body.textContent.includes('Tu tienda funciona como catálogo'))
  assert.doesNotMatch(document.body.textContent, /CHECKOUT MONTADO/)
  assert.ok(document.querySelector('[href="/pedidos"]'))
})
test('acceso privado de otra tienda o resolución incompleta no monta formularios', async () => {
  handler = async () => ({ revendedorId: 'r2', modoCatalogo: false })
  await mount(fixture.OperationGate, { url: '/pricing', session: { revendedorId: 'r1', roles: ['REVENDEDOR'] }, props: childProps() })
  await until(() => document.body.textContent.includes('No se pudieron verificar'))
  assert.doesNotMatch(document.body.textContent, /CHECKOUT MONTADO/)
})
test('acceso privado anterior permite sus formularios después de validar', async () => {
  handler = async () => ({ revendedorId: 'r1', modoCatalogo: false })
  await mount(fixture.OperationGate, { url: '/pricing', session: { revendedorId: 'r1', roles: ['REVENDEDOR'] }, props: childProps() })
  await until(() => document.body.textContent.includes('CHECKOUT MONTADO'))
})

test('enlace guardado del panel espera la tienda sin redirigir ni consultar datos', async () => {
  handler = async () => { throw Error('No debe consultar todavía') }
  await mount(fixture.Owner, { url: '/mi-tienda?tab=pedidos', tenant: { ...baseTenant(), status: 'loading', isLoading: true, storefront: null, revendedorId: null }, session: { revendedorId: 'r1', roles: ['REVENDEDOR'] } })
  await act(async () => { await tick() })
  assert.match(document.body.textContent, /Cargando panel/); assert.equal(calls.length, 0)
})
test('cuenta espera la tienda antes de consultar perfil y pedidos históricos', async () => {
  handler = async () => { throw Error('No debe consultar todavía') }
  await mount(fixture.Account, { url: '/mi-cuenta', tenant: { ...baseTenant(), status: 'loading', isLoading: true, storefront: null, revendedorId: null }, session: customer })
  await act(async () => { await tick() })
  assert.match(document.body.textContent, /Cargando cuenta/); assert.equal(calls.length, 0)
})

test('administracion no ofrece ni consulta Mercado Pago incluso con callback viejo', async () => {
  handler = async (method,url) => { if(url.includes('mercadopago')) throw Error('API retirada'); return url.endsWith('/costo-md') ? {items:[]} : url.includes('categorias') ? [] : {smtpAuth:true,smtpStartTls:true} }
  window.history.replaceState(null,'','/?mp_oauth_status=success')
  await mount(fixture.AdminConfig,{url:'/admin/configuracion'})
  await until(()=>calls.length>=3)
  assert.equal(calls.some(c=>c[1].includes('mercadopago')),false)
  assert.equal(button('Generar OAuth MD'),undefined)
  assert.equal(button('Guardar MP MD'),undefined)
  assert.equal(button('Guardar PSP'),undefined)
  window.history.replaceState(null,'','/')
})

test('resultado antiguo con URL de checkout guardada no ofrece abrir pasarela', async () => {
  handler=async()=>{throw Error('Sin API')}
  await mount(fixture.CheckoutResult,{url:'/test',props:{order,email:'test@test.invalid',productName:'Producto',image:null,quantity:1,checkoutUrl:'https://www.mercadopago.com/checkout/v1/redirect',error:null}})
  assert.equal([...document.querySelectorAll('a')].some(a=>a.href.includes('mercadopago')),false)
  assert.equal(calls.length,0)
})

for (const removed of ['pedidos','cobros','consultas','disponibilidad','desconocida']) test(`catálogo redirige tab obsoleto ${removed} sin consultar su API`, async () => {
  handler = async (method, url) => url.endsWith('/catalogo-config') ? { cotizacionUsd:'1200.000000', telefonoComercial:'+5491112345678', condicionesEntrega:'Local', version:0, pendientes:[], modo:'PREPARACION' } : []
  await mount(fixture.Owner, { url: '/mi-tienda?tab='+removed, session:{ revendedorId:'r1', roles:['REVENDEDOR'], nombre:'Responsable' } })
  await until(() => Boolean(button('Agregar producto')))
  assert.equal(button('Pedidos anteriores'), undefined)
  assert.ok(calls.every(c => !/pedidos|preguntas|disponibilidad|mercadopago/.test(c[1])))
})
test('métricas no carga productos ni categorías ni presenta consultas retiradas', async () => {
  handler = async () => ({visitantesUnicos:2,visitas:3,vistasProductos:5,consultas:7,clicksWhatsapp:1,rubrosMasVistos:[],productosMasVistos:[]})
  await mount(fixture.Owner, {url:'/mi-tienda?tab=metricas',session:{revendedorId:'r1',roles:['REVENDEDOR'],nombre:'Responsable'}})
  await until(() => document.body.textContent.includes('Visitantes únicos'))
  assert.ok(calls.every(c => c[1].endsWith('/metricas')))
  assert.doesNotMatch(document.body.textContent,/Consultas enviadas/)
})

test('validación comercial acepta formatos explícitos y rechaza números ambiguos', () => {
  for (const [input,expected] of [['+54 9 3482 670015','+5493482670015'],['005493482670015','+5493482670015'],['(00)54 9 3482 670015','+5493482670015'],['011 15 1234 5678','+5491112345678'],['5493482670015','+5493482670015'],['+34 612 345 678','+34612345678'],['3482670015',null],['1234',null],['++5493482670015',null],['+549348267001',null],['',null]]) assert.equal(fixture.normalizeCommercialPhone(input),expected,input)
})
test('estado comercial depende del número y actividad, sin exigir acceso al panel', () => {
  assert.equal(fixture.contactStatus(true,'+5493482670015'),'Disponible para compras')
  assert.match(fixture.contactStatus(true,null),/Sin WhatsApp/)
  assert.match(fixture.contactStatus(true,'1234'),/Número inválido/)
  assert.equal(fixture.contactStatus(false,'+5493482670015'),'Inactivo')
})
