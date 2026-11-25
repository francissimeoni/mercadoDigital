import fs from 'node:fs/promises'
import path from 'node:path'
import http from 'node:http'
import { pathToFileURL } from 'node:url'
import { build } from 'esbuild'

/** Navegador y componentes reales con API HTTP simulada local; no inicia Java ni usa base. */
export async function startPublicPreview() {
  const base = path.resolve(import.meta.dirname, '..')
  const assets = await fs.readdir(path.join(base, 'dist/assets'))
  const css = await fs.readFile(path.join(base, 'dist/assets', assets.find(n => n.endsWith('.css'))))
  const state = { contactos: 'varios', precio: '128000.25' }
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
    const branding={nombreComercial:'Tienda del Sur',colorPrimario:'#0f5f53',telefonoComercial:'+5491112345678',heroTitulo:'Elegí tu próximo favorito',heroSubtitulo:'Productos y atención directa de nuestra tienda.',portadas:[],quienesGaleria:[]};
    const tenant={status:'resolved',isLoading:false,host:'tienda.test.invalid',revendedorId:'r1',tipoStorefront:'REVENDEDOR',storefront:{modoCatalogo:true,dominio:'tienda.test.invalid'},branding,refresh:async()=>{}};
    const client=new QueryClient({defaultOptions:{queries:{retry:false},mutations:{retry:false}}});
    window.__catalogoNavigations=[];
    createRoot(document.getElementById('app')).render(<QueryClientProvider client={client}><TenantContext.Provider value={tenant}><StorefrontAuthProvider><BrowserRouter><Routes><Route element={<Layout/>}><Route path="/" element={<Catalog/>}/><Route path="/catalogo" element={<Catalog/>}/><Route path="/producto/:productoCanonicoId" element={<Detail/>}/><Route path="/producto/:productoCanonicoId/checkout" element={<Gate><p>Checkout legado: no debe montarse</p></Gate>}/></Route></Routes></BrowserRouter></StorefrontAuthProvider></TenantContext.Provider></QueryClientProvider>);
  `
  const result = await build({ stdin: { contents: source, resolveDir: base, loader: 'jsx' }, bundle: true, write: false, format: 'esm', jsx: 'automatic', loader: { '.jpeg': 'dataurl', '.png': 'dataurl', '.svg': 'dataurl' }, define: { 'import.meta.env': JSON.stringify({ VITE_API_BASE_URL: '/api', DEV: false }) }, plugins: [{ name: 'sin-navegacion-externa', setup(b) {
    b.onLoad({ filter: /catalogoWhatsApp\.ts$/ }, async ({ path }) => ({ contents: (await fs.readFile(path,'utf8')).replace('window.location.assign(url)', 'window.__catalogoNavigations.push(url)'), loader: 'ts' }))
  } }] })
  const js = result.outputFiles[0].contents
  const html = '<!doctype html><html lang="es"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Catálogo público · simulación local</title><link rel="stylesheet" href="/style.css"></head><body><p class="bg-amber-100 p-2 text-center text-xs">Vista local con datos simulados. WhatsApp no se abre y no se envían mensajes.</p><div id="app"></div><script type="module" src="/preview.mjs"></script></body></html>'
  const owner = { id:'responsable',nombre:'Tienda del Sur',responsable:true,whatsapp:'+5491112345678' }
  const advisor = { id:'a1',nombre:'Ana · Asesora de ventas',responsable:false,whatsapp:'+5491112345679' }
  const condiciones = 'Consultá las zonas de cobertura y coordiná la entrega con la tienda.'
  const products = () => [
    { productoId:'p1',productoCanonicoId:null,nombrePublico:'Cafetera espresso compacta',categoriaProducto:'Cocina',mejorPrecioFinalEstimado:Number(state.precio),precioPublicoArs:state.precio,modoCatalogo:true,stockDisponible:null,demoraEstimadaDias:null,cantidadOfertas:null,imagenPrincipal:'/producto.svg',promocion:false,novedad:true },
    { productoId:'p2',productoCanonicoId:null,nombrePublico:'Auriculares inalámbricos',categoriaProducto:'Tecnología',mejorPrecioFinalEstimado:38000,precioPublicoArs:'38000.00',modoCatalogo:true,stockDisponible:null,demoraEstimadaDias:null,cantidadOfertas:null,imagenPrincipal:null,promocion:true,novedad:false },
  ]
  const svg = '<svg xmlns="http://www.w3.org/2000/svg" width="640" height="500" viewBox="0 0 640 500"><rect width="640" height="500" fill="#f0f7f4"/><rect x="185" y="70" width="245" height="310" rx="30" fill="#23483e"/><rect x="215" y="95" width="185" height="85" rx="12" fill="#d9e9e2"/><circle cx="250" cy="135" r="15" fill="#508872"/><rect x="225" y="215" width="165" height="110" rx="8" fill="#15392f"/><rect x="265" y="240" width="70" height="60" rx="12" fill="#fff"/><path d="M335 250h15v25h-15" fill="none" stroke="white" stroke-width="9"/><rect x="170" y="375" width="275" height="25" rx="10" fill="#15392f"/></svg>'
  const server = http.createServer((req,res) => {
    const url = new URL(req.url,'http://127.0.0.1')
    requests.push({ method:req.method,path:url.pathname,query:Object.fromEntries(url.searchParams) })
    const send = (type,body,status=200) => { res.writeHead(status,{'Content-Type':type,'Cache-Control':'no-store'});res.end(body) }
    if (url.pathname === '/preview.mjs') return send('text/javascript',js)
    if (url.pathname === '/style.css') return send('text/css',css)
    if (url.pathname === '/producto.svg') return send('image/svg+xml',svg)
    if (url.pathname === '/api/public/storefront/metricas/eventos') return send('application/json','{}')
    if (url.pathname === '/api/public/catalogo/tienda') return send('application/json',JSON.stringify(products()))
    if (url.pathname.startsWith('/api/public/catalogo/tienda/productos/')) {
      const product = products().find(p => p.productoId === url.pathname.split('/')[6])
      if (!product) return send('application/json','{}',404)
      if (url.pathname.endsWith('/contacto')) {
        if (state.contactos === 'error') return send('application/json','{"message":"Error simulado"}',503)
        const contactos = state.contactos === 'ninguno' ? [] : state.contactos === 'uno' ? [owner] : [owner,advisor]
        return send('application/json',JSON.stringify({ productoId:product.productoId,nombre:product.nombrePublico,sku:'MD-C01',precioPublicoArs:product.precioPublicoArs,condicionesEntrega:condiciones,storefrontHost:'tienda.test.invalid',contactos }))
      }
      return send('application/json',JSON.stringify({...product,descripcion:'Diseño compacto para disfrutar de tu café todos los días. Consultá al equipo de la tienda para conocer más detalles.',imagenes:product.imagenPrincipal?[product.imagenPrincipal]:[],sku:'MD-C01',condicionesEntrega:condiciones,seguroTransporteEstimado:null,totalConSeguroEstimado:null}))
    }
    if (url.pathname.startsWith('/api/')) return send('application/json','{}',404)
    if (url.pathname === '/favicon.ico') return send('image/x-icon','',404)
    return send('text/html; charset=utf-8',html)
  })
  await new Promise(resolve=>server.listen(0,'127.0.0.1',resolve))
  return {server,url:`http://127.0.0.1:${server.address().port}`,state,requests}
}
if (process.argv[1] && import.meta.url === pathToFileURL(path.resolve(process.argv[1])).href) {
  const {server,url}=await startPublicPreview(); console.log(`Catálogo público simulado: ${url}`)
  process.once('SIGINT',()=>server.close())
}
