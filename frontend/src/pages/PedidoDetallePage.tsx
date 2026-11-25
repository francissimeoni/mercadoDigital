import { Link, useParams } from "react-router-dom"
import { usePedidoDetalle } from "../hooks/usePedidos"

const currencyFormatter = new Intl.NumberFormat("es-AR", {
  style: "currency",
  currency: "ARS",
  maximumFractionDigits: 2,
})

function formatFecha(value: string) {
  return new Date(value).toLocaleString("es-AR")
}

export default function PedidoDetallePage() {
  const { pedidoId } = useParams()
  const { data, isLoading, isError, refetch } = usePedidoDetalle(pedidoId)

  if (!pedidoId) {
    return (
      <section className="rounded-lg border border-amber-300 bg-amber-50 p-6 shadow-sm">
        <h2 className="text-xl font-semibold text-slate-900">Detalle de pedido</h2>
        <p className="mt-2 text-sm text-amber-900">No se encontro un identificador de pedido valido.</p>
      </section>
    )
  }

  if (isLoading) {
    return (
      <section className="space-y-3">
        {Array.from({ length: 3 }).map((_, index) => (
          <div key={index} className="h-24 animate-pulse rounded-lg border border-slate-200 bg-slate-200" />
        ))}
      </section>
    )
  }

  if (isError || !data) {
    return (
      <section className="rounded-lg border border-red-300 bg-red-50 p-6 shadow-sm">
        <h2 className="text-xl font-semibold text-slate-900">Detalle de pedido</h2>
        <p className="mt-2 text-sm text-red-800">No se pudo cargar el detalle del pedido.</p>
        <button
          type="button"
          className="mt-4 rounded-md bg-red-700 px-4 py-2 text-sm font-medium text-white hover:bg-red-800"
          onClick={() => {
            void refetch()
          }}
        >
          Reintentar
        </button>
      </section>
    )
  }

  return (
    <section className="space-y-4">
      <header className="rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
        <div className="mb-3">
          <Link className="text-sm font-medium text-blue-700 hover:text-blue-900" to="/pedidos">
            {"<-"} Volver a pedidos
          </Link>
        </div>
        <h2 className="break-all text-xl font-semibold text-slate-900">Pedido {data.id}</h2>
        <p className="mt-2 text-sm text-slate-600">Creado: {formatFecha(data.createdAt)}</p>
      </header>

      <div className="grid grid-cols-1 gap-3 md:grid-cols-3">
        <div className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
          <p className="text-xs uppercase tracking-wide text-slate-500">Estado pedido</p>
          <p className="mt-1 text-sm font-medium text-slate-900">{data.estadoPedido}</p>
        </div>
        <div className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
          <p className="text-xs uppercase tracking-wide text-slate-500">Estado pago</p>
          <p className="mt-1 text-sm font-medium text-slate-900">{data.estadoPago}</p>
        </div>
        <div className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
          <p className="text-xs uppercase tracking-wide text-slate-500">Estado envio</p>
          <p className="mt-1 text-sm font-medium text-slate-900">{data.estadoEnvio || "N/A"}</p>
        </div>
      </div>

      <section className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
        <h3 className="mb-3 text-base font-semibold text-slate-900">Items</h3>
        <div className="space-y-3">
          {data.items.map((item) => (
            <article key={item.id} className="rounded-lg border border-slate-200 bg-slate-50 p-4">
              <p className="text-sm font-semibold text-slate-900">{item.productoNombre}</p>
              <div className="mt-2 grid grid-cols-1 gap-2 sm:grid-cols-3">
                <p className="text-sm text-slate-700">Cantidad: {item.cantidad}</p>
                <p className="text-sm text-slate-700">
                  Precio congelado: {currencyFormatter.format(item.precioItemFinal)}
                </p>
                <p className="text-sm text-slate-700">
                  Seguro: {item.tieneSeguro ? currencyFormatter.format(item.montoSeguro) : "No"}
                </p>
              </div>
            </article>
          ))}
        </div>
      </section>

      <section className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
        <h3 className="mb-3 text-base font-semibold text-slate-900">Totales</h3>
        <div className="grid grid-cols-1 gap-2 text-sm text-slate-700 sm:grid-cols-2">
          <p>Total bruto: {currencyFormatter.format(data.totalBruto)}</p>
          <p>Total PSP: {currencyFormatter.format(data.totalPsp)}</p>
          <p>Total envio: {currencyFormatter.format(data.totalEnvio)}</p>
          <p>Total seguro: {currencyFormatter.format(data.totalSeguro)}</p>
        </div>
        <p className="mt-3 text-lg font-bold text-slate-900">
          Total final: {currencyFormatter.format(data.totalFinal)}
        </p>
      </section>
    </section>
  )
}
