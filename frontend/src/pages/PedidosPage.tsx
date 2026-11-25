import { Link, useSearchParams } from "react-router-dom"
import FeedbackAlert from "../components/ui/FeedbackAlert"
import { usePedidos } from "../hooks/usePedidos"

const currencyFormatter = new Intl.NumberFormat("es-AR", {
  style: "currency",
  currency: "ARS",
  maximumFractionDigits: 2,
})

function formatFecha(value: string) {
  return new Date(value).toLocaleString("es-AR")
}

export default function PedidosPage() {
  const [searchParams] = useSearchParams()
  const pedidoCreadoId = searchParams.get("creado")
  const revendedorId =
    searchParams.get("revendedorId") || localStorage.getItem("revendedorId") || import.meta.env.VITE_REVENDEDOR_ID || null
  const { data, isLoading, isError, refetch } = usePedidos(revendedorId)

  return (
    <section className="space-y-4">
      {pedidoCreadoId ? (
        <FeedbackAlert variant="success" title="Pedido creado">
          Pedido generado correctamente. ID: <span className="font-medium">{pedidoCreadoId}</span>
        </FeedbackAlert>
      ) : null}

      <header className="rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
        <h2 className="text-xl font-semibold text-slate-900">Pedidos</h2>
        <p className="mt-2 text-sm text-slate-600">Listado real de pedidos del revendedor.</p>
      </header>

      {!revendedorId ? (
        <div className="rounded-lg border border-amber-300 bg-amber-50 p-6 text-sm text-amber-900 shadow-sm">
          Falta `revendedorId`. Configuralo en sesion local para poder consultar los pedidos del revendedor.
        </div>
      ) : null}

      {revendedorId && isLoading ? (
        <div className="space-y-3">
          {Array.from({ length: 4 }).map((_, index) => (
            <div key={index} className="h-24 animate-pulse rounded-lg border border-slate-200 bg-slate-200" />
          ))}
        </div>
      ) : null}

      {revendedorId && isError ? (
        <div className="rounded-lg border border-red-300 bg-red-50 p-6 shadow-sm">
          <p className="text-sm text-red-800">No se pudieron cargar los pedidos.</p>
          <button
            type="button"
            className="mt-4 rounded-md bg-red-700 px-4 py-2 text-sm font-medium text-white hover:bg-red-800"
            onClick={() => {
              void refetch()
            }}
          >
            Reintentar
          </button>
        </div>
      ) : null}

      {revendedorId && !isLoading && !isError ? (
        data && data.length > 0 ? (
          <div className="space-y-3">
            {data.map((pedido) => (
              <article
                key={pedido.pedidoId}
                className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm"
              >
                <div className="grid grid-cols-1 gap-2 md:grid-cols-5 md:items-center">
                  <div>
                    <p className="text-xs uppercase tracking-wide text-slate-500">Pedido</p>
                    <p className="break-all text-sm font-medium text-slate-900">{pedido.pedidoId}</p>
                  </div>
                  <div>
                    <p className="text-xs uppercase tracking-wide text-slate-500">Fecha</p>
                    <p className="text-sm text-slate-800">{formatFecha(pedido.fechaCreacionPedido)}</p>
                  </div>
                  <div>
                    <p className="text-xs uppercase tracking-wide text-slate-500">Estado</p>
                    <p className="text-sm text-slate-800">{pedido.estadoPedido}</p>
                  </div>
                  <div>
                    <p className="text-xs uppercase tracking-wide text-slate-500">Total</p>
                    <p className="text-sm font-semibold text-slate-900">
                      {currencyFormatter.format(pedido.totalFinal)}
                    </p>
                  </div>
                  <div className="md:text-right">
                    <Link
                      to={`/pedidos/${pedido.pedidoId}`}
                      className="inline-block rounded-md bg-slate-900 px-3 py-2 text-sm font-medium text-white hover:bg-slate-700"
                    >
                      Ver detalle
                    </Link>
                  </div>
                </div>
              </article>
            ))}
          </div>
        ) : (
          <div className="rounded-lg border border-slate-200 bg-white p-6 text-sm text-slate-600 shadow-sm">
            Todavia no hay pedidos para este revendedor. Puedes crear uno desde el catalogo.
          </div>
        )
      ) : null}
    </section>
  )
}
