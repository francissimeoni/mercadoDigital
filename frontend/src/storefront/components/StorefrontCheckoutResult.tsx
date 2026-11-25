import { MERCADO_PAGO_DISPONIBLE } from "../../config/mercadoPago"
import { Link } from "react-router-dom"
import { useTenant } from "../../tenant/useTenant"
import type { StorefrontPedidoDirectoResponse } from "../types/storefrontPedido"
import { normalizeWhatsAppPhone } from "../utils/storefrontContact"
import { resolveStorefrontAssetUrl } from "../utils/storefrontBranding"

// Production bR, confirmation branches: an order created is still unpaid.
export default function StorefrontCheckoutResult({ order, email, productName, image, quantity, checkoutUrl, error }: {
  order: StorefrontPedidoDirectoResponse; email: string; productName: string; image: string | null; quantity: number; checkoutUrl: string | null; error: string | null
}) {
  const { branding } = useTenant()
  const accent = branding?.colorPrimario || "#0f172a"
  const store = branding?.nombreComercial?.trim() || "la tienda"
  const shortId = order.pedidoId.slice(0, 8).toUpperCase()
  const logo = resolveStorefrontAssetUrl(branding?.logoUrl)
  const phone = branding?.telefonoComercial ? normalizeWhatsAppPhone(branding.telefonoComercial) : null
  const productImage = resolveStorefrontAssetUrl(image)
  const message = [`Hola ${store}, registré el pedido #${shortId}.`, `Producto: ${productName}`, `Cantidad: ${quantity}`, "Estado: pendiente de pago por transferencia", order.trackingToken ? `Seguimiento: ${window.location.origin}/pedido/${order.trackingToken}` : null, `Abrir pedido en la tienda: ${window.location.origin}/mi-tienda?tab=pedidos&pedido=${order.pedidoId}`, productImage ? `Imagen del producto: ${productImage}` : null].filter(Boolean).join("\n")
  if (order.metodoPago !== "TRANSFERENCIA" && !MERCADO_PAGO_DISPONIBLE) return <section className="mx-auto max-w-2xl rounded-[2rem] border border-slate-200 bg-white p-8 text-center shadow-xl">
    <h1 className="text-2xl font-bold">Tu pedido sigue registrado</h1>
    <p className="mt-4">MercadoDigital ya no procesa pagos con Mercado Pago. Contactá a {store} para revisar el estado del pedido #{shortId} y coordinar cómo continuar. Si ya pagaste, informalo antes de realizar otro pago.</p>
    {order.trackingToken && <Link to={`/pedido/${encodeURIComponent(order.trackingToken)}`} className="mt-5 inline-flex rounded-full border px-5 py-3 font-semibold">Consultar pedido registrado</Link>}
  </section>
  if (order.metodoPago !== "TRANSFERENCIA") return <section className="mx-auto max-w-2xl rounded-[2rem] border border-sky-200 bg-white p-8 text-center shadow-xl">
    {logo && <img src={logo} alt={store} className="mx-auto mb-6 max-h-14" />}<p className="font-bold text-sky-700">Pago seguro</p><h1 className="mt-2 text-3xl font-bold">{error ? "Pedido registrado: revisá el estado del pago" : "Te estamos llevando a Mercado Pago"}</h1>
    <p className="mt-4">Tu compra todavía no fue confirmada. Completá el pago en Mercado Pago y luego volverás automáticamente a {store}.</p><p className="mt-3">Referencia del pedido: #{shortId}</p>
    {error && <p role="alert" className="mt-6 text-red-700">No se pudo iniciar el pago: {error}</p>}
    {MERCADO_PAGO_DISPONIBLE && checkoutUrl && <a href={checkoutUrl} className="mt-6 inline-flex rounded-full px-6 py-3 font-bold text-white" style={{ backgroundColor: accent }}>Abrir Mercado Pago</a>}
    {order.trackingToken && <Link to={`/pedido/${encodeURIComponent(order.trackingToken)}`} className="mt-5 inline-flex rounded-full border px-5 py-3 font-semibold">Consultar pedido registrado</Link>}
    {MERCADO_PAGO_DISPONIBLE && checkoutUrl && <p className="mt-4 text-sm">Si la redirección no comienza, usá el botón para continuar.</p>}
  </section>
  return <section className="mx-auto max-w-4xl space-y-5">
    <div className="overflow-hidden rounded-3xl border bg-white shadow-xl"><header className="p-7 text-white" style={{ backgroundColor: accent }}>{logo && <img src={logo} alt={store} className="mb-5 max-h-14 rounded-xl bg-white p-2" />}<p>Pendiente de pago</p><h1 className="mt-3 text-3xl font-bold">Tu pedido está pendiente de pago</h1><p className="mt-3">Para confirmar la compra, realizá la transferencia y cargá el comprobante. {store} verificará el pago antes de comenzar a preparar el pedido.</p></header>
      <div className="space-y-4 p-6"><h2 className="text-2xl font-bold">Pedido registrado #{shortId}</h2><p>El pedido todavía no está confirmado. Guardá este número para cualquier consulta.</p><div className="flex flex-wrap gap-3">
        {order.transferenciaToken && <Link to={`/transferencia/${order.transferenciaToken}`} className="rounded-full px-5 py-3 font-bold text-white" style={{ backgroundColor: accent }}>Cargar comprobante</Link>}
        {order.trackingToken && <Link to={`/pedido/${order.trackingToken}`} className="rounded-full border px-5 py-3">Seguir mi pedido</Link>}
        {phone && <a href={`https://wa.me/${phone}?text=${encodeURIComponent(message)}`} target="_blank" rel="noreferrer" className="rounded-full bg-emerald-500 px-5 py-3 font-bold text-white">Avisar por WhatsApp</a>}
      </div></div>
    </div>
    <div className="rounded-3xl border border-amber-200 bg-amber-50 p-6"><h2 className="text-lg font-bold">Cómo completar el pago</h2><ol className="mt-4 list-inside list-decimal space-y-3"><li>Revisá en <strong>{email}</strong> los datos para realizar la transferencia.</li><li>Transferí el importe indicado utilizando esos datos.</li><li>Cargá una foto o un PDF del comprobante para que la tienda pueda revisarlo.</li></ol><p className="mt-4 font-semibold">La compra quedará confirmada cuando {store} valide el pago.</p></div>
    <div className="rounded-3xl border bg-white p-6"><h2 className="font-bold">¿Necesitás ayuda?</h2><p>Contactá directamente a {store}.{branding?.telefonoComercial ? ` WhatsApp: ${branding.telefonoComercial}.` : ""}{branding?.emailComercial ? ` Email: ${branding.emailComercial}.` : ""}</p>{branding?.direccionComercial && <p>{branding.direccionComercial}</p>}</div>
    {error && <p role="alert" className="text-red-700">No se pudo iniciar el pago: {error}</p>}
  </section>
}
