// Vista local para revisar presentación. Respuestas fijas; no valida persistencia ni cálculos.
import { build } from 'esbuild'
import fs from 'node:fs/promises'
import path from 'node:path'
import http from 'node:http'
import { pathToFileURL } from 'node:url'

export async function startPreview() {
  const base = path.resolve(import.meta.dirname, '..')
  const cssName = (await fs.readdir(path.join(base, 'dist/assets'))).find(name => name.endsWith('.css'))
  if (!cssName) throw Error('Ejecutá npm run build antes de la vista local')
  const fixture = {
    precio: { estado: 'LISTO', pendientes: [], monedaOriginal: 'USD', precioOriginal: 100, cotizacionUsd: 1200, baseArs: 120000, envioArs: 8000, precioPublicoArs: 128000 },
    config: { cotizacionUsd: 1200, condicionesEntrega: 'Envíos dentro de nuestra zona de cobertura. Otros destinos se coordinan por WhatsApp.', telefonoComercial: '+5491100000000', version: 0, modo: 'PREPARACION', pendientes: [] },
    categoria: { id: 'envio-1', nombre: 'Artículo mediano', costoArs: 8000, version: 0 },
  }
  fixture.producto = { productoId: 'producto-1', nombre: 'Auriculares inalámbricos', sku: 'AUR-100', moneda: 'USD', precioOriginal: 100, incluyeEnvio: false, categoriaEnvioId: 'envio-1', version: 0, calculo: fixture.precio }
  const result = await build({
    stdin: { resolveDir: base, loader: 'tsx', contents: `import {createRoot} from 'react-dom/client'; import {QueryClient,QueryClientProvider} from '@tanstack/react-query'; import Section from './src/storefront/components/CatalogoAdminSection'; createRoot(document.getElementById('app')).render(<QueryClientProvider client={new QueryClient()}><Section id="tienda-local" notify={text=>console.info(text)}/></QueryClientProvider>);` },
    bundle: true, write: false, format: 'esm', platform: 'browser', jsx: 'automatic',
    define: { 'process.env.NODE_ENV': '"development"' },
    plugins: [{ name: 'fixture-only', setup(b) {
      b.onResolve({ filter: /storefrontOwnerClient$/ }, () => ({ path: 'fixture', namespace: 'preview' }))
      b.onLoad({ filter: /.*/, namespace: 'preview' }, () => ({ contents: `const f=${JSON.stringify(fixture)};const call=async(method,url,body)=>({data:method==='get'?(url.endsWith('/productos')?[f.producto]:url.endsWith('/categorias')?[f.categoria]:f.config):url.includes('/productos/')?(url.endsWith('/vista-previa')?{calculo:f.precio,token:'simulado'}:f.producto):{productos:[],token:'simulado'}});export default{get:u=>call('get',u),post:(u,d)=>call('post',u,d),put:(u,d)=>call('put',u,d),delete:u=>call('delete',u)};` }))
    } }],
  })
  const js = result.outputFiles[0].contents
  const css = await fs.readFile(path.join(base, 'dist/assets', cssName))
  const html = '<!doctype html><html lang="es"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Catálogo · vista local simulada</title><link rel="stylesheet" href="/style.css"><body class="bg-slate-50 text-slate-950"><p class="bg-amber-100 p-3 text-center text-sm">Vista local con datos simulados. Los cambios no se guardan.</p><main id="app" class="mx-auto max-w-5xl px-4 py-6"></main><script type="module" src="/preview.mjs"></script></body></html>'
  const server = http.createServer((req, res) => {
    const routes = { '/': ['text/html; charset=utf-8', html], '/style.css': ['text/css', css], '/preview.mjs': ['text/javascript', js] }
    const route = routes[req.url]
    if (!route) { res.writeHead(404); res.end(); return }
    res.writeHead(200, { 'Content-Type': route[0], 'Cache-Control': 'no-store' }); res.end(route[1])
  })
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve))
  return { server, url: `http://127.0.0.1:${server.address().port}/` }
}
if (process.argv[1] && import.meta.url === pathToFileURL(path.resolve(process.argv[1])).href) {
  const { server, url } = await startPreview()
  console.log(`Vista local simulada: ${url}`)
  process.once('SIGINT', () => server.close())
}
