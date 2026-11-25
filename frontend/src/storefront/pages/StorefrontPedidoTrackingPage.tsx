import axios from "axios"
import { useMutation } from "@tanstack/react-query"
import { Link, useParams, useSearchParams } from "react-router-dom"
import { confirmarStorefrontEntrega } from "../api/storefrontTrackingApi"
import StorefrontErrorState from "../components/StorefrontErrorState"
import StorefrontHistoricalPayment from "../components/StorefrontHistoricalPayment"
import { usePedidoTracking } from "../hooks/usePedidoTracking"
import { useTenant } from "../../tenant/useTenant"
import { normalizeWhatsAppPhone } from "../utils/storefrontContact"
import { resolveStorefrontAssetUrl } from "../utils/storefrontBranding"

// Production OR/UR, offsets 764684/771797; direct assistance replaces the old RMA UI.
export default function StorefrontTrackingRecovered() {
  const { trackingToken } = useParams()
  const [params] = useSearchParams()
  const tenant = useTenant()
  const accent = tenant.branding?.colorPrimario || "#0f172a"
  const tracking = usePedidoTracking(trackingToken, tenant.host)
  const confirm = useMutation({ mutationFn: () => confirmarStorefrontEntrega(trackingToken!, tenant.host!), onSuccess: async () => { await tracking.refetch() } })
  if (!trackingToken || !tenant.host) return <StorefrontErrorState title="Seguimiento no disponible" message="El enlace de seguimiento no es válido para esta tienda." />
  if (tracking.isLoading) return <div role="status" className="animate-pulse rounded-3xl bg-slate-100 p-12">Cargando seguimiento...</div>
  if (tracking.isError || !tracking.data) {
    const missing = axios.isAxiosError(tracking.error) && tracking.error.response?.status === 404
    return <StorefrontErrorState title={missing ? "Pedido no encontrado" : "No se pudo consultar el pedido"} message={missing ? "No encontramos una compra asociada a este enlace." : "Ocurrió un problema temporal. Intentá nuevamente en unos segundos."} actionLabel={missing ? undefined : "Reintentar"} onAction={missing ? undefined : () => void tracking.refetch()} />
  }
  const data = tracking.data
  const state = (data.envio.estadoEnvio || "").trim().toUpperCase()
  const paymentState = (data.estadoPago || "").trim().toUpperCase()
  const paid = paymentState === "CONFIRMADO" && data.estadoPedido !== "CANCELADO"
  const paymentStep = paid
    ? { title: "Pago confirmado", detail: "La tienda recibió la confirmación del pago." }
    : paymentState === "REEMBOLSADO"
      ? { title: "Pago reembolsado", detail: "El pago figura como reembolsado. Contactá a la tienda si necesitás más información." }
      : paymentState === "RECHAZADO" || paymentState === "CANCELADO"
        ? { title: "Pago no completado", detail: "El pago fue rechazado o cancelado. Consultá a la tienda antes de volver a intentar." }
        : { title: "Pago en verificación", detail: "Aún no consta la confirmación final del pago o del comprobante." }
  const delivered = state === "ENTREGADO"
  const sent = ["DESPACHADO", "ENTREGADO"].includes(state)
  const preparing = ["PENDIENTE_PREPARACION", "LISTO_PARA_RETIRO", "RETIRADO", "DESPACHADO", "ENTREGADO"].includes(state)
  const steps = [
    { ...paymentStep, state: paid ? "completed" : "current" },
    { title: "Preparación", detail: "La tienda prepara y controla el producto antes del despacho.", state: paid ? sent ? "completed" : preparing ? "current" : "upcoming" : "upcoming" },
    { title: "En camino", detail: "El pedido fue entregado al transporte para llegar a destino.", state: delivered ? "completed" : sent ? "current" : "upcoming" },
    { title: "Entregado", detail: "El pedido llegó a destino y la entrega quedó confirmada.", state: delivered ? "completed" : "upcoming" },
  ]
  const shortId = data.pedidoId.slice(0, 8).toUpperCase()
  const store = tenant.branding?.nombreComercial?.trim() || "la tienda"
  const phone = tenant.branding?.telefonoComercial ? normalizeWhatsAppPhone(tenant.branding.telefonoComercial) : null
  const message = [`Hola ${store}, necesito ayuda con el pedido #${shortId}.`, `Producto: ${data.items.map(item => item.nombreProducto).join(", ")}.`, `Seguimiento: ${window.location.href.split("?")[0]}`].join("\n")
  const panel = "rounded-[2rem] border border-slate-200 bg-white p-6 shadow-sm sm:p-8"
  return <section className="mx-auto max-w-5xl space-y-6">
    {params.get("origen") === "compra-anterior" && <p role="status" className="rounded-2xl border border-slate-200 bg-white p-4 text-sm text-slate-700">Encontramos la referencia de un pedido anterior en este navegador. Este es su estado actual en la tienda.</p>}
    {tenant.storefront?.modoCatalogo && <StorefrontHistoricalPayment key={data.pedidoId} order={data} trackingToken={trackingToken} host={tenant.host} />}
    <section className={panel}><p className="text-xs uppercase text-slate-400">Tu compra</p><h1 className="mt-2 text-2xl font-bold">Productos del pedido #{shortId}</h1>
      {data.items.map(item => {
        const content = <><div className="h-24 w-24 shrink-0 rounded-2xl border bg-slate-50">{item.imagenPrincipal ? <img src={resolveStorefrontAssetUrl(item.imagenPrincipal) || item.imagenPrincipal} alt={item.nombreProducto} className="h-full w-full object-contain p-2" /> : <span>Sin imagen</span>}</div><div><h2 className="text-lg font-bold">{item.nombreProducto}</h2><p>Cantidad: {item.cantidad}</p>{item.productoPublicacionId && <p className="mt-3 font-bold" style={{ color: accent }}>Ver publicación →</p>}</div></>
        return item.productoPublicacionId ? <Link key={item.pedidoItemId} to={`/producto/${item.productoPublicacionId}`} className="mt-4 flex gap-4 border-t pt-5">{content}</Link> : <article key={item.pedidoItemId} className="mt-4 flex gap-4 border-t pt-5">{content}</article>
      })}
    </section>
    <section className={panel}><p className="text-xs uppercase text-slate-400">Estado del envío</p><h2 className="mt-2 text-2xl font-bold">Seguimiento de tu pedido</h2>
      {data.envio.trackingLogistica && <p>Código {data.envio.trackingLogistica}</p>}{data.envio.transportistaNombre && <p>Transportista: <strong>{data.envio.transportistaNombre}</strong></p>}
      {params.get("confirmarEntrega") === "1" && <div className="mt-6 rounded-2xl border bg-blue-50 p-5"><h3 className="font-bold">{delivered ? "Entrega confirmada" : "¿Recibiste correctamente tu compra?"}</h3><p>{delivered ? "Gracias por confirmar. El pedido quedó cerrado como entregado." : "Revisá el paquete y el producto antes de confirmar la recepción."}</p>{!delivered && <button type="button" disabled={state !== "DESPACHADO" || confirm.isPending} className="mt-4 rounded-full px-5 py-3 font-bold text-white disabled:opacity-50" style={{ backgroundColor: accent }} onClick={() => { if (window.confirm("¿Confirmás que recibiste el pedido correctamente?")) confirm.mutate() }}>{confirm.isPending ? "Confirmando..." : state === "DESPACHADO" ? "Confirmar entrega" : "Aún no está disponible"}</button>}{confirm.isError && <p role="alert">No pudimos confirmar la entrega. Intentá nuevamente.</p>}</div>}
      <ol className="mt-7 space-y-6">{steps.map(step => <li key={step.title} className={step.state === "upcoming" ? "text-slate-400" : "text-slate-900"} aria-current={step.state === "current" ? "step" : undefined}><h3 className="font-bold">{step.state === "completed" ? "✓ " : ""}{step.title}</h3><p className="text-sm">{step.detail}</p></li>)}</ol>
    </section>
    <section className={panel}><p className="text-xs uppercase text-slate-400">Ayuda directa</p><h2 className="mt-2 text-2xl font-bold">Postventa y garantía</h2><p className="mt-3">Si tenés un inconveniente o necesitás gestionar la garantía, comunicate directamente con {store}. El mensaje incluirá la referencia de esta compra.</p><div className="mt-5 flex flex-wrap gap-3">
      {phone && <a className="rounded-full bg-emerald-600 px-5 py-3 font-bold text-white" href={`https://wa.me/${phone}?text=${encodeURIComponent(message)}`} target="_blank" rel="noreferrer">Contactar por WhatsApp</a>}
      {tenant.branding?.emailComercial && <a className="rounded-full border px-5 py-3" href={`mailto:${tenant.branding.emailComercial}?subject=${encodeURIComponent(`Consulta pedido #${shortId}`)}`}>Enviar un correo</a>}
      <Link className="rounded-full border px-5 py-3" to="/catalogo">Volver al catálogo</Link>
    </div></section>
  </section>
}
