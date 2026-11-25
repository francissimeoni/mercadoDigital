import { startPreview } from './catalogo-ficha-preview.mjs'
import { spawn } from 'node:child_process'
import fs from 'node:fs/promises'
import path from 'node:path'
import assert from 'node:assert/strict'

const out = path.resolve(import.meta.dirname, '../../artifacts/paso-5-catalogo')
const profile = path.join(out, 'chrome-local-profile')
const { server, url } = await startPreview()
await fs.mkdir(profile, { recursive: true })
await fs.rm(path.join(profile, 'DevToolsActivePort'), { force: true })
const browser = spawn('C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe', [
  '--headless', '--disable-gpu', '--no-first-run', '--no-default-browser-check',
  '--disable-background-networking', '--disable-component-update', '--remote-debugging-port=0',
  `--user-data-dir=${profile}`, 'about:blank',
], { windowsHide: true, stdio: 'ignore' })
let socket
const sleep = ms => new Promise(resolve => setTimeout(resolve, ms))
try {
  let port
  for (let i = 0; i < 100; i++) {
    try { port = (await fs.readFile(path.join(profile, 'DevToolsActivePort'), 'utf8')).split('\n')[0]; break } catch { await sleep(100) }
  }
  assert.ok(port, 'Chrome expone un puerto local de depuración')
  const pages = await (await fetch(`http://127.0.0.1:${port}/json/list`)).json()
  socket = new WebSocket(pages.find(p => p.type === 'page').webSocketDebuggerUrl)
  await new Promise((resolve, reject) => { socket.addEventListener('open', resolve, { once: true }); socket.addEventListener('error', reject, { once: true }) })
  let id = 0
  const pending = new Map(), browserErrors = []
  socket.addEventListener('message', event => {
    const message = JSON.parse(event.data)
    if (message.method === 'Runtime.exceptionThrown') browserErrors.push(message.params.exceptionDetails.text)
    if (message.id && pending.has(message.id)) { const p = pending.get(message.id); pending.delete(message.id); if (message.error) p.reject(Error(JSON.stringify(message.error))); else p.resolve(message.result) }
  })
  const cdp = (method, params = {}) => new Promise((resolve, reject) => { const next = ++id; pending.set(next, { resolve, reject }); socket.send(JSON.stringify({ id: next, method, params })) })
  const evaluate = async expression => {
    const value = await cdp('Runtime.evaluate', { expression, returnByValue: true })
    if (value.exceptionDetails) throw Error(value.exceptionDetails.exception?.description ?? value.exceptionDetails.text)
    return value.result.value
  }
  await cdp('Page.enable'); await cdp('Runtime.enable')
  await cdp('Page.navigate', { url })
  for (let i = 0; i < 100 && !await evaluate('Boolean(document.querySelector("form"))'); i++) await sleep(100)
  assert.equal(await evaluate('Boolean(document.querySelector("form"))'), true)
  const waitFor = async expression => {
    for (let i = 0; i < 100; i++) { if (await evaluate(expression)) return; await sleep(100) }
    throw Error('No se alcanzó: ' + expression + '\n' + await evaluate('document.body.textContent'))
  }
  await evaluate('[...document.querySelectorAll("button")].find(b=>b.textContent.includes("Editar ficha y precio")).click()')
  await waitFor('Boolean(document.querySelector("#catalogo-ficha")) && document.querySelector("#catalogo-ficha").textContent.includes("128.000")')
  assert.equal(await evaluate('document.querySelector("#catalogo-ficha").textContent.includes("Precio anterior")'), false)
  await evaluate('[...document.querySelectorAll("button")].find(b=>b.textContent==="Editar").click()')
  await waitFor('document.body.textContent.includes("Editar categoría")')
  await evaluate(`(()=>{const e=document.querySelector('input[aria-label="Costo en ARS"]');Object.getOwnPropertyDescriptor(HTMLInputElement.prototype,"value").set.call(e,"10000,00");e.dispatchEvent(new Event("input",{bubbles:true}))})()`)
  await sleep(100)
  await evaluate('[...document.querySelectorAll("button")].find(b=>b.textContent.includes("Revisar impacto en precios")).click()')
  await waitFor('document.body.textContent.includes("130.000")')
  assert.equal(await evaluate('window.__writes.length'), 0, 'La vista previa no guarda el costo')
  await evaluate('[...document.querySelectorAll("button")].find(b=>b.textContent.includes("Confirmar y guardar categoría")).click()')
  await waitFor('window.__writes.length===1 && document.querySelector("#catalogo-ficha").textContent.includes("130.000")')
  const result = { modo: 'Chrome real con API sintética; sin backend, base ni WhatsApp', incremento: await evaluate('window.__writes[0]'), desktop: {}, mobile: {} }
  assert.equal(result.incremento.body.costoArs, '10000.00')
  assert.equal(result.incremento.body.impactoToken, 'impacto-sintetico')
  for (const [name, width, height] of [['desktop', 1280, 1000], ['mobile', 390, 844]]) {
    await cdp('Emulation.setDeviceMetricsOverride', { width, height, deviceScaleFactor: 1, mobile: name === 'mobile' })
    await evaluate('document.querySelector("#catalogo-ficha").scrollIntoView()'); await sleep(200)
    result[name] = await evaluate('({ancho:innerWidth,anchoDocumento:document.documentElement.scrollWidth,camposSinEtiqueta:[...document.querySelectorAll("input,select,textarea")].filter(e=>!e.labels?.length&&!e.getAttribute("aria-label")).length})')
    assert.ok(result[name].anchoDocumento <= result[name].ancho, `Sin desborde en ${name}`)
    assert.equal(result[name].camposSinEtiqueta, 0)
    const screenshot = await cdp('Page.captureScreenshot', { format: 'png' })
    await fs.writeFile(path.join(out, `ficha-${name}.png`), Buffer.from(screenshot.data, 'base64'))
    if (name === 'mobile') {
      await evaluate('document.querySelector("#catalogo-ficha").lastElementChild.scrollIntoView({block:"end"})'); await sleep(100)
      const detail = await cdp('Page.captureScreenshot', { format: 'png' })
      await fs.writeFile(path.join(out, 'ficha-mobile-precio.png'), Buffer.from(detail.data, 'base64'))
    }
  }
  await evaluate('document.querySelector("#catalogo-ficha input").focus()')
  await cdp('Input.dispatchKeyEvent', { type: 'keyDown', key: 'Tab', code: 'Tab', windowsVirtualKeyCode: 9 })
  await cdp('Input.dispatchKeyEvent', { type: 'keyUp', key: 'Tab', code: 'Tab', windowsVirtualKeyCode: 9 })
  result.teclado = await evaluate('document.activeElement.closest("label")?.textContent')
  assert.match(result.teclado, /Nombre del producto/)
  assert.deepEqual(browserErrors, [])
  await fs.writeFile(path.join(out, 'verificacion-navegador.json'), JSON.stringify(result, null, 2))
  console.log(JSON.stringify(result, null, 2))
} finally {
  socket?.close(); browser.kill(); await new Promise(resolve => server.close(resolve))
}
