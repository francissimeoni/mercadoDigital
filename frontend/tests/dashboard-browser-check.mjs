import fs from 'node:fs/promises'
import path from 'node:path'
import assert from 'node:assert/strict'
import {startTransitionPreview} from './catalogo-transicion-preview.mjs'
const {chromium}=await import(process.env.DASHBOARD_PLAYWRIGHT_MODULE ?? 'playwright')

const out=process.env.DASHBOARD_OUT ?? path.resolve(import.meta.dirname,'../../artifacts/dashboard-vps/evidencia');await fs.mkdir(out,{recursive:true})
const {server,url,requests}=await startTransitionPreview()
const previous=server.listeners('request')[0];server.removeAllListeners('request')
const config={cotizacionUsd:'1200.000000',condicionesEntrega:'Coordiná la entrega con nuestra tienda.',telefonoComercial:'+5491112345678',version:0,modo:'PREPARACION',pendientes:[]}
const categories=[{id:'envio1',nombre:'Envío local',costoArs:'8000.00',version:0},{id:'envio2',nombre:'Paquete grande',costoArs:'12000.00',version:0}]
const calculate=p=>{const base=p.moneda==='USD'?Number(p.precioOriginal)*Number(config.cotizacionUsd):Number(p.precioOriginal);const envio=p.incluyeEnvio?0:Number(categories.find(c=>c.id===p.categoriaEnvioId)?.costoArs??0);return {estado:'LISTO',pendientes:[],monedaOriginal:p.moneda,precioOriginal:p.precioOriginal,cotizacionUsd:config.cotizacionUsd,baseArs:base.toFixed(2),envioArs:envio.toFixed(2),precioPublicoArs:(base+envio).toFixed(2)}}
let products=Array.from({length:176},(_,i)=>{const p={productoId:'p'+i,id:'p'+i,nombre:i===0?'Auriculares de prueba '+ 'nombre largo '.repeat(8):'Artículo del catálogo '+i,sku:'SKU-'+i,activo:i<144,moneda:i===0?'USD':'ARS',precioOriginal:i===0?'100.00':'825492.00',incluyeEnvio:true,categoriaEnvioId:null,version:0,stock:0,media:[],promocion:false,novedad:false,categoriaProductoId:'rubro1',categoriaProductoNombre:'Tecnología',descripcion:'Descripción de prueba',imagenPrincipalUrl:null};return {...p,calculo:calculate(p)}})
products[1].media=[{id:'m1',tipoMedia:'IMAGEN',url:url+'/foto-producto.svg'},{id:'m2',tipoMedia:'IMAGEN',url:url+'/foto-producto.svg?otra'}]
const advisors=[{id:'a1',nombre:'Ayelén Fernández',activo:true,whatsappComercial:null,email:null},{id:'a2',nombre:'Asesor válido',activo:true,whatsappComercial:'+5491112345679',email:null},{id:'a3',nombre:'Asesor inactivo',activo:false,whatsappComercial:'+5491112345670',email:null},{id:'a4',nombre:'Asesor número inválido',activo:true,whatsappComercial:'1234',email:null}]
let content={nombreComercial:'Mega Reconquista',logoUrl:null,faviconUrl:null,colorPrimario:'#0f172a',colorSecundario:'#334155',heroTitulo:'Tecnología para tu hogar',heroSubtitulo:'Atención personalizada',heroImagenUrl:null,portadas:[],footerTexto:'Consultanos',quienesTitulo:'Quiénes somos',quienesTexto:'Tienda de prueba visual',quienesGaleria:[],emailComercial:'ventas@tienda.test.invalid',telefonoComercial:config.telefonoComercial,direccionComercial:'Dirección comercial de ejemplo',instagramUrl:null,facebookUrl:null,tiktokUrl:null,youtubeUrl:null,transferenciaHabilitada:true,transferenciaAlias:'alias.anterior',transferenciaTitular:'Titular anterior',smtpHost:'smtp.test.invalid',smtpPort:587,smtpUsername:'correo@test.invalid',smtpConfigured:true,smtpAuth:true,smtpStartTls:true}
const state={fail:null,conflict:false,lastContent:null,lastProduct:null,lastConfig:null}
const publicProducts=()=>products.filter(p=>p.activo).map(p=>({...p,nombrePublico:p.nombre,precioPublicoArs:calculate(p).precioPublicoArs,modoCatalogo:true,mejorPrecioFinalEstimado:Number(calculate(p).precioPublicoArs),stockDisponible:null,imagenPrincipal:null}))
const ficha=p=>({datos:p,versionFicha:p.version,precio:{...p,calculo:calculate(p)}})
server.on('request',async(req,res)=>{
 const pathname=new URL(req.url,url).pathname
 if(pathname==='/foto-producto.svg'){res.writeHead(200,{'Content-Type':'image/svg+xml'});res.end('<svg xmlns="http://www.w3.org/2000/svg" width="180" height="180"><rect x="55" y="15" width="70" height="150" rx="10" fill="#c6d8d5"/><rect x="60" y="23" width="60" height="130" rx="5" fill="#426e65"/></svg>');return}
 if(pathname==='/foto-rota.jpg'){res.writeHead(404);res.end();return}
 if(!pathname.startsWith('/api/')||pathname==='/api/public/storefront/auth/google/configuracion')return previous(req,res)
 requests.push({method:req.method,path:pathname})
 const json=(x,status=200)=>{res.writeHead(status,{'Content-Type':'application/json'});res.end(JSON.stringify(x))}
 if(state.fail&&pathname.includes(state.fail))return json({message:'Error de API simulado'},503)
 let body='';for await(const c of req)body+=c
 let input=body.startsWith('{')?JSON.parse(body):null
 if(pathname.includes('/asesores')&&req.headers['content-type']?.startsWith('multipart/')) {const match=body.match(/\r\n\r\n(\{.*?\})\r\n--/s);input=JSON.parse(match[1]);input.fotoUrl=url+'/foto-producto.svg'}
 const isGet=req.method==='GET'
 if(pathname.endsWith('/catalogo-config')){
  if(isGet)return json(config)
  state.lastConfig=input
  if(input.impactoToken!=='config-'+config.version||state.conflict)return json({message:'La configuración cambió en otra sesión. Revisá nuevamente.'},409)
  Object.assign(config,input,{version:config.version+1});delete config.impactoToken;content.telefonoComercial=config.telefonoComercial;return json(config)
 }
 if(pathname.endsWith('/catalogo-config/vista-previa'))return json({productos:products.filter(p=>p.moneda==='USD').map(p=>({productoId:p.productoId,nombre:p.nombre,anterior:calculate(p),propuesto:{...calculate(p),precioPublicoArs:(Number(p.precioOriginal)*Number(input.cotizacionUsd)).toFixed(2)}})),token:'config-'+config.version})
 if(pathname.endsWith('/catalogo-config/productos'))return json(products.map(p=>({...p,calculo:calculate(p)})))
 if(pathname.endsWith('/catalogo-config/categorias')){
  if(isGet)return json(categories)
  const c={...input,id:'envio'+(categories.length+1),version:0};categories.push(c);return json(c)
 }
 if(/catalogo-config\/categorias\/[^/]+/.test(pathname)){
  const c=categories.find(c=>c.id===pathname.split('/')[6]);if(!c)return json({},404)
  if(pathname.endsWith('/vista-previa'))return json({productos:products.filter(p=>p.categoriaEnvioId===c.id&&!p.incluyeEnvio).map(p=>({productoId:p.productoId,nombre:p.nombre,anterior:calculate(p),propuesto:{...calculate(p),precioPublicoArs:(Number(calculate(p).baseArs)+Number(input.costoArs)).toFixed(2)}})),token:'category-'+c.version})
  if(req.method==='DELETE'){if(products.some(p=>p.categoriaEnvioId===c.id))return json({message:'La categoría está en uso.'},409);categories.splice(categories.indexOf(c),1);return json({})}
  if(input.impactoToken!=='category-'+c.version)return json({message:'Revisión vencida'},409);Object.assign(c,input,{version:c.version+1});return json(c)
 }
 if(pathname.includes('/catalogo-config/fichas')){
  const id=pathname.split('/')[6],p=products.find(p=>p.id===id)
  if(isGet)return p?json(ficha(p)):json({},404)
  if(pathname.endsWith('/vista-previa'))return json({calculo:calculate(input.precio),token:'ficha-'+(p?.version??-1)})
  const match=body.match(/\r\n\r\n(\{.*?\})\r\n--/s);const value=JSON.parse(match[1]);state.lastProduct=value
  if(state.conflict||value.previewToken!=='ficha-'+(p?.version??-1))return json({message:'La ficha cambió en otra sesión.'},409)
  const saved={...p,...value,...value.precio,id:p?.id??'pnew',productoId:p?.id??'pnew',version:(p?.version??-1)+1,media:value.imagenes.map((m,i)=>({...m,id:m.id??'mnew'+i,tipoMedia:'IMAGEN'})),imagenPrincipalUrl:null}
  if(p)products[products.indexOf(p)]=saved;else products.push(saved)
  return json(ficha(saved))
 }
 if(pathname.endsWith('/productos-propios'))return json(products)
 if(pathname.endsWith('/categorias-producto'))return json([{id:'rubro1',nombre:'Tecnología'}])
 if(pathname.endsWith('/asesores')){if(isGet)return json(advisors);const a={...input,id:'anuevo',email:null};advisors.push(a);return json(a)}
 if(pathname.includes('/asesores/')){const a=advisors.find(a=>a.id===pathname.split('/').at(-1));if(!a)return json({},404);Object.assign(a,input);if(input.quitarFoto)a.fotoUrl=null;return json(a)}
 if(pathname.endsWith('/storefront/contenido')){if(isGet)return json(content);state.lastContent=input;content={...content,...input,smtpConfigured:true};return json(content)}
 if(pathname.endsWith('/storefront/assets'))return json({url:'/producto.svg',filename:'imagen.png',contentType:'image/png',size:70})
 if(pathname.endsWith('/metricas'))return json({desde:'2026-11-01',hasta:'2026-11-24',visitantesUnicos:12,visitas:20,vistasProductos:30,consultas:7,clicksWhatsapp:3,rubrosMasVistos:state.metricsEmpty?[]:[{nombre:'Tecnología',total:22},{nombre:'Hogar',total:8}],productosMasVistos:state.metricsEmpty?[]:[{productoId:'p1',nombre:'Artículo del catálogo 1',total:12,imagenPrincipalUrl:url+'/foto-producto.svg'},{productoId:'historico',nombre:'Producto histórico sin imagen',total:3,imagenPrincipalUrl:null}]})
 if(pathname==='/api/public/catalogo/tienda')return json(publicProducts())
 if(pathname.endsWith('/contacto')){const p=products.find(p=>p.id===pathname.split('/')[6]);return json({productoId:p.id,nombre:p.nombre,sku:p.sku,precioPublicoArs:calculate(p).precioPublicoArs,condicionesEntrega:config.condicionesEntrega,storefrontHost:'tienda.test.invalid',contactos:[{id:'responsable',nombre:'Mega Reconquista',responsable:true,whatsapp:config.telefonoComercial},...advisors.filter(a=>a.activo&&/^\+549\d{10}$/.test(a.whatsappComercial??'')).map(a=>({id:a.id,nombre:a.nombre,responsable:false,whatsapp:a.whatsappComercial,fotoUrl:a.fotoUrl,provincia:a.provincia,localidad:a.localidad,zonaAtencion:a.zonaAtencion}))]})}
 if(pathname.includes('/public/catalogo/tienda/productos/')){const p=publicProducts().find(p=>p.id===pathname.split('/')[6]);return json({...p,imagenes:[],descripcion:'Descripción',sku:p.sku,condicionesEntrega:config.condicionesEntrega})}
 if(pathname.endsWith('/metricas/eventos'))return json({})
 return json({message:'Ruta no simulada'},404)
})
const browser=await chromium.launch({headless:true,args:['--disable-background-networking'],env:process.env})
const context=await browser.newContext({locale:'es-AR'})
await context.addInitScript(()=>{localStorage.setItem('md:storefront-session',JSON.stringify({token:'sesion-sintetica-local',nombre:'Responsable de prueba',email:'owner@test.invalid',roles:['REVENDEDOR'],revendedorId:'r1',clienteId:null,asesorId:null,issuedAt:'2026-11-24'}))})
await context.route('**/*',route=>route.request().url().startsWith(url)||route.request().url().startsWith('data:')?route.continue():route.abort())
const page=await context.newPage();const errors=[];page.on('pageerror',e=>errors.push(e.message))
const report={scope:'Chromium real en VPS, componentes y CSS reales; APIs y sesión sintéticas. Sin escrituras productivas ni mensajes.',views:[],cases:[],errors}
const go=async tab=>{await page.goto(url+'/mi-tienda?tab='+tab);await page.locator('main h1').waitFor();await page.waitForTimeout(180)}
try{
 for(const viewport of [{width:360,height:800},{width:390,height:844},{width:768,height:1024},{width:1366,height:768},{width:1920,height:1080}]){
  await page.setViewportSize(viewport)
  for(const tab of ['productos','precios','asesores','difusion','cuenta','metricas']){
   const from=requests.length;await go(tab)
   const v=await page.evaluate(()=>({width:innerWidth,doc:document.documentElement.scrollWidth,formWidth:document.querySelector('main form')?.getBoundingClientRect().width??null,h1:document.querySelector('main h1').textContent,overflow:[...document.querySelectorAll('main input,main select,main button,main form')].filter(e=>{const r=e.getBoundingClientRect();return r.width&&r.right>innerWidth+1}).length,advisorTop:[...document.querySelectorAll('h3')].find(x=>x.textContent.includes('Ayelén'))?.getBoundingClientRect().top??null}));
   assert.ok(v.doc<=viewport.width,JSON.stringify({tab,viewport,v}));assert.equal(v.overflow,0,JSON.stringify({tab,viewport,v}));if(tab==='cuenta'&&viewport.width===1366)assert.ok(v.formWidth>700,'Formulario principal desktop ancho');
   if(tab==='metricas'||tab==='cuenta'||tab==='asesores')assert.ok(requests.slice(from).every(r=>!r.path.endsWith('/productos-propios')),'Sin carga innecesaria de productos');
   assert.equal(await page.getByRole('button',{name:'Pedidos anteriores',exact:true}).count(),0);assert.equal(await page.getByRole('button',{name:'Pagos',exact:true}).count(),0)
   if([390,1366].includes(viewport.width))await page.screenshot({path:path.join(out,`${tab}-${viewport.width}.png`)})
   report.views.push({tab,...viewport,...v});console.log('Vista',tab,viewport.width)
  }
 }
 // Product editor: real CSS/font, five widths, keyboard, upload lifecycle and draft protection.
 for (const viewport of [{width:360,height:800},{width:390,height:844},{width:768,height:1024},{width:1366,height:768},{width:1920,height:1080}]) {
  await page.setViewportSize(viewport);await go('productos')
  await page.locator('main article').nth(1).getByRole('button',{name:/^Editar /}).click();await page.locator('#catalogo-ficha').waitFor()
  assert.equal(await page.getByRole('button',{name:'Agregar producto',exact:true}).count(),0)
  assert.equal(await page.getByLabel('Buscar por nombre o SKU').count(),0)
  assert.equal(await page.locator('main h1').count(),1)
  await page.evaluate(()=>document.fonts.load('400 14px "Inter Dashboard"'))
  const view=await page.evaluate(()=>({width:innerWidth,doc:document.documentElement.scrollWidth,font:document.fonts.check('400 14px "Inter Dashboard"'),columns:getComputedStyle(document.querySelector('.product-editor-grid')).gridTemplateColumns,buttonWidth:document.querySelector('.product-actions button').getBoundingClientRect().width,formWidth:document.querySelector('#catalogo-ficha').getBoundingClientRect().width,overflow:[...document.querySelectorAll('#catalogo-ficha input,#catalogo-ficha select,#catalogo-ficha button')].filter(e=>{const r=e.getBoundingClientRect();return r.width&&r.right>innerWidth+1}).map(e=>e.textContent)}))
  assert.deepEqual(await page.locator('#catalogo-ficha section').evaluateAll(nodes=>nodes.map(n=>n.getAttribute('aria-labelledby'))),['product-info-title','product-images-title','product-price-title','product-publish-title'])
  assert.ok(view.doc<=viewport.width,JSON.stringify(view));assert.equal(view.overflow.length,0);assert.equal(view.font,true)
  if(viewport.width>=1366)assert.ok(view.buttonWidth<230,'Acción desktop de ancho contenido')
  await page.screenshot({path:path.join(out,`editor-${viewport.width}.png`),fullPage:true});report.views.push({tab:'editor',...viewport,...view})
 }
 report.cases.push('Editor independiente en cinco tamaños, sin encabezados repetidos, Inter local, miniaturas y acción principal compacta')
 // Equivalent 200% desktop reflow plus CSS zoom exercises actual available-space sizing.
 await page.setViewportSize({width:683,height:384});await go('productos');await page.locator('main article').nth(1).getByRole('button',{name:/^Editar /}).click();await page.locator('#catalogo-ficha').waitFor();assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth));await page.screenshot({path:path.join(out,'editor-reflow-200.png'),fullPage:true})
 await page.setViewportSize({width:1366,height:768});await page.evaluate(()=>document.documentElement.style.zoom='2');assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth));await page.screenshot({path:path.join(out,'editor-css-zoom-200.png'),fullPage:true});await page.evaluate(()=>document.documentElement.style.zoom='')
 await page.getByRole('button',{name:'← Productos',exact:true}).focus();await page.keyboard.press('Tab');const focus=await page.locator(':focus').evaluate(e=>({tag:e.tagName,outline:getComputedStyle(e).outlineStyle,width:getComputedStyle(e).outlineWidth}));assert.equal(focus.tag,'INPUT');assert.equal(focus.outline,'solid');assert.equal(focus.width,'2px');report.cases.push('Teclado/foco visible, reflow equivalente al 200% y zoom CSS al 200% sin overflow')
 const imageSection=page.locator('section').filter({has:page.getByRole('heading',{name:'Imágenes',exact:true})})
 await imageSection.getByRole('button',{name:'Usar imagen 2 como portada'}).click();await imageSection.getByRole('button',{name:'Quitar imagen 2',exact:true}).click();assert.equal(await imageSection.locator('.product-photo').count(),1)
 await imageSection.getByText('Agregar desde un enlace',{exact:true}).click();await page.getByLabel('Enlace de la imagen',{exact:true}).fill(url+'/foto-rota.jpg');await imageSection.getByRole('button',{name:'Agregar imagen',exact:true}).click();await imageSection.getByText('No se pudo cargar la imagen',{exact:true}).waitFor();await imageSection.getByRole('button',{name:'Quitar imagen 2',exact:true}).click()
 await page.evaluate(()=>{window.__objectUrls={created:[],revoked:[]};const create=URL.createObjectURL.bind(URL),revoke=URL.revokeObjectURL.bind(URL);URL.createObjectURL=file=>{const u=create(file);window.__objectUrls.created.push(u);return u};URL.revokeObjectURL=u=>{window.__objectUrls.revoked.push(u);revoke(u)}})
 const tiny={name:'imagen.png',mimeType:'image/png',buffer:Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=','base64')}
 await page.getByLabel('Subir imágenes desde mi equipo').setInputFiles(tiny);await page.getByText('1 imagen(es) para guardar junto al producto.',{exact:true}).waitFor();await imageSection.getByRole('button',{name:'Quitar archivo 1',exact:true}).click();await page.waitForFunction(()=>window.__objectUrls.revoked.length===window.__objectUrls.created.length)
 await page.getByLabel('Subir imágenes desde mi equipo').setInputFiles([tiny,{...tiny,name:'segunda.png'}]);await page.getByText('2 imagen(es) para guardar junto al producto.',{exact:true}).waitFor();await page.getByRole('button',{name:'Guardar cambios',exact:true}).click();await page.getByText('Producto y precio de catálogo guardados.',{exact:true}).waitFor();assert.equal(state.lastProduct.archivos.length,2);assert.equal(state.lastProduct.imagenes.length,1);assert.ok(state.lastProduct.previewToken);await page.waitForFunction(()=>window.__objectUrls.revoked.length===window.__objectUrls.created.length)
 report.cases.push('Portada, quitar persistida solo en borrador, enlace roto, archivos con preview, multipart conjunto y revocación de object URLs')
 await go('productos');await page.getByRole('button',{name:'Agregar producto',exact:true}).click();await page.getByLabel('SKU',{exact:true}).fill('BORRADOR-PRODUCTO');const reject=dialog=>void dialog.dismiss();page.on('dialog',reject);await page.getByRole('button',{name:'← Productos',exact:true}).click();assert.equal(await page.getByLabel('SKU',{exact:true}).inputValue(),'BORRADOR-PRODUCTO');page.off('dialog',reject);const agree=dialog=>void dialog.accept();page.on('dialog',agree);await page.getByRole('button',{name:'← Productos',exact:true}).click();page.off('dialog',agree);await page.getByRole('heading',{name:'Productos',exact:true}).waitFor();report.cases.push('Volver desde el producto protege el borrador y permite descartar explícitamente')

 report.cases.push('30 vistas: seis secciones y cinco tamaños, sin desbordamientos ni controles cortados')
 await page.setViewportSize({width:1366,height:768});await go('productos')
 await page.getByLabel('Buscar por nombre o SKU').fill('Artículo del catálogo');await page.getByLabel('Publicación',{exact:true}).selectOption('activos')
 assert.equal(await page.locator('main article').count(),12);await page.getByRole('button',{name:'Siguiente',exact:true}).click();assert.match(await page.locator('main').textContent(),/Página 2 de 12/)
 await page.locator('main article').first().getByRole('button',{name:/^Editar /}).scrollIntoViewIfNeeded();const previousScroll=await page.evaluate(()=>scrollY)
 await page.locator('main article').first().getByRole('button',{name:/^Editar /}).click();await page.locator('#catalogo-ficha').waitFor();assert.equal(await page.locator('main article').count(),0)
 await page.getByRole('button',{name:'← Productos'}).click();assert.match(await page.locator('main').textContent(),/Página 2 de 12/);assert.equal(await page.getByLabel('Buscar por nombre o SKU').inputValue(),'Artículo del catálogo');assert.equal(await page.getByLabel('Publicación',{exact:true}).inputValue(),'activos');await page.waitForTimeout(100);assert.ok(Math.abs((await page.evaluate(()=>scrollY))-previousScroll)<3);assert.match(await page.locator(':focus').getAttribute('aria-label'),/^Editar /);report.cases.push('Paginación, búsqueda, filtros, scroll y foco se conservan al volver del editor')
 await go('productos');await page.locator('main article').first().getByRole('button',{name:/^Editar /}).click();await page.locator('#catalogo-ficha').waitFor()
 await page.getByLabel('¿El precio ya incluye envío?').selectOption('false');await page.getByLabel('Categoría de envío').selectOption('envio1');await page.getByText('$ 128.000,00',{exact:true}).waitFor();
 await page.getByRole('button',{name:/^(Guardar cambios|Crear producto)$/}).click();await page.getByRole('status').filter({hasText:'Producto y precio de catálogo guardados'}).waitFor();assert.equal(state.lastProduct.precio.categoriaEnvioId,'envio1');assert.ok(state.lastProduct.previewToken);report.cases.push('Edición USD + envío y guardado con token; precio ARS 128000 provisto por API')
 await go('precios');assert.equal(await page.getByLabel('Pesos por 1 USD',{exact:true}).inputValue(),'1200.00');await page.getByLabel('Pesos por 1 USD',{exact:true}).fill('1300');await page.getByRole('button',{name:'Revisar cambios antes de guardar',exact:true}).click();await page.getByRole('button',{name:'Confirmar y guardar configuración',exact:true}).waitFor();await page.getByRole('button',{name:'Confirmar y guardar configuración',exact:true}).click();await page.getByText('Configuración del catálogo guardada.',{exact:true}).waitFor();assert.equal(config.cotizacionUsd,'1300.000000');assert.equal(await page.getByLabel('Pesos por 1 USD',{exact:true}).inputValue(),'1300.00');assert.equal(config.telefonoComercial,'+5491112345678');report.cases.push('Cotización revisada y confirmada sin borrar teléfono oculto')
 await page.getByRole('button',{name:'Editar',exact:true}).first().click();await page.getByLabel('Costo en ARS',{exact:true}).fill('10000');await page.getByRole('button',{name:'Revisar impacto en precios',exact:true}).click();await page.getByRole('button',{name:'Confirmar y guardar categoría',exact:true}).click();await page.getByText('Categoría de envío guardada.',{exact:true}).waitFor();assert.equal(categories[0].costoArs,'10000.00');report.cases.push('Revisión y actualización de categoría usada conservan contrato')
 await go('asesores');assert.match(await page.locator('main').textContent(),/Sin WhatsApp: no aparece al comprar/)
 await page.getByRole('button',{name:'Editar asesor Ayelén Fernández'}).click();const advisorForm=page.getByRole('form',{name:'Editar asesor'})
 await advisorForm.getByLabel('WhatsApp comercial',{exact:true}).fill('3482670015');await advisorForm.getByText(/Se usará:/).waitFor()
 assert.match(await advisorForm.getByRole('link',{name:'Probar contacto'}).getAttribute('href'),/5493482670015$/)
 await advisorForm.getByLabel('Provincia',{exact:true}).fill('Córdoba');await advisorForm.getByLabel('Localidad principal').fill('Córdoba capital');await advisorForm.getByLabel('Detalle de la zona').fill('Zona norte y alrededores')
 await advisorForm.getByLabel('Foto del asesor').setInputFiles(tiny)
 for(const width of [390,1366]){await page.setViewportSize({width,height:width===390?844:768});assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth));await page.screenshot({path:path.join(out,'asesor-editor-'+width+'.png'),fullPage:true})}
 await advisorForm.getByLabel('WhatsApp comercial',{exact:true}).fill('123');assert.equal(await advisorForm.getByRole('button',{name:'Guardar asesor'}).isDisabled(),true);await advisorForm.getByLabel('WhatsApp comercial',{exact:true}).fill('3482670015')
 state.fail='/asesores/';await advisorForm.getByRole('button',{name:'Guardar asesor'}).click();await advisorForm.getByRole('alert').waitFor();assert.equal(await advisorForm.getByLabel('Provincia',{exact:true}).inputValue(),'Córdoba');assert.ok(await advisorForm.locator('img[src^="blob:"]').count());state.fail=null
 await advisorForm.getByRole('button',{name:'Guardar asesor'}).click();await page.getByText('Asesor guardado.',{exact:true}).waitFor()
 assert.equal(advisors[0].whatsappComercial,'+5493482670015');assert.equal(advisors[0].provincia,'Córdoba');assert.ok(advisors[0].fotoUrl)
 report.cases.push('Ficha unificada: teléfono nacional AR normalizado, foto multipart, zona opcional y asesora sin usuario')
 await go('cuenta');await page.getByLabel('Nombre comercial',{exact:true}).fill('Nombre editado');assert.equal(await page.getByText('Correo y notificaciones',{exact:true}).count(),0);assert.equal(await page.getByText('Transferencias de pedidos anteriores',{exact:false}).count(),0)
 content.telefonoComercial='+5491112345670';content.transferenciaAlias='alias.mas.reciente';await page.getByRole('button',{name:'Guardar y publicar',exact:true}).click();await page.getByText('La página de la tienda quedó actualizada.',{exact:true}).waitFor();assert.equal(state.lastContent.telefonoComercial,'+5491112345670');assert.equal(state.lastContent.transferenciaAlias,'alias.mas.reciente');assert.equal(state.lastContent.smtpHost,'smtp.test.invalid');assert.equal(state.lastContent.smtpPassword,'');report.cases.push('Mi página conserva teléfono actual y configuración SMTP/transferencias ocultas')
 await go('productos');await page.getByRole('button',{name:'Agregar producto',exact:true}).click();await page.getByLabel('SKU',{exact:true}).fill('BORRADOR');let dialogs=0;const dismiss=dialog=>{dialogs++;void dialog.dismiss()};page.on('dialog',dismiss);await page.getByRole('button',{name:'Mi página',exact:false}).click();assert.ok(page.url().includes('productos'));assert.equal(await page.getByLabel('SKU',{exact:true}).inputValue(),'BORRADOR');page.off('dialog',dismiss)
 const accept=dialog=>{void dialog.accept()};page.on('dialog',accept);await page.getByRole('button',{name:'Mi página',exact:false}).click();page.off('dialog',accept);await page.getByRole('heading',{name:'Mi página',exact:true}).waitFor();await page.getByLabel('Nombre comercial',{exact:true}).fill('Borrador de apariencia');page.on('dialog',dismiss);await page.waitForTimeout(200);await page.evaluate(()=>history.back());await page.waitForTimeout(1800);assert.ok(page.url().includes('cuenta'));assert.equal(await page.getByLabel('Nombre comercial',{exact:true}).inputValue(),'Borrador de apariencia');assert.ok(dialogs>=2);page.off('dialog',dismiss);page.on('dialog',accept);await page.goBack();await page.getByRole('heading',{name:'Productos',exact:true}).waitFor();page.off('dialog',accept);report.cases.push('Borradores protegidos al cambiar sección y al usar atrás; cancelar conserva contenido')
 await go('productos');state.fail='catalogo-config';await page.reload();await page.getByRole('alert').filter({hasText:'No se pudo cargar el catálogo'}).waitFor();state.fail=null;await page.getByRole('button',{name:'Reintentar',exact:true}).click();await page.getByRole('button',{name:'Agregar producto'}).waitFor();report.cases.push('Error de API y reintento recuperable')
 await go('productos');await page.getByRole('button',{name:'Agregar producto',exact:true}).click();await page.getByLabel('SKU',{exact:true}).fill('NEW-SKU');await page.getByLabel('Nombre del producto',{exact:true}).fill('Producto nuevo de prueba');await page.getByLabel('Moneda del artículo',{exact:true}).selectOption('ARS');await page.getByLabel('Precio de venta',{exact:true}).fill('10000');await page.getByLabel('¿El precio ya incluye envío?').selectOption('true');await page.getByLabel('Subir imágenes desde mi equipo').setInputFiles({name:'grande.png',mimeType:'image/png',buffer:Buffer.alloc(6*1024*1024)});await page.getByRole('alert').filter({hasText:'Elegí hasta 5 imágenes'}).waitFor();await page.getByLabel('Subir imágenes desde mi equipo').setInputFiles({name:'imagen.png',mimeType:'image/png',buffer:Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=','base64')});await page.getByText('1 imagen(es) para guardar junto al producto.',{exact:true}).waitFor();await page.getByRole('button',{name:/^(Guardar cambios|Crear producto)$/}).waitFor();state.conflict=true;await page.getByRole('button',{name:/^(Guardar cambios|Crear producto)$/}).click();await page.getByRole('alert').filter({hasText:'La ficha cambió en otra sesión'}).waitFor();assert.equal(await page.getByLabel('SKU',{exact:true}).inputValue(),'NEW-SKU');state.conflict=false;report.cases.push('Conflicto mantiene borrador y muestra revisión necesaria')
 page.on('dialog',accept);await go('productos');page.off('dialog',accept)
 await go('productos');await page.locator('main article').nth(1).getByRole('button',{name:/^Editar /}).click();await page.locator('#catalogo-ficha').waitFor()
 await page.getByLabel('Precio de venta',{exact:true}).fill('825493');await page.getByRole('button',{name:'Guardar cambios',exact:true}).waitFor({state:'visible'})
 for(const y of [0,900]){await page.evaluate(y=>scrollTo(0,y),y);const box=await page.getByRole('button',{name:'Guardar cambios',exact:true}).boundingBox();assert.ok(box.y>=0&&box.y+box.height<=768,'Guardar visible durante scroll desktop')}
 page.on('dialog',accept);await go('difusion');page.off('dialog',accept)
 assert.ok(await page.locator('.diffusion-row img').count()>0,'Portada visible sin desplegar')
 await page.locator('.diffusion-row input').nth(1).check();await page.locator('.diffusion-share-image img').waitFor();assert.ok(await page.locator('.diffusion-share-image img').evaluate(el=>!el.closest('button')));assert.ok(await page.getByRole('link',{name:/Abrir imagen original/}).getAttribute('href'));await page.getByLabel('Buscar por nombre o SKU').fill('Auriculares');assert.equal(await page.locator('.diffusion-preview').getByText('1 seleccionado',{exact:false}).count(),1)
 await page.getByLabel('Buscar por nombre o SKU').fill('');await page.getByRole('button',{name:'Siguiente',exact:true}).click();assert.equal(await page.locator('.diffusion-preview').getByText('1 seleccionado',{exact:false}).count(),1)
 let copied='';await page.evaluate(()=>Object.defineProperty(navigator,'clipboard',{configurable:true,value:{writeText:async text=>{window.__copied=text}}}))
 await page.getByRole('button',{name:'Copiar enlace',exact:true}).click();await page.waitForFunction(()=>window.__copied);copied=await page.evaluate(()=>window.__copied);assert.match(copied,/\/api\/public\/share\/producto\/p1/)
 products[1].precioOriginal='900000.00';await page.getByRole('button',{name:'Copiar mensaje',exact:true}).click();await page.getByRole('alert').filter({hasText:'Actualizamos los datos'}).waitFor();assert.equal(await page.evaluate(()=>window.__copied),copied)
 await page.getByRole('button',{name:'Copiar mensaje',exact:true}).click();await page.waitForFunction(()=>window.__copied.includes('900.000'));report.cases.push('Difusión: miniatura visible, selección entre filtros/páginas, enlace y revalidación impide compartir precio anterior')
 for(const width of [390,1366]){await page.setViewportSize({width,height:width===390?844:768});await go('difusion');await page.screenshot({path:path.join(out,'difusion-final-'+width+'.png'),fullPage:true})}
 await go('metricas');assert.equal(await page.locator('.metric-card').count(),4);assert.ok(await page.locator('.metric-ranking img').count()>0);await page.getByLabel('Período',{exact:true}).selectOption('7');await page.waitForTimeout(150)
 assert.equal(await page.getByText('Top 0',{exact:true}).count(),0)
 for(const width of [390,1366]){await page.setViewportSize({width,height:width===390?844:768});await page.screenshot({path:path.join(out,'metricas-final-'+width+'.png'),fullPage:true})}
 state.metricsEmpty=true;await page.getByLabel('Período',{exact:true}).selectOption('30');await page.getByText('Todavía no hay vistas registradas en este período.',{exact:true}).first().waitFor();report.cases.push('Métricas: cuatro indicadores, portadas del ranking, períodos y estado vacío sin Top 0')
 await page.setViewportSize({width:390,height:844});await go('productos');await page.locator('main article').nth(1).getByRole('button',{name:/^Editar /}).click();await page.locator('#catalogo-ficha').waitFor()
 const action=page.getByRole('button',{name:'Guardar cambios',exact:true});let actionBox=await action.boundingBox();assert.ok(actionBox.y>=0&&actionBox.y+actionBox.height<=844)
 await page.evaluate(()=>scrollTo(0,1000));actionBox=await action.boundingBox();assert.ok(actionBox.y>=0&&actionBox.y+actionBox.height<=844,'Guardar móvil sigue visible después de desplazar');await page.evaluate(()=>scrollTo(0,0))
 await page.evaluate(()=>{Object.defineProperty(visualViewport,'height',{configurable:true,value:500});visualViewport.dispatchEvent(new Event('resize'))});actionBox=await action.boundingBox();assert.ok(actionBox.y+actionBox.height<=500,'Barra accesible con viewport visual reducido por teclado simulado')
 await page.evaluate(()=>{delete visualViewport.height;visualViewport.dispatchEvent(new Event('resize'))});report.cases.push('Barra móvil visible, espacio inferior y adaptación a viewport visual reducido; teclado físico no certificado')
 await go('productos')

 await page.setViewportSize({width:1366,height:768})

 await page.goto(url+'/producto/p0?asesorId=ajeno');await page.getByRole('button',{name:'Comprar por WhatsApp',exact:true}).click();const dialog=page.getByRole('dialog');await dialog.getByText('Ayelén Fernández',{exact:true}).waitFor();await dialog.getByText('Córdoba capital · Córdoba',{exact:true}).waitFor();await page.screenshot({path:path.join(out,'selector-con-zona.png')});assert.equal(await dialog.getByText('Asesor inactivo',{exact:true}).count(),0);assert.equal(await dialog.getByText('Asesor número inválido',{exact:true}).count(),0)
 await page.keyboard.press('Escape');await page.locator('dialog').waitFor({state:'detached'});assert.match(await page.locator(':focus').textContent(),/Comprar/)
 await page.getByRole('button',{name:'Comprar por WhatsApp',exact:true}).click();await dialog.getByText('Ayelén Fernández',{exact:true}).click();await page.waitForFunction(()=>window.__catalogoNavigations.length>0);const destination=await page.evaluate(()=>window.__catalogoNavigations[0]);assert.ok(destination.startsWith('https://wa.me/5493482670015?'));assert.match(decodeURIComponent(destination),/asesorId=a1/);assert.doesNotMatch(destination,/asesorId=ajeno/);report.cases.push('Selector conserva responsable, excluye contactos inválidos/inactivos y respeta elección sobre asesorId de origen; foco y WhatsApp interceptado')
 assert.ok(requests.every(r=>!/(pedidos|preguntas|disponibilidad|mercadopago)/.test(r.path)),'Sin consultas históricas ni operaciones de compra')
 assert.deepEqual(errors,[])
 await fs.writeFile(path.join(out,'navegador-resultados.json'),JSON.stringify(report,null,2));console.log(JSON.stringify({views:report.views.length,cases:report.cases,errors},null,2))
}catch(e){await page.screenshot({path:path.join(out,'fallo.png')});await fs.writeFile(path.join(out,'fallo.txt'),e.stack+'\n'+await page.locator('body').innerText());throw e}finally{await browser.close();server.close()}
