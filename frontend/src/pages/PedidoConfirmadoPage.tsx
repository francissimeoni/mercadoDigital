import { Link, useParams } from "react-router-dom"

export default function PedidoConfirmadoPage() {
  const { pedidoId } = useParams()

  return (
    <section className="rounded-lg border border-emerald-200 bg-emerald-50 p-6 shadow-sm">
      <h2 className="text-xl font-semibold text-emerald-900">Pedido confirmado</h2>
      <p className="mt-2 text-sm text-emerald-900">
        El pedido se creo correctamente en backend.
      </p>
      <p className="mt-2 break-all text-sm text-slate-700">
        ID de pedido: <span className="font-medium">{pedidoId || "N/A"}</span>
      </p>

      <div className="mt-4 flex gap-3">
        <Link
          to={pedidoId ? `/pedidos?creado=${pedidoId}` : "/pedidos"}
          className="rounded-md bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-700"
        >
          Ir a Pedidos
        </Link>
        <Link
          to="/catalogo"
          className="rounded-md bg-white px-4 py-2 text-sm font-medium text-slate-800 ring-1 ring-slate-300 hover:bg-slate-100"
        >
          Volver al catalogo
        </Link>
      </div>
    </section>
  )
}
