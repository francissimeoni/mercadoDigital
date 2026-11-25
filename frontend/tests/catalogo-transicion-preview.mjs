import fs from 'node:fs/promises'
import path from 'node:path'
import http from 'node:http'
import { build } from 'esbuild'

/** Componentes reales; tenant, sesión y API sintéticos. Nunca inicia Java ni servicios externos. */
export async function startTransitionPreview() {
  const base = path.resolve(import.meta.dirname, '..')
  const assets = await fs.readdir(path.join(base, 'dist/assets'))
  const css = await fs.readFile(path.join(base, 'dist/assets', assets.find(n => n.endsWith('.css'))))
  const state = { resolveError: false, trackingError: false, emptyOrders: false, paymentError: false, privateMode: true }
  const requests = []
  const source = `
    import React from 'react'; import {createRoot} from 'react-dom/client';
    import {BrowserRouter,Routes,Route} from 'react-router-dom';
    import {QueryClient,QueryClientProvider} from '@tanstack/react-query';
    import {TenantContext} from './src/tenant/TenantContext';
    import {StorefrontAuthProvider} from './src/storefront/auth/StorefrontAuthContext';
    import Layout from './src/storefront/layout/PublicStoreLayout';
    import Catalog from './src/storefront/pages/StorefrontCatalogPage';
    import Detail from './src/storefront/pages/StorefrontProductDetailPage';
    import Gate from './src/storefront/components/CatalogoCompraGate';
    import Checkout from './src/storefront/pages/StorefrontCheckoutPage';
    import Summary from './src/storefront/pages/StorefrontCompraResumenPage';
    import Tracking from './src/storefront/pages/StorefrontPedidoTrackingPage';
    import Return from './src/storefront/pages/StorefrontPagoRetornoPage';
    import Account from './src/storefront/pages/StorefrontCustomerAccountPage';
    import Owner from './src/storefront/pages/StorefrontOwnerPage';
    import Login from './src/storefront/pages/StorefrontLoginPage';
    import OperationGate from './src/storefront/components/CatalogoOperacionGate';
    const scenario=new URLSearchParams(location.search).get('escenario');
    const provider=scenario==='proveedor'; const mode=!['anterior','proveedor','desactualizado'].includes(scenario);
    const host=provider?'proveedor.test.invalid':scenario==='anterior'?'anterior.test.invalid':'tienda.test.invalid';
    const branding={transferenciaHabilitada:scenario==='anterior',nombreComercial:'Tienda del Sur',colorPrimario:'#0f5f53',telefonoComercial:'+5491112345678',portadas:[],quienesGaleria:[]};
    const storefront={tipoStorefront:provider?'PROVEEDOR':'REVENDEDOR',revendedorId:provider?null:'r1',proveedorId:provider?'prov1':null,modoCatalogo:mode,dominio:host,mercadoPagoHabilitado:!mode,publicada:true};
    const tenant={status:'resolved',isLoading:false,host,revendedorId:storefront.revendedorId,proveedorId:storefront.proveedorId,tipoStorefront:storefront.tipoStorefront,storefront,branding,refresh:async()=>{}};
    const client=new QueryClient({defaultOptions:{queries:{retry:false},mutations:{retry:false}}});
    window.__catalogoNavigations=[];
    const protectedPurchase=(page)=><Gate>{page}</Gate>;
    createRoot(document.getElementById('app')).render(<QueryClientProvider client={client}><TenantContext.Provider value={tenant}><StorefrontAuthProvider><BrowserRouter><Routes><Route element={<Layout/>}><Route path="/" element={<Catalog/>}/><Route path="/catalogo" element={<Catalog/>}/><Route path="/producto/:productoCanonicoId" element={<Detail/>}/><Route path="/producto/:productoCanonicoId/checkout" element={protectedPurchase(<Checkout/>)}/><Route path="/producto/:productoCanonicoId/resumen-compra" element={protectedPurchase(<Summary/>)}/><Route path="/pedido/:trackingToken" element={<Tracking/>}/><Route path="/pago/retorno" element={<Return/>}/><Route path="/mi-cuenta" element={<Account/>}/><Route path="/ingresar" element={<Login/>}/><Route path="/transferencia/:token" element={<p>Ruta conservada de comprobantes; carga excluida de esta simulación.</p>}/></Route><Route path="/mi-tienda" element={<Owner/>}/><Route path="/pricing" element={<OperationGate><p>Formulario anterior habilitado</p></OperationGate>}/></Routes></BrowserRouter></StorefrontAuthProvider></TenantContext.Provider></QueryClientProvider>);
  `
  const result = await build({ stdin: { contents: source, resolveDir: base, loader: 'jsx' }, bundle: true, write: false, format: 'esm', jsx: 'automatic', loader: { '.jpeg': 'dataurl', '.png': 'dataurl', '.svg': 'dataurl' }, define: { 'import.meta.env': JSON.stringify({ VITE_API_BASE_URL: '/api', DEV: false }) }, plugins: [{ name: 'sin-whatsapp-real', setup(b) {
    b.onResolve({ filter: /\/auth\/useAuth$/ }, () => ({ path: 'auth-privado-sintetico', namespace: 'preview' }));
    b.onLoad({ filter: /.*/, namespace: 'preview' }, () => ({ contents: 'export const useAuth=()=>({session:JSON.parse(localStorage.getItem("md:storefront-session")||"null")});' }));
    b.onLoad({ filter: /catalogoWhatsApp\.ts$/ }, async ({ path }) => ({ contents: (await fs.readFile(path, 'utf8')).replace('window.location.assign(url)', 'window.__catalogoNavigations.push(url)'), loader: 'ts' }))
  } }] })
  const html = '<!doctype html><html lang="es"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Transición de catálogo · prueba local</title><link rel="stylesheet" href="/style.css"></head><body><p class="bg-amber-100 p-2 text-center text-xs">Prueba local con API y sesiones sintéticas. Sin pagos ni mensajes reales.</p><div id="app"></div><script type="module" src="/preview.mjs"></script></body></html>'
  const item = { productoId: 'p1', productoCanonicoId: null, nombrePublico: 'Cafetera espresso compacta', categoriaProducto: 'Cocina', mejorPrecioFinalEstimado: 128000.25, precioPublicoArs: '128000.25', modoCatalogo: true, stockDisponible: null, demoraEstimadaDias: null, imagenPrincipal: null, promocion: false, novedad: true }
  const order = { pedidoId: '00000000-0000-0000-0000-000000000010', trackingToken: 'tracking-anterior', estadoPedido: 'CREADO', estadoPago: 'PENDIENTE', metodoPago: 'PSP', totalFinal: 128000.25, fechaCreacion: '2026-09-20T10:00:00', totalProducto: 128000.25, totalEnvio: 0, totalSeguro: 0, totalDescuento: 0, demoraEstimadaDias: 5, items: [{ pedidoItemId: 'i1', nombreProducto: item.nombrePublico, cantidad: 1, imagenPrincipal: null, productoPublicacionId: 'p1' }], envio: { estadoEnvio: 'CREADO', trackingLogistica: null } }
  const profile = { nombre: 'Cliente de prueba', email: 'cliente@test.invalid', telefonoWhatsapp: '+5491112345678', provincia: 'Buenos Aires', localidad: 'La Plata', codigoPostal: '1900', calle: 'Calle de prueba', numero: '123' }
  const server = http.createServer((req, res) => {
    const url = new URL(req.url, 'http://127.0.0.1')
    requests.push({ method: req.method, path: url.pathname, query: Object.fromEntries(url.searchParams) })
    const send = (type, body, status = 200) => { res.writeHead(status, { 'Content-Type': type, 'Cache-Control': 'no-store' }); res.end(body) }
    const json = (body, status = 200) => send('application/json', JSON.stringify(body), status)
    if (url.pathname.startsWith('/fonts/')) {
      const allowed = ['inter-latin.woff2', 'Inter-LICENSE.txt']
      const name = url.pathname.slice('/fonts/'.length)
      if (!allowed.includes(name)) return send('text/plain', 'Not found', 404)
      fs.readFile(path.resolve(base, 'public/fonts', name)).then(data => send(name.endsWith('.woff2') ? 'font/woff2' : 'text/plain', data)).catch(() => send('text/plain', 'Not found', 404))
      return
    }
    if (url.pathname === '/preview.mjs') return send('text/javascript', result.outputFiles[0].contents)
    if (url.pathname === '/style.css') return send('text/css', css)
    if (url.pathname === '/api/public/storefront/resolve') {
      if (state.resolveError) return json({ message: 'Error simulado' }, 503)
      const host = url.searchParams.get('host'), provider = host?.startsWith('proveedor'), mode = !provider && !host?.startsWith('anterior')
      return json({ tipoStorefront: provider ? 'PROVEEDOR' : 'REVENDEDOR', revendedorId: provider ? null : 'r1', proveedorId: provider ? 'prov1' : null, modoCatalogo: mode, publicada: true, dominio: host })
    }
    if (url.pathname === '/api/public/storefront/auth/google/configuracion') return json({ habilitado: false })
    if (url.pathname === '/api/public/storefront/metricas/eventos') return json({})
    if (url.pathname === '/api/cliente/storefront/pedidos') return json(state.emptyOrders ? [] : [order])
    if (url.pathname === '/api/cliente/storefront/perfil') return json(profile)
    if (url.pathname.endsWith('/pricing-v1/config')) return json({ revendedorId: 'r1', modoCatalogo: state.privateMode })
    if (url.pathname.endsWith('/catalogo-config')) return json({ cotizacionUsd: '1200.000000', condicionesEntrega: 'Cobertura local sintética.', telefonoComercial: '+5491100000000', version: 0, modo: 'PREPARACION', pendientes: [] })
    if (url.pathname.startsWith('/api/revendedores/')) return json([])
    if (url.pathname === '/api/public/pedido/tracking-anterior/reanudar-pago') return state.paymentError ? json({ message: 'Error simulado' }, 409) : json({ pedidoId: order.pedidoId, checkoutUrl: 'https://www.mercadopago.com/checkout/v1/redirect?pref=simulada' })
    if (url.pathname.startsWith('/api/public/pedido/')) return state.trackingError || !url.pathname.endsWith('/tracking-anterior') ? json({ message: 'Pedido no encontrado' }, 404) : json(order)
    if (url.pathname === '/api/public/catalogo/tienda') return json([item])
    if (url.pathname.endsWith('/contacto')) return json({ productoId: 'p1', nombre: item.nombrePublico, sku: 'C01', precioPublicoArs: item.precioPublicoArs, condicionesEntrega: 'Coordiná la entrega con la tienda.', storefrontHost: 'tienda.test.invalid', contactos: [{ id: 'responsable', nombre: 'Tienda del Sur', responsable: true, whatsapp: '+5491112345678' }] })
    if (url.pathname.startsWith('/api/public/catalogo/tienda/productos/')) {
      const legacy = /^(anterior|proveedor)\./.test(url.searchParams.get('host') ?? '')
      return json({ ...item, modoCatalogo: !legacy, stockDisponible: legacy ? 3 : null, descripcion: 'Producto de prueba local.', imagenes: [], condicionesEntrega: 'Coordiná la entrega con la tienda.', seguroTransporteEstimado: legacy ? 100 : null, totalConSeguroEstimado: legacy ? 128100.25 : null })
    }
    if (url.pathname.startsWith('/api/')) return json({ message: 'Ruta no simulada' }, 404)
    if (url.pathname === '/favicon.ico') return send('image/x-icon', '', 404)
    return send('text/html; charset=utf-8', html)
  })
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve))
  return { server, url: `http://127.0.0.1:${server.address().port}`, state, requests, order }
}
