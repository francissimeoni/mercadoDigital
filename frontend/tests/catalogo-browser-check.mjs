import { startPreview } from './catalogo-preview.mjs'
import { spawn } from 'node:child_process'
import fs from 'node:fs/promises'
import path from 'node:path'
import assert from 'node:assert/strict'

const out = path.resolve(import.meta.dirname, '../../artifacts/paso-1-catalogo')
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
  const evaluate = async expression => (await cdp('Runtime.evaluate', { expression, returnByValue: true })).result.value
  await cdp('Page.enable'); await cdp('Runtime.enable')
  await cdp('Page.navigate', { url })
  for (let i = 0; i < 100 && !await evaluate('Boolean(document.querySelector("form"))'); i++) await sleep(100)
  assert.equal(await evaluate('Boolean(document.querySelector("form"))'), true)
  await evaluate('[...document.querySelectorAll("button")].find(b=>b.textContent.includes("Configurar precio")).click()')
  for (let i = 0; i < 100 && !await evaluate('document.body.textContent.includes("128.000")'); i++) await sleep(100)
  assert.equal(await evaluate('document.body.textContent.includes("128.000")'), true)
  const result = { modo: 'Navegador real con transporte simulado, sin backend ni base', desktop: {}, mobile: {} }
  for (const [name, width, height] of [['desktop', 1280, 1000], ['mobile', 390, 844]]) {
    await cdp('Emulation.setDeviceMetricsOverride', { width, height, deviceScaleFactor: 1, mobile: name === 'mobile' })
    await evaluate('window.scrollTo(0,0)'); await sleep(200)
    result[name] = await evaluate('({ancho:innerWidth,anchoDocumento:document.documentElement.scrollWidth,camposSinEtiqueta:[...document.querySelectorAll("input,select,textarea")].filter(e=>!e.labels?.length&&!e.getAttribute("aria-label")).length})')
    assert.equal(result[name].anchoDocumento <= result[name].ancho, true, `Sin desborde horizontal en ${name}`)
    assert.equal(result[name].camposSinEtiqueta, 0, 'Todos los campos tienen etiqueta')
    const image = await cdp('Page.captureScreenshot', { format: 'png' })
    await fs.writeFile(path.join(out, `catalogo-${name}.png`), Buffer.from(image.data, 'base64'))
    if (name === 'mobile') {
      await evaluate('[...document.querySelectorAll("h3")].find(h=>h.textContent.includes("Precio de catálogo")).scrollIntoView()')
      await sleep(100)
      const detail = await cdp('Page.captureScreenshot', { format: 'png' })
      await fs.writeFile(path.join(out, 'catalogo-mobile-precio.png'), Buffer.from(detail.data, 'base64'))
    }
  }
  await evaluate('document.querySelector("input").focus()')
  await cdp('Input.dispatchKeyEvent', { type: 'keyDown', key: 'Tab', code: 'Tab', windowsVirtualKeyCode: 9 })
  await cdp('Input.dispatchKeyEvent', { type: 'keyUp', key: 'Tab', code: 'Tab', windowsVirtualKeyCode: 9 })
  result.teclado = await evaluate('({tipo:document.activeElement.type,etiqueta:document.activeElement.closest("label")?.textContent})')
  assert.equal(result.teclado.tipo, 'tel', 'Tab pasa de cotización al WhatsApp comercial')
  assert.deepEqual(browserErrors, [])
  result.erroresJavaScript = browserErrors
  await fs.writeFile(path.join(out, 'verificacion-navegador.json'), JSON.stringify(result, null, 2))
  console.log(JSON.stringify(result))
  await cdp('Browser.close')
} finally {
  socket?.close(); browser.kill(); server.close()
  await sleep(500)
  // El perfil es desechable. Verificar el destino real antes de borrarlo recursivamente en Windows.
  const realProfile = await fs.realpath(profile)
  const realOut = await fs.realpath(out)
  assert.ok(realProfile.startsWith(realOut + path.sep) && path.basename(realProfile) === 'chrome-local-profile')
  await fs.rm(realProfile, { recursive: true, force: true, maxRetries: 10, retryDelay: 200 })
}
