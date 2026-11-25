import fs from 'node:fs/promises'
import path from 'node:path'
import http from 'node:http'
import assert from 'node:assert/strict'
const {chromium}=await import(process.env.DASHBOARD_PLAYWRIGHT_MODULE ?? 'playwright')
const work=path.resolve('..'),out=path.join(work,'evidencia'),dist=path.resolve('dist')
await fs.mkdir(out,{recursive:true})
const baseline=JSON.parse(await fs.readFile(path.join(work,'baseline.json'),'utf8'))
const base='https://www.megareconquista.com',host='www.megareconquista.com'
const original=baseline.products,products=original.map(p=>({...p,categoriaProducto:['CHICO','MEDIANO'].includes(p.categoriaProducto)?'Otros':p.categoriaProducto}))
const selected=[products.find(p=>/samsung a56/i.test(p.nombrePublico)),products.find(p=>/GRUPO ELECTR/i.test(p.nombrePublico))]
assert.ok(selected.every(Boolean))
const details={},contacts={}
for(const p of selected){details[p.productoId]=await(await fetch(base+`/api/public/catalogo/tienda/productos/${p.productoId}?host=${host}`)).json();contacts[p.productoId]=await(await fetch(base+`/api/public/catalogo/tienda/productos/${p.productoId}/contacto?host=${host}`)).json()}
const axe=await(await fetch('https://cdnjs.cloudflare.com/ajax/libs/axe-core/4.10.3/axe.min.js')).text()
const requests=[],errors=[],external=[],views=[],cases=[]
let scenario='normal'
const server=http.createServer(async(req,res)=>{
 try{const url=new URL(req.url,'http://localhost');let file=path.resolve(dist,'.'+decodeURIComponent(url.pathname));if(!file.startsWith(dist+path.sep))file=path.join(dist,'index.html');try{if(!(await fs.stat(file)).isFile())file=path.join(dist,'index.html')}catch{file=path.join(dist,'index.html')}
 const type={'.html':'text/html','.js':'text/javascript','.css':'text/css','.woff2':'font/woff2','.png':'image/png','.jpeg':'image/jpeg','.svg':'image/svg+xml'}[path.extname(file)]||'application/octet-stream';res.writeHead(200,{'Content-Type':type});res.end(await fs.readFile(file))}catch(e){res.writeHead(500);res.end(String(e))}
})
await new Promise(r=>server.listen(0,'127.0.0.1',r));const url=`http://127.0.0.1:${server.address().port}`
const browser=await chromium.launch({headless:true}),context=await browser.newContext({locale:'es-AR',reducedMotion:'reduce'})
await context.addInitScript(()=>sessionStorage.setItem('md:storefront-host-override','www.megareconquista.com'))
await context.route('**/*',async route=>{
 const req=route.request(),u=new URL(req.url())
 if(!['GET','HEAD'].includes(req.method())){requests.push({blocked:req.method(),url:u.href});return route.fulfill({status:200,contentType:'application/json',headers:{'Access-Control-Allow-Origin':'*','Access-Control-Allow-Headers':'*','Access-Control-Allow-Methods':'GET, HEAD, OPTIONS, POST'},body:'{}'})}
 if(/^https:\/\/(wa.me|api.whatsapp.com)/.test(u.href)){external.push(u.href);return route.abort()}
 const json=(data,status=200)=>route.fulfill({status,contentType:'application/json',headers:{'Access-Control-Allow-Origin':'*'},body:JSON.stringify(data)})
 if(u.pathname.startsWith('/api/') || u.port==='8080'){
  requests.push({path:u.pathname,query:Object.fromEntries(u.searchParams)})
  if(u.pathname.endsWith('/storefront/resolve')){const t={...baseline.tenants[host]};if(scenario.startsWith('banner-')){
    const shape=scenario.slice(7),dimensions=shape==='square'?[600,600]:shape==='portrait'?[600,1000]:[1600,480]
    const svg=(width,height,label)=>'data:image/svg+xml,'+encodeURIComponent('<svg xmlns="http://www.w3.org/2000/svg" width="'+width+'" height="'+height+'"><rect width="100%" height="100%" fill="#173a51"/><rect x="10" y="10" width="'+(width-20)+'" height="'+(height-20)+'" fill="none" stroke="white" stroke-width="4"/><text x="50%" y="50%" text-anchor="middle" fill="white" font-size="32">'+label+'</text></svg>')
    t.nombreComercial='Tienda de prueba';t.logoUrl=null;t.colorPrimario='#70451c';t.heroTitulo='Una portada propia';t.heroSubtitulo=null;t.heroImagenUrl=shape==='none'?null:svg(...dimensions,'Portada completa');t.portadas=shape==='multiple'?[{id:'a',imagenUrl:svg(1600,480,'Primera portada'),titulo:'Campaña de la tienda',subtitulo:'Texto configurado por el comercio'},{id:'b',imagenUrl:svg(600,900,'Segunda portada'),titulo:'Otra campaña',subtitulo:null}]:[]
  }if(scenario==='legacy')t.modoCatalogo=false;if(scenario==='brand'){t.colorPrimario='#f9d836';t.nombreComercial='Otra tienda';t.logoUrl=null;t.heroImagenUrl=null;t.quienesTitulo='Nuestro comercio';t.quienesTexto='Una historia propia\nAtención de nuestra tienda';t.quienesGaleria=[]}return json(t)}
  if(u.pathname.endsWith('/catalogo/tienda')){if(scenario==='error')return json({message:'Error simulado'},503);if(scenario==='empty')return json([]);if(scenario==='legacy')return json(products.map(p=>({...p,modoCatalogo:false,stockDisponible:3})));if(scenario==='large')return json([{...selected[0],precioPublicoArs:'99999999999999999.99',nombrePublico:'Nombre muy largo '.repeat(10),imagenPrincipal:null}]);return json(products)}
  if(u.pathname.endsWith('/contacto'))return json(contacts[u.pathname.split('/').at(-2)]||contacts[selected[0].productoId])
  if(/\/catalogo\/tienda\/productos\//.test(u.pathname)){const id=u.pathname.split('/').at(-1);return json(details[id]||{...details[selected[0].productoId],productoId:id})}
  if(u.pathname.endsWith('/google/configuracion'))return json({habilitado:false})
  return json({},403)
 }
 if(scenario==='broken'&&req.resourceType()==='image')return route.abort()
 return route.continue()
})
const page=await context.newPage();page.on('pageerror',e=>errors.push(e.message))
const settle=()=>page.waitForTimeout(350)
async function visit(route){await page.goto(url+route);await page.locator('.sf-header .sf-search input').waitFor();await page.locator('.sf-product-card,.sf-product-detail,.sf-about,.sf-empty,[role=alert]').first().waitFor();await settle();await page.evaluate(()=>document.fonts.ready)}
async function capture(name,width){
 const data=await page.evaluate(()=>({width:innerWidth,scrollWidth:document.documentElement.scrollWidth,font:getComputedStyle(document.querySelector('.sf-storefront')).fontFamily,searchVisible:document.querySelector('input[type=search]').getBoundingClientRect().height>0,firstCardY:document.querySelector('.sf-product-card')?.getBoundingClientRect().top,columns:getComputedStyle(document.querySelector('.sf-product-grid')||document.body).gridTemplateColumns,cardCount:document.querySelectorAll('.sf-product-card').length,headerHeight:document.querySelector('.sf-header').getBoundingClientRect().height,buyY:[...document.querySelectorAll('button')].find(b=>b.textContent==='Comprar por WhatsApp')?.getBoundingClientRect().top}))
 assert.ok(data.scrollWidth<=width,`${name} ${width}: desbordamiento`);assert.ok(data.searchVisible)
 await page.addScriptTag({content:axe});const audit=await page.evaluate(async()=>{const r=await axe.run(document,{runOnly:{type:'tag',values:['wcag2a','wcag2aa','wcag21aa','wcag22aa']}});return {violations:r.violations.map(v=>({id:v.id,impact:v.impact,count:v.nodes.length,nodes:v.nodes.slice(0,4).map(n=>({target:n.target,summary:n.failureSummary}))})),incomplete:r.incomplete.map(v=>({id:v.id,count:v.nodes.length}))}})
 await page.screenshot({path:path.join(out,`${name}-${width}.png`)});views.push({name,...data,...audit});console.log(JSON.stringify({name,width,...data,violations:audit.violations}))
}
try{
 for(const width of process.env.STOREFRONT_FAST ? [] : [320,390,768,1366,1920]){
  await page.setViewportSize({width,height:width<=390?844:768})
  for(const [name,route]of[['catalog','/catalogo'],['product','/producto/'+selected[1].productoId],['about','/quienes-somos']]){await visit(route);await capture(name,width)}
 }
 await page.setViewportSize({width:1366,height:768});await visit('/catalogo')
 const promotions=products.filter(p=>p.promocion),news=products.filter(p=>p.novedad)
 assert.equal(await page.locator('.sf-collection').count(),Number(promotions.length>0)+Number(news.length>0))
 const promotion=page.locator('.sf-collection').filter({has:page.getByRole('heading',{name:'Promociones',exact:true})})
 assert.equal(await promotion.locator('.sf-collection-item').count(),Math.min(12,promotions.length))
 await promotion.getByRole('button',{name:/Ver todas/}).click();await settle();assert.equal(new URL(page.url()).searchParams.get('coleccion'),'promocion');assert.equal(await page.locator('.sf-product-card').count(),Math.min(24,promotions.length));assert.equal(await page.locator('.sf-hero,.sf-collection').count(),0)
 await page.locator('.sf-card-link').first().click();await settle();await page.getByRole('link',{name:/Volver al catalogo/}).click();await settle();assert.equal(new URL(page.url()).searchParams.get('coleccion'),'promocion')
 await page.getByRole('button',{name:'Promociones ×',exact:true}).click();await settle();assert.equal(await page.locator('.sf-collection').count(),2)
 const categoryTile=page.locator('.sf-category-tiles button').first(),categoryName=await categoryTile.innerText();await categoryTile.click();await settle();assert.equal(new URL(page.url()).searchParams.get('categoria'),categoryName);assert.equal(await page.locator('.sf-hero,.sf-collection').count(),0)
 await visit('/catalogo');const stripLink=page.locator('.sf-collection-item').first(),stripId=await stripLink.getAttribute('id');await stripLink.click();await settle();await page.getByRole('link',{name:/Volver al catalogo/}).click();await settle();assert.equal(await page.locator(':focus').getAttribute('id'),stripId)
 cases.push('Promociones y novedades reales, filtros de colección/categoría y retorno con foco a la tira')
 await visit('/catalogo')
 assert.equal(await page.locator('.sf-product-card').count(),24);assert.ok(!await page.getByRole('button',{name:'CHICO',exact:true}).count());assert.ok(!await page.getByRole('button',{name:'MEDIANO',exact:true}).count())
 const search=page.getByRole('searchbox',{name:'Buscar productos'})
 const expected=products.filter(p=>/samsung/i.test(p.nombrePublico)&&/a56/i.test(p.nombrePublico)).length
 await search.pressSequentially('Samsung A56',{delay:30});assert.equal(await search.inputValue(),'Samsung A56');await search.press('Enter');await page.waitForURL('**q=Samsung+A56');await settle();assert.equal(await page.locator('.sf-product-card').count(),expected)
 const savedUrl=page.url();await page.locator('.sf-card-link').first().click();await settle();await page.getByRole('link',{name:/Volver al catalogo/}).click();await settle();assert.equal(page.url(),savedUrl);assert.equal(await search.inputValue(),'Samsung A56');assert.equal(await page.locator('.sf-product-card').count(),expected)
 await search.fill('Samsung A56');await search.press('Enter');await settle();assert.equal(await page.locator('.sf-product-card').count(),expected);cases.push('Escritura y pegado equivalentes, espacio preservado y búsqueda restaurada desde ficha')
 await visit('/catalogo?sort=precio-desc');await page.getByRole('button',{name:'Ver más productos'}).click();await settle();assert.equal(await page.locator('.sf-product-card').count(),48)
 const card=page.locator('.sf-card-link').nth(28);await card.scrollIntoViewIfNeeded();const p=await card.getAttribute('id');await card.click();await settle();const savedScroll=await page.evaluate(()=>history.state.usr.catalogReturn.scroll);await page.getByRole('link',{name:/Volver al catalogo/}).click();await settle();assert.equal(await page.locator('.sf-product-card').count(),48);assert.ok(Math.abs(await page.evaluate(()=>scrollY)-savedScroll)<80,`Scroll guardado ${savedScroll}, actual ${await page.evaluate(()=>scrollY)}`);assert.equal(await page.locator(':focus').getAttribute('id'),p);cases.push('Ver más, orden, posición y foco al volver')
 await page.locator('.sf-card-link').nth(26).click();await settle();await page.goBack();await settle();assert.equal(await page.locator('.sf-product-card').count(),48);assert.ok(await page.evaluate(()=>scrollY)>500);cases.push('Atrás nativo conserva catálogo y posición')
 await visit('/catalogo?lista='+selected.map(p=>p.productoId).join(',')+'&asesorId=a1&asesor=Asesora&storefrontHost='+host);await search.fill('Samsung A56');await search.press('Enter');await settle();for(const key of ['lista','asesorId','asesor','storefrontHost'])assert.ok(new URL(page.url()).searchParams.has(key));assert.equal(await page.locator('.sf-product-card').count(),1);await page.getByRole('button',{name:'Búsqueda: Samsung A56 ×'}).click();await settle();assert.equal(await page.locator('.sf-product-card').count(),2);cases.push('Lista y atribución conservadas al buscar y limpiar')
 await visit('/producto/'+selected[1].productoId);await page.getByRole('button',{name:'Ampliar imagen del producto'}).click();const dialog=page.getByRole('dialog');await dialog.waitFor();assert.equal(await page.locator(':focus').textContent(),'Cerrar imagen');await page.keyboard.press('Tab');assert.equal(await page.locator(':focus').textContent(),'Cerrar imagen');await page.keyboard.press('Escape');assert.equal(await page.locator(':focus').getAttribute('aria-label'),'Ampliar imagen del producto');cases.push('Galería modal, foco contenido, Escape y retorno')
 assert.equal(await page.locator('.sf-description p').evaluate(el=>getComputedStyle(el).whiteSpace),'pre-line');assert.ok(await page.locator('.sf-description p').textContent().then(t=>t.includes('\n')));assert.ok(!await page.getByText('Producto destacado',{exact:true}).count());cases.push('Descripción conserva líneas, precio antes del detalle y sin destacados ficticios')
 if(await page.locator('.sf-delivery').count()){assert.equal(await page.locator('.sf-delivery').getAttribute('open'),null);await page.locator('.sf-delivery summary').focus();await page.keyboard.press('Enter');assert.equal(await page.locator('.sf-delivery').getAttribute('open'),'');await page.keyboard.press('Enter');assert.equal(await page.locator('.sf-delivery').getAttribute('open'),null);cases.push('Condiciones de entrega desplegables con teclado')}
 await page.setViewportSize({width:390,height:844});await visit('/producto/'+selected[0].productoId);assert.ok(await page.getByRole('button',{name:'Comprar por WhatsApp',exact:true}).isVisible());await page.evaluate(()=>scrollTo(0,900));await settle();const box=await page.getByRole('button',{name:'Comprar por WhatsApp',exact:true}).boundingBox();assert.ok(box.y+box.height<=844);assert.ok(!await page.getByRole('link',{name:/Contactar a .*WhatsApp/}).count());await page.getByRole('button',{name:'Comprar por WhatsApp',exact:true}).click();await page.getByRole('dialog').getByText('Ayelen Fernandez',{exact:true}).waitFor();assert.ok(await page.getByText('Responsable de la tienda',{exact:true}).isVisible());await page.screenshot({path:path.join(out,'selector-390.png')});await page.keyboard.press('Escape');cases.push('Acción mobile visible durante scroll y selector con responsable y Ayelén')
 await page.getByRole('button',{name:'Abrir menú'}).click();await page.keyboard.press('Escape');assert.equal(await page.getByRole('button',{name:'Abrir menú'}).getAttribute('aria-expanded'),'false');cases.push('Menú mobile con teclado')
 for(const name of ['empty','error','brand','legacy','large','broken']){scenario=name;await visit(name==='brand'?'/quienes-somos':'/catalogo');await capture('scenario-'+name,390);if(name==='legacy')assert.ok(await page.getByText('Stock 3',{exact:true}).count());if(name==='brand')assert.ok(await page.getByText('Una historia propia',{exact:false}).count())}
 scenario='normal';await visit('/catalogo');await page.evaluate(()=>document.documentElement.style.fontSize='32px');await settle();await capture('text-200',390);await page.evaluate(()=>document.documentElement.style.fontSize='');cases.push('Estados vacíos, error, otro branding, modo anterior, importe extremo, imágenes fallidas y texto ampliado')
 for(const width of [390,1366]){
  await page.setViewportSize({width,height:width===390?844:768})
  for(const shape of ['wide','square','portrait','none','multiple']){
   scenario='banner-'+shape;await visit('/catalogo');await capture(scenario,width)
   if(shape==='none'){assert.equal(await page.locator('.sf-hero-image').count(),0);assert.ok(await page.locator('.sf-hero-text').isVisible())}
   else {const img=page.locator('.sf-hero-image img');assert.equal(await img.evaluate(el=>getComputedStyle(el).objectFit),'contain');assert.ok(await img.evaluate(el=>el.complete&&el.naturalWidth>0));assert.equal(await page.locator('.sf-hero-lead').count(),0)}
   if(shape==='multiple'){await page.getByRole('button',{name:'Portada siguiente'}).click();await page.getByRole('heading',{name:'Otra campaña',exact:true}).waitFor();assert.equal(await page.locator('.sf-hero-controls [role=status]').innerText(),'2 de 2');await page.getByRole('button',{name:'Portada anterior'}).click();await page.getByRole('heading',{name:'Campaña de la tienda',exact:true}).waitFor();const c=await page.locator('.sf-hero-caption').boundingBox(),buttons=await page.locator('.sf-hero-controls').boundingBox();assert.ok(buttons.y>=c.y+c.height-1,'El carrusel no debe superponer texto y controles')}
  }
 }
 cases.push('Banners de otra tienda: horizontal, cuadrado, vertical, sin imagen y carrusel con textos; sin recorte ni superposición en mobile/desktop')
 scenario='banner-multiple';await visit('/catalogo');await page.locator('.sf-hero-art').dispatchEvent('touchstart',{touches:[{identifier:1,clientX:250,clientY:180}]});await page.locator('.sf-hero-art').dispatchEvent('touchend',{changedTouches:[{identifier:1,clientX:100,clientY:183}]});await page.getByRole('heading',{name:'Otra campaña',exact:true}).waitFor();await page.getByRole('button',{name:'Ampliar portada'}).click();await page.getByRole('dialog').waitFor();await page.keyboard.press('Escape');cases.push('Gesto horizontal cambia portada y el siguiente clic permite ampliarla')
 assert.deepEqual(errors,[])
 await fs.writeFile(path.join(work,'navegador-resultados.json'),JSON.stringify({scope:'Build real con API pública reproducida en loopback y estados sintéticos; escrituras bloqueadas.',views,cases,errors,requests,external},null,2))
 const violations=views.flatMap(v=>v.violations.map(x=>({view:v.name,width:v.width,...x})));if(violations.length)throw Error('Accesibilidad: '+JSON.stringify(violations))
 console.log(JSON.stringify({views:views.length,cases:cases.length,errors}))
}catch(e){await page.screenshot({path:path.join(out,'fallo.png')});await fs.writeFile(path.join(work,'navegador-fallo.txt'),e.stack);throw e}finally{await browser.close();await new Promise(r=>server.close(r))}
