import { Link } from "react-router-dom"
import type { StorefrontPedidoTrackingResponse } from "../types/storefrontPedido"

/** Solo recibe el pedido consultado con token y host; nunca usa una URL guardada. */
export default function StorefrontHistoricalPayment({ order }: {
  order: StorefrontPedidoTrackingResponse; trackingToken: string; host: string
}) {
  if (order.estadoPedido !== "CREADO" || order.estadoPago !== "PENDIENTE") return null
  if (order.metodoPago !== "PSP" && !order.transferenciaToken) return null
  return <section className="rounded-3xl border border-amber-200 bg-amber-50 p-5 sm:p-6">
    <h2 className="text-lg font-bold text-amber-950">Pago de tu pedido anterior</h2>
    <p className="mt-2 text-sm leading-6 text-amber-950">{order.metodoPago === "PSP" ? "MercadoDigital ya no procesa pagos con Mercado Pago. Tu pedido sigue registrado: contactá a la tienda para revisar su estado y coordinar cómo continuar. Si ya pagaste, informalo antes de realizar otro pago." : "Este pedido ya está registrado. Podés cargar el comprobante de tu transferencia."}</p>
    {order.metodoPago === "TRANSFERENCIA" && order.transferenciaToken ? <Link className="mt-4 inline-flex rounded-full bg-slate-950 px-5 py-3 text-sm font-semibold text-white" to={`/transferencia/${encodeURIComponent(order.transferenciaToken)}`}>Cargar comprobante</Link> : null}
  </section>
}
