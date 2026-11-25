import {startTransitionPreview} from './catalogo-transicion-preview.mjs'
import {spawn} from 'node:child_process'
import fs from 'node:fs/promises'
import path from 'node:path'
import assert from 'node:assert/strict'

const out=process.env.CATALOGO_TRANSICION_OUT ?? path.resolve(import.meta.dirname,'../../artifacts/paso-3-catalogo')
const profile=path.join(out,'chrome-transicion-local-profile')
const {server,url,state,requests,order}=await startTransitionPreview()
await fs.mkdir(profile,{recursive:true})
await fs.rm(path.join(profile,'DevToolsActivePort'),{force:true})
const browser=spawn(process.env.CATALOGO_CHROME ?? 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',[
  ...(process.platform==='linux'?['--no-sandbox']:[]),'--headless','--disable-gpu','--no-first-run','--no-default-browser-check','--disable-background-networking','--disable-component-update','--remote-debugging-port=0',`--user-data-dir=${profile}`,'about:blank',
],{windowsHide:true,stdio:'ignore'})
let socket
const sleep=ms=>new Promise(resolve=>setTimeout(resolve,ms))
try {
  let port
  for(let i=0;i<100;i++){try{port=(await fs.readFile(path.join(profile,'DevToolsActivePort'),'utf8')).split('\n')[0];break}catch{await sleep(100)}}
  assert.ok(port)
  const pages=await(await fetch(`http://127.0.0.1:${port}/json/list`)).json()
  socket=new WebSocket(pages.find(p=>p.type==='page').webSocketDebuggerUrl)
  await new Promise((resolve,reject)=>{socket.addEventListener('open',resolve,{once:true});socket.addEventListener('error',reject,{once:true})})
  let id=0
  const pending=new Map(),errors=[],external=[]
  const cdp=(method,params={})=>new Promise((resolve,reject)=>{const next=++id;pending.set(next,{resolve,reject});socket.send(JSON.stringify({id:next,method,params}))})
  socket.addEventListener('message',event=>{
    const message=JSON.parse(event.data)
    if(message.method==='Runtime.exceptionThrown')errors.push(message.params.exceptionDetails.text)
    if(message.method==='Fetch.requestPaused') {
      const req=message.params
      if(req.request.url.startsWith(url+'/') || req.request.url.startsWith('data:')) void cdp('Fetch.continueRequest',{requestId:req.requestId})
      else {external.push(req.request.url);void cdp('Fetch.failRequest',{requestId:req.requestId,errorReason:'BlockedByClient'})}
    }
    if(message.id&&pending.has(message.id)){const p=pending.get(message.id);pending.delete(message.id);message.error?p.reject(Error(JSON.stringify(message.error))):p.resolve(message.result)}
  })
  const evaluate=async expression=>{const r=await cdp('Runtime.evaluate',{expression,returnByValue:true});if(r.exceptionDetails)throw Error(r.exceptionDetails.exception?.description ?? r.exceptionDetails.text);return r.result.value}
  const until=async expression=>{for(let i=0;i<100;i++){if(await evaluate(expression))return;await sleep(100)}throw Error('Tiempo agotado: '+expression)}
  const click=async text=>evaluate(`[...document.querySelectorAll('button')].find(b=>b.textContent.includes(${JSON.stringify(text)})).click()`)
  const close=async()=>evaluate(`document.querySelector('[aria-label="Cerrar selector de contactos"]').click()`)

  await cdp('Page.enable'); await cdp('Runtime.enable'); await cdp('Fetch.enable',{patterns:[{urlPattern:'*'}]});
  const navigate=async route=>cdp('Page.navigate',{url:url+route});
  const capture=async name=>fs.writeFile(path.join(out,name+'.png'),Buffer.from((await cdp('Page.captureScreenshot',{format:'png',captureBeyondViewport:true})).data,'base64'));
  const result={modo:'Chrome real con API, sesión y tenant sintéticos. Sin Java/MySQL, pagos ni mensajes reales.',casos:{},medidas:{}};
  const checkoutKey='storefront-checkout:REVENDEDOR:r1:p1',otherKey='storefront-checkout:REVENDEDOR:r2:p1';
  await cdp('Emulation.setDeviceMetricsOverride',{width:1280,height:1000,deviceScaleFactor:1,mobile:false});
  await navigate('/catalogo'); await until('document.body.textContent.includes("128.000,25")');
  await evaluate('sessionStorage.setItem('+JSON.stringify(otherKey)+',"conservar"); localStorage.setItem("otra-sesion","conservar")');
  await navigate('/producto/p1/checkout?seguro=1');
  await until('location.pathname==="/producto/p1" && document.body.textContent.includes("Ahora esta tienda")');
  assert.equal(await evaluate('Boolean(document.querySelector("form:not([role=search]),[href*=checkout],[href*=resumen-compra]"))'),false);
  await capture('borrador-catalogo-desktop'); result.casos.borradorSinCheckout=true;
  await navigate('/producto/p1/resumen-compra');
  await until('location.pathname==="/producto/p1"'); result.casos.resumenSinCheckout=true;
  await evaluate('sessionStorage.setItem('+JSON.stringify(checkoutKey)+','+JSON.stringify(JSON.stringify({pedidoCreado:order,pagoPreparado:{checkoutUrl:'https://no-confiable.invalid/'},updatedAt:1}))+')');
  await navigate('/producto/p1/checkout');
  await until('location.pathname==="/pedido/tracking-anterior" && document.body.textContent.includes("ya no procesa pagos")');
  assert.equal(await evaluate('Boolean(document.querySelector("a[href*=no-confiable]"))'),false);
  result.casos.pedidoAnteriorValidado=true;
  for(const [name,width,height]of[['desktop',1280,1000],['mobile',390,844]]){
    await cdp('Emulation.setDeviceMetricsOverride',{width,height,deviceScaleFactor:1,mobile:name==='mobile'});
    await sleep(100);result.medidas[name]=await evaluate('({ancho:innerWidth,documento:document.documentElement.scrollWidth})');
    assert.ok(result.medidas[name].documento<=width);await capture('pedido-anterior-'+name);
  }
  assert.equal(requests.filter(r=>r.method==='POST'&&r.path.endsWith('/reanudar-pago')).length,0);
  assert.equal(await evaluate('Boolean(document.querySelector("a[href*=mercadopago]"))'),false);
  result.casos.pagoRetiradoSinReapertura=true;
  await cdp('Page.reload'); await until('document.body.textContent.includes("ya no procesa pagos")');
  assert.equal(requests.filter(r=>r.method==='POST'&&r.path.endsWith('/reanudar-pago')).length,0);result.casos.recargaSinCobro=true;
  await navigate('/pago/retorno?status=failure');await until('location.pathname==="/pedido/tracking-anterior" && document.body.textContent.includes("ya no procesa pagos")');
  assert.equal(requests.filter(r=>r.method==='POST'&&r.path.endsWith('/reanudar-pago')).length,0);result.casos.retornoConsultaPedidoSinConfiarEnStatusUrl=true;
  state.trackingError=true; await navigate('/producto/p1/checkout'); await until('document.body.textContent.includes("Pedido no encontrado")');
  assert.equal(await evaluate('Boolean([...document.querySelectorAll("button")].find(b=>b.textContent.includes("Retomar pago")))'),false);state.trackingError=false;result.casos.tokenInvalidoSinPago=true;
  await evaluate('sessionStorage.removeItem('+JSON.stringify(checkoutKey)+')');
  await navigate('/producto/p1/checkout?escenario=desactualizado'); await until('location.pathname==="/producto/p1"');result.casos.contextoAnteriorRevalidado=true;
  state.resolveError=true;await navigate('/producto/p1/checkout?escenario=desactualizado');await until('document.body.textContent.includes("No se pudo verificar")');
  assert.equal(await evaluate('Boolean(document.querySelector("form:not([role=search])"))'),false);state.resolveError=false;await click('Reintentar');await until('location.pathname==="/producto/p1"');result.casos.errorResolucionYReintento=true;
  await navigate('/producto/p1/checkout?escenario=anterior');await until('document.body.textContent.includes("Finalizar compra")');assert.equal(await evaluate('Boolean(document.querySelector("input[type=email]"))'),true);result.casos.modoAnteriorConservado=true;
  await navigate('/producto/p1/checkout?escenario=proveedor');await until('document.body.textContent.includes("Esta tienda no recibe pagos por la plataforma")');assert.equal(await evaluate('Boolean(document.querySelector("input[type=email]"))'),false);result.casos.proveedorSinPasarela=true;
  const session={token:'sesion-sintetica-local',email:'cliente@test.invalid',nombre:'Cliente de prueba',issuedAt:'2026-11-24',revendedorId:null,clienteId:'c1',asesorId:null,roles:['CLIENTE']};
  await evaluate('localStorage.setItem("md:storefront-session",'+JSON.stringify(JSON.stringify(session))+')');
  await navigate('/mi-cuenta');await until('document.body.textContent.includes("Pedidos anteriores") && document.body.textContent.includes("Ver seguimiento")');
  result.medidas.cuentaMobile=await evaluate('({ancho:innerWidth,documento:document.documentElement.scrollWidth})');assert.ok(result.medidas.cuentaMobile.documento<=390);
  await capture('cuenta-historica-mobile');await evaluate('[...document.querySelectorAll("a")].find(a=>a.getAttribute("href")==="/pedido/tracking-anterior").click()');await until('location.pathname==="/pedido/tracking-anterior"');
  await evaluate('history.back()');await until('location.pathname==="/mi-cuenta" && document.body.textContent.includes("Ver seguimiento")');
  await evaluate('history.forward()');await until('location.pathname==="/pedido/tracking-anterior"');
  await cdp('Page.reload');await until('document.body.textContent.includes("ya no procesa pagos")');result.casos.cuentaAtrasAdelanteYRecarga=true;
  state.emptyOrders=true;await navigate('/mi-cuenta');await until('document.body.textContent.includes("no se registran automáticamente")');await capture('cuenta-sin-pedidos-mobile');state.emptyOrders=false;
  const owner={...session,roles:['REVENDEDOR','ADMIN_MD'],revendedorId:'r1',clienteId:null,nombre:'Responsable de prueba'};
  await evaluate('localStorage.setItem("md:storefront-session",'+JSON.stringify(JSON.stringify(owner))+')');
  await navigate('/mi-tienda');await until('document.body.textContent.includes("Agregar producto")');
  assert.equal(await evaluate('Boolean([...document.querySelectorAll("button")].find(b=>b.textContent==="Importar productos"))'),false);
  assert.equal(await evaluate('document.body.textContent.includes("Pedidos anteriores") || document.body.textContent.includes("Consultas anteriores")'),false);
  result.medidas.panelMobile=await evaluate('({ancho:innerWidth,documento:document.documentElement.scrollWidth})');assert.ok(result.medidas.panelMobile.documento<=390);
  assert.equal(await evaluate('document.body.textContent.includes("Precio anterior ARS")'),false);
  await capture('panel-catalogo-mobile');result.casos.panelSinImportacionAnterior=true;
  await evaluate('const select=document.querySelector("nav select");select.value="precios";select.dispatchEvent(new Event("change",{bubbles:true}))');await until('location.search.includes("precios")');await evaluate('history.back()');await until('document.body.textContent.includes("Agregar producto")');result.casos.panelRespetaHistorialDeNavegacion=true;
  await navigate('/pricing');await until('document.body.textContent.includes("Tu tienda funciona como catálogo")');assert.equal(await evaluate('document.body.textContent.includes("Formulario anterior habilitado")'),false);
  state.privateMode=false;await cdp('Page.reload');await until('document.body.textContent.includes("Formulario anterior habilitado")');state.privateMode=true;result.casos.accesoPrivadoPorModo=true;
  assert.equal(await evaluate('sessionStorage.getItem('+JSON.stringify(otherKey)+')'), 'conservar');assert.equal(await evaluate('localStorage.getItem("otra-sesion")'),'conservar');result.casos.otrasTiendasConservadas=true;
  await navigate('/catalogo');await until('document.body.textContent.includes("Comprar")');await click('Comprar');await until('Boolean(document.querySelector("dialog[open]"))');
  await cdp('Input.dispatchKeyEvent',{type:'keyDown',key:'Escape',code:'Escape',windowsVirtualKeyCode:27});await until('!document.querySelector("dialog[open]")');
  assert.equal(await evaluate('document.activeElement?.textContent.includes("Comprar")'),true);result.casos.selectorYFocoConservados=true;
  result.erroresJavaScript=errors;result.externasBloqueadas=external;
  result.operacionesNuevas=requests.filter(r=>r.method!=='GET'&&r.path.includes('/pedido')&&!r.path.endsWith('/reanudar-pago'));
  assert.deepEqual(errors,[]);assert.deepEqual(external,[]);assert.deepEqual(result.operacionesNuevas,[]);
  await fs.writeFile(path.join(out,'verificacion-navegador.json'),JSON.stringify(result,null,2));
  await fs.writeFile(path.join(out,'solicitudes-navegador.json'),JSON.stringify(requests,null,2));
  // The page WebSocket can close before Browser.close replies. Do not await a
  // response from a target that is shutting down after the assertions passed.
  console.log(JSON.stringify(result,null,2));socket.send(JSON.stringify({id:++id,method:'Browser.close'}));
} finally {
  socket?.close();browser.kill();server.closeAllConnections();server.close();await sleep(500)
  const realProfile=await fs.realpath(profile),realOut=await fs.realpath(out)
  assert.ok(realProfile.startsWith(realOut+path.sep)&&path.basename(realProfile)==='chrome-transicion-local-profile')
  await fs.rm(realProfile,{recursive:true,force:true,maxRetries:10,retryDelay:200})
}
