import {startPublicPreview} from './catalogo-publico-preview.mjs'
import {spawn} from 'node:child_process'
import fs from 'node:fs/promises'
import path from 'node:path'
import assert from 'node:assert/strict'

const out=path.resolve(import.meta.dirname,'../../artifacts/paso-2-catalogo')
const profile=path.join(out,'chrome-publico-local-profile')
const {server,url,state,requests}=await startPublicPreview()
await fs.mkdir(profile,{recursive:true})
await fs.rm(path.join(profile,'DevToolsActivePort'),{force:true})
const browser=spawn('C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe',[
  '--headless','--disable-gpu','--no-first-run','--no-default-browser-check','--disable-background-networking','--disable-component-update','--remote-debugging-port=0',`--user-data-dir=${profile}`,'about:blank',
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
  await cdp('Page.enable');await cdp('Runtime.enable');await cdp('Fetch.enable',{patterns:[{urlPattern:'*'}]})
  await cdp('Emulation.setDeviceMetricsOverride',{width:1280,height:1000,deviceScaleFactor:1,mobile:false})
  await cdp('Page.navigate',{url:url+'/catalogo?stock=1&sort=stock-desc'})
  await until('document.body.textContent.includes("128.000,25")')
  assert.equal(await evaluate('document.querySelectorAll("article[role=link]").length'),2)
  assert.equal(await evaluate('/Stock|Sin stock|Solo disponibles/.test(document.body.textContent)'),false)
  await fs.writeFile(path.join(out,'catalogo-desktop.png'),Buffer.from((await cdp('Page.captureScreenshot',{format:'png',captureBeyondViewport:true})).data,'base64'))
  await cdp('Page.navigate',{url:url+'/producto/p1?asesorId=ajeno&asesor=Falso'})
  await until('document.body.textContent.includes("Comprar por WhatsApp")')
  assert.equal(await evaluate('Boolean(document.querySelector("[href*=checkout],[href*=resumen-compra]"))'),false)
  const result={modo:'Chrome real, layout y API HTTP simulada local; sin Java, base ni WhatsApp real',desktop:{},mobile:{},teclado:{},estados:{}}
  for(const [name,width,height]of[['desktop',1280,1000],['mobile',390,844]]){
    await cdp('Emulation.setDeviceMetricsOverride',{width,height,deviceScaleFactor:1,mobile:name==='mobile'})
    await evaluate('window.scrollTo(0,0)');await sleep(150)
    result[name]=await evaluate('({ancho:innerWidth,anchoDocumento:document.documentElement.scrollWidth})')
    assert.ok(result[name].anchoDocumento<=width)
    await fs.writeFile(path.join(out,`ficha-${name}.png`),Buffer.from((await cdp('Page.captureScreenshot',{format:'png',captureBeyondViewport:true})).data,'base64'))
    await click('Comprar por WhatsApp');await until('Boolean(document.querySelector("dialog[open] [role=group]"))')
    const metrics=await evaluate('(()=>{const d=document.querySelector("dialog");return {ancho:d.getBoundingClientRect().width,scroll:d.scrollWidth,cliente:d.clientWidth,responsablePrimero:d.querySelector("[role=group] button").textContent.includes("Responsable de la tienda"),foco:d.contains(document.activeElement)}})()')
    assert.ok(metrics.ancho<=width);assert.ok(metrics.scroll<=metrics.cliente+1);assert.ok(metrics.responsablePrimero);assert.ok(metrics.foco)
    result[name].selector=metrics
    await fs.writeFile(path.join(out,`selector-${name}.png`),Buffer.from((await cdp('Page.captureScreenshot',{format:'png'})).data,'base64'))
    for(let i=0;i<8;i++){await cdp('Input.dispatchKeyEvent',{type:'keyDown',key:'Tab',code:'Tab',windowsVirtualKeyCode:9});await cdp('Input.dispatchKeyEvent',{type:'keyUp',key:'Tab',code:'Tab',windowsVirtualKeyCode:9});assert.ok(await evaluate('document.querySelector("dialog").contains(document.activeElement)'))}
    await cdp('Input.dispatchKeyEvent',{type:'keyDown',key:'Escape',code:'Escape',windowsVirtualKeyCode:27});await cdp('Input.dispatchKeyEvent',{type:'keyUp',key:'Escape',code:'Escape',windowsVirtualKeyCode:27})
    await until('!document.querySelector("dialog")')
    assert.equal(await evaluate('document.activeElement.textContent.includes("Comprar por WhatsApp")'),true)
  }
  result.teclado={focoConfinado:true,escapeCierra:true,focoRestaurado:true}
  state.contactos='ninguno';await click('Comprar por WhatsApp');await until('document.body.textContent.includes("no tiene contactos")');result.estados.sinContactos=true;await close()
  state.contactos='error';await click('Comprar por WhatsApp');await until('Boolean(document.querySelector("[role=alert]"))');result.estados.error=true
  state.contactos='uno';await click('Reintentar');await until('document.querySelectorAll("dialog [role=group] button").length===1');result.estados.reintentoYUnContacto=true;await close()
  state.contactos='varios';await click('Comprar por WhatsApp');await until('document.querySelectorAll("dialog [role=group] button").length===2')
  state.precio='140000.00';await click('Ana · Asesora');await until('document.body.textContent.includes("El precio o el contacto cambió")')
  assert.equal(await evaluate('window.__catalogoNavigations.length'),0)
  await click('Ana · Asesora');await until('window.__catalogoNavigations.length===1')
  const destination=await evaluate('window.__catalogoNavigations[0]')
  assert.ok(new URL(destination).pathname==='/5491112345679');assert.ok(new URL(destination).searchParams.get('text').includes('140.000,00'))
  result.estados.precioRevalidadoYDestinatarioExpreso=true
  result.erroresJavaScript=errors;result.solicitudesExternasBloqueadas=external
  assert.deepEqual(errors,[]);assert.deepEqual(external,[])
  result.sinSolicitudesDePedido=requests.every(r=>!r.path.includes('/pedido'))
  assert.ok(result.sinSolicitudesDePedido)
  await fs.writeFile(path.join(out,'verificacion-navegador.json'),JSON.stringify(result,null,2))
  await fs.writeFile(path.join(out,'solicitudes-navegador.json'),JSON.stringify(requests,null,2))
  console.log(JSON.stringify(result,null,2))
  await cdp('Browser.close')
} finally {
  socket?.close();browser.kill();server.close();await sleep(500)
  const realProfile=await fs.realpath(profile),realOut=await fs.realpath(out)
  assert.ok(realProfile.startsWith(realOut+path.sep)&&path.basename(realProfile)==='chrome-publico-local-profile')
  await fs.rm(realProfile,{recursive:true,force:true,maxRetries:10,retryDelay:200})
}
