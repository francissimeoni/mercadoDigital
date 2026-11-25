// API sintética dentro del bundle de prueba. No hay conexiones a backend, base ni WhatsApp.
import { build } from 'esbuild'
import fs from 'node:fs/promises'
import path from 'node:path'
import http from 'node:http'

export async function startPreview() {
  const base = path.resolve(import.meta.dirname, '..')
  const cssName = (await fs.readdir(path.join(base, 'dist/assets'))).find(name => name.endsWith('.css'))
  const fixture = {
    config: { cotizacionUsd: '1200.000000', condicionesEntrega: 'Cobertura local. Otros destinos se coordinan con la tienda.', telefonoComercial: '+5491100000000', version: 0, modo: 'PREPARACION', pendientes: [] },
    category: { id: 'envio-1', nombre: 'Artículo mediano', costoArs: '8000.00', version: 0 },
    datos: { id: 'producto-1', sku: 'AUR-100', nombre: 'Auriculares inalámbricos', descripcion: 'Ejemplo sintético para verificar la administración del catálogo.', categoriaProductoId: null, activo: true, promocion: false, novedad: true, media: [], precioVenta: null, stock: null, categoriaPaquete: null },
  }
  const result = await build({
    stdin: { resolveDir: base, loader: 'tsx', contents: `import {createRoot} from 'react-dom/client';import {QueryClient,QueryClientProvider} from '@tanstack/react-query';import Section from './src/storefront/components/CatalogoAdminSection';createRoot(document.getElementById('app')).render(<QueryClientProvider client={new QueryClient()}><Section id="tienda-local" modoCatalogo notify={text=>console.info(text)}/></QueryClientProvider>);` },
    bundle: true, write: false, format: 'esm', platform: 'browser', jsx: 'automatic', define: { 'process.env.NODE_ENV': '"development"' },
    plugins: [{ name: 'fixture-only', setup(b) {
      b.onResolve({ filter: /storefrontOwnerClient$/ }, () => ({ path: 'fixture', namespace: 'preview' }))
      b.onLoad({ filter: /.*/, namespace: 'preview' }, () => ({ contents: `
        const f=${JSON.stringify(fixture)};window.__writes=[];
        const price=cost=>({estado:'LISTO',pendientes:[],monedaOriginal:'USD',precioOriginal:'100.00',cotizacionUsd:'1200.000000',baseArs:'120000.00',envioArs:cost,precioPublicoArs:cost==='8000.00'?'128000.00':'130000.00'});
        const product=()=>({productoId:f.datos.id,nombre:f.datos.nombre,sku:f.datos.sku,moneda:'USD',precioOriginal:'100.00',incluyeEnvio:false,categoriaEnvioId:f.category.id,version:0,activo:true,calculo:price(f.category.costoArs)});
        const call=async(method,url,body)=>{
          if(method==='get')return{data:url.endsWith('/catalogo-config')?f.config:url.endsWith('/categorias')?[f.category]:url.endsWith('/productos')?[product()]:url.includes('/fichas/')?{datos:f.datos,versionFicha:0,precio:product()}:[]};
          if(method==='post'&&url.includes('/fichas'))return{data:{calculo:price(f.category.costoArs),token:'ficha-sintetica'}};
          if(method==='post'&&url.includes('/categorias/'))return{data:{productos:[{productoId:f.datos.id,nombre:f.datos.nombre,anterior:price(f.category.costoArs),propuesto:price(body.costoArs)}],token:'impacto-sintetico'}};
          if(method==='put'&&url.includes('/categorias/')){window.__writes.push({url,body});f.category={...f.category,nombre:body.nombre,costoArs:body.costoArs,version:f.category.version+1};return{data:f.category}};
          throw Error('Ruta no preparada en API sintetica: '+method+' '+url)
        };export default{get:u=>call('get',u),post:(u,d)=>call('post',u,d),put:(u,d)=>call('put',u,d),delete:u=>call('delete',u),request:c=>call(c.method,c.url,c.data)};` }))
    } }],
  })
  const css = await fs.readFile(path.join(base, 'dist/assets', cssName))
  const html = '<!doctype html><html lang="es"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Catálogo · prueba local sintética</title><link rel="stylesheet" href="/style.css"><body class="bg-slate-50 text-slate-950"><p class="bg-amber-100 p-3 text-center text-sm">Datos sintéticos. Sin conexión a servicios reales.</p><main id="app" class="mx-auto max-w-5xl px-4 py-6"></main><script type="module" src="/preview.mjs"></script></body></html>'
  const server = http.createServer((req, res) => {
    const route = { '/': ['text/html; charset=utf-8', html], '/style.css': ['text/css', css], '/preview.mjs': ['text/javascript', result.outputFiles[0].contents] }[req.url]
    if (!route) { res.writeHead(404); res.end(); return }
    res.writeHead(200, { 'Content-Type': route[0], 'Cache-Control': 'no-store' }); res.end(route[1])
  })
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve))
  return { server, url: `http://127.0.0.1:${server.address().port}/` }
}
