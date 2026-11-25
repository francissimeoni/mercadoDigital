import { startTransitionPreview } from './catalogo-transicion-preview.mjs'
import { spawn } from 'node:child_process'
import fs from 'node:fs/promises'
import path from 'node:path'
import assert from 'node:assert/strict'

const out = path.resolve(import.meta.dirname, '../../artifacts/paso-4-catalogo')
const profile = path.join(out, 'chrome-pagos-local-profile')
const { server, url, requests, order } = await startTransitionPreview()
await fs.mkdir(profile, { recursive: true })
const browser = spawn('C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe', [
  '--headless', '--disable-gpu', '--no-first-run', '--no-default-browser-check',
  '--disable-background-networking', '--disable-component-update',
  '--remote-debugging-port=0', `--user-data-dir=${profile}`, 'about:blank',
], { windowsHide: true, stdio: 'ignore' })
let socket
const sleep = ms => new Promise(resolve => setTimeout(resolve, ms))
try {
  let port
  for (let i = 0; i < 100; i++) {
    try { port = (await fs.readFile(path.join(profile, 'DevToolsActivePort'), 'utf8')).split('\n')[0]; break }
    catch { await sleep(100) }
  }
  assert.ok(port, 'Chrome no abrió el puerto de control')
  const pages = await (await fetch(`http://127.0.0.1:${port}/json/list`)).json()
  socket = new WebSocket(pages.find(page => page.type === 'page').webSocketDebuggerUrl)
  await new Promise((resolve, reject) => {
    socket.addEventListener('open', resolve, { once: true })
    socket.addEventListener('error', reject, { once: true })
  })
  let id = 0
  const pending = new Map(), errors = [], external = []
  const cdp = (method, params = {}) => new Promise((resolve, reject) => {
    const next = ++id
    pending.set(next, { resolve, reject })
    socket.send(JSON.stringify({ id: next, method, params }))
  })
  socket.addEventListener('message', event => {
    const message = JSON.parse(event.data)
    if (message.method === 'Runtime.exceptionThrown') errors.push(message.params.exceptionDetails.text)
    if (message.method === 'Fetch.requestPaused') {
      const request = message.params
      if (request.request.url.startsWith(url + '/') || request.request.url.startsWith('data:')) {
        void cdp('Fetch.continueRequest', { requestId: request.requestId })
      } else {
        external.push(request.request.url)
        void cdp('Fetch.failRequest', { requestId: request.requestId, errorReason: 'BlockedByClient' })
      }
    }
    if (message.id && pending.has(message.id)) {
      const promise = pending.get(message.id)
      pending.delete(message.id)
      message.error ? promise.reject(Error(JSON.stringify(message.error))) : promise.resolve(message.result)
    }
  })
  const evaluate = async expression => {
    const result = await cdp('Runtime.evaluate', { expression, returnByValue: true })
    if (result.exceptionDetails) throw Error(result.exceptionDetails.exception?.description ?? result.exceptionDetails.text)
    return result.result.value
  }
  const until = async expression => {
    for (let i = 0; i < 100; i++) { if (await evaluate(expression)) return; await sleep(100) }
    throw Error('Tiempo agotado: ' + expression)
  }
  await cdp('Page.enable')
  await cdp('Runtime.enable')
  await cdp('Fetch.enable', { patterns: [{ urlPattern: '*' }] })
  await cdp('Emulation.setDeviceMetricsOverride', { width: 1280, height: 900, deviceScaleFactor: 1, mobile: false })
  await cdp('Page.navigate', { url: url + '/catalogo?escenario=anterior' })
  await until('document.body.textContent.includes("Comprar")')
  const key = 'storefront-checkout:REVENDEDOR:r1:p1'
  const saved = JSON.stringify({ pedidoCreado: order, updatedAt: 1 })
  await evaluate(`sessionStorage.setItem(${JSON.stringify(key)},${JSON.stringify(saved)})`)
  const cases = [
    ['PENDIENTE', 'CREADO', 'Estamos verificando tu pago'],
    ['CONFIRMADO', 'PAGADO', 'Pago confirmado'],
    ['APROBADO', 'CREADO', 'Estamos verificando tu pago'],
    ['RECHAZADO', 'CREADO', 'Pago no completado'],
    ['REEMBOLSADO', 'PAGADO', 'Pago reembolsado'],
    ['CANCELADO', 'CANCELADO', 'Pago no completado'],
  ]
  const result = { modo: 'Chrome real con componentes React y API sintética; sin Java, MySQL o PSP real.', casos: {}, medidas: {}, erroresJavaScript: errors, externasBloqueadas: external }
  for (const [payment, pedido, expected] of cases) {
    order.estadoPago = payment
    order.estadoPedido = pedido
    await cdp('Page.navigate', { url: url + '/pago/retorno?escenario=anterior&status=success' })
    await until(`document.querySelector('h1')?.textContent === ${JSON.stringify(expected)}`)
    assert.equal(await evaluate('document.querySelector("h1").textContent'), expected)
    assert.equal(await evaluate(`Boolean(sessionStorage.getItem(${JSON.stringify(key)}))`), true)
    result.casos[payment] = true
    if (payment === 'RECHAZADO') {
      await cdp('Emulation.setDeviceMetricsOverride', { width: 390, height: 844, deviceScaleFactor: 1, mobile: true })
      await sleep(150)
      result.medidas.mobile = await evaluate('({ancho:innerWidth,documento:document.documentElement.scrollWidth})')
      assert.ok(result.medidas.mobile.documento <= 390)
      await fs.writeFile(path.join(out, 'retorno-rechazado-mobile.png'), Buffer.from((await cdp('Page.captureScreenshot', { format: 'png', captureBeyondViewport: true })).data, 'base64'))
      await cdp('Emulation.setDeviceMetricsOverride', { width: 1280, height: 900, deviceScaleFactor: 1, mobile: false })
    }
    if (payment === 'REEMBOLSADO') {
      result.medidas.desktop = await evaluate('({ancho:innerWidth,documento:document.documentElement.scrollWidth})')
      assert.ok(result.medidas.desktop.documento <= 1280)
      await fs.writeFile(path.join(out, 'retorno-reembolsado-desktop.png'), Buffer.from((await cdp('Page.captureScreenshot', { format: 'png', captureBeyondViewport: true })).data, 'base64'))
    }
  }
  assert.deepEqual(errors, [])
  assert.deepEqual(external, [])
  assert.equal(requests.filter(request => request.method === 'POST' && /\/pedido\/.+\/pago/.test(request.path)).length, 0)
  await fs.writeFile(path.join(out, 'verificacion-navegador.json'), JSON.stringify(result, null, 2))
  console.log(JSON.stringify(result, null, 2))
  await cdp('Browser.close')
} finally {
  socket?.close()
  browser.kill()
  server.close()
  await sleep(500)
  const realProfile = await fs.realpath(profile), realOut = await fs.realpath(out)
  assert.ok(realProfile.startsWith(realOut + path.sep) && path.basename(realProfile) === 'chrome-pagos-local-profile')
  await fs.rm(realProfile, { recursive: true, force: true, maxRetries: 10, retryDelay: 200 })
}
