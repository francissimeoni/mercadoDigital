import { Link, useNavigate, useSearchParams } from "react-router-dom"
import CarritoItem from "../components/carrito/CarritoItem"
import FeedbackAlert from "../components/ui/FeedbackAlert"
import { useCarrito } from "../hooks/useCarrito"
import { useCrearPedido } from "../hooks/useCrearPedido"

const currencyFormatter = new Intl.NumberFormat("es-AR", {
  style: "currency",
  currency: "ARS",
  maximumFractionDigits: 2,
})

function CarritoSkeleton() {
  return (
    <div className="space-y-3">
      {Array.from({ length: 3 }).map((_, index) => (
        <div key={index} className="h-28 animate-pulse rounded-lg border border-slate-200 bg-slate-200" />
      ))}
    </div>
  )
}

export default function CarritoPage() {
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const { data, isLoading, isError, refetch } = useCarrito()
  const crearPedido = useCrearPedido()
  const clienteId = searchParams.get("clienteId") || localStorage.getItem("clienteId") || import.meta.env.VITE_CLIENTE_ID || null
  const revendedorId =
    searchParams.get("revendedorId") || localStorage.getItem("revendedorId") || import.meta.env.VITE_REVENDEDOR_ID || null

  const puedeConfirmar = Boolean(clienteId && revendedorId && data && data.items.length > 0 && !crearPedido.isPending)

  const confirmarPedido = () => {
    if (!clienteId || !revendedorId || !data || data.items.length === 0) {
      return
    }

    crearPedido.mutate(
      {
        clienteId,
        revendedorId,
        items: data.items.map((item) => ({
          productoCanonicoId: item.productoId,
          cantidad: item.cantidad,
          tieneSeguro: false,
        })),
      },
      {
        onSuccess: (pedido) => {
          navigate(`/pedidos/confirmado/${pedido.id}`, { replace: true })
        },
      },
    )
  }

  return (
    <section className="space-y-4">
      <header className="rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
        <h2 className="text-xl font-semibold text-slate-900">Carrito</h2>
        <p className="mt-2 text-sm text-slate-600">Resumen de productos agregados.</p>
      </header>

      {isLoading ? <CarritoSkeleton /> : null}

      {isError ? (
        <div className="rounded-lg border border-red-300 bg-red-50 p-6 shadow-sm">
          <p className="text-sm text-red-800">No se pudo cargar el carrito. Reintenta para recuperar los productos agregados.</p>
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

      {!isLoading && !isError ? (
        data && data.items.length > 0 ? (
          <div className="space-y-3">
            {data.items.map((item) => (
              <CarritoItem key={item.productoId} item={item} />
            ))}
            {typeof data.total === "number" ? (
              <div className="rounded-lg border border-slate-200 bg-white p-4 text-right shadow-sm">
                <p className="text-sm text-slate-600">Total</p>
                <p className="text-xl font-bold text-slate-900">{currencyFormatter.format(data.total)}</p>
              </div>
            ) : null}

            <div className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
              {!clienteId || !revendedorId ? (
                <FeedbackAlert variant="warning">
                  Falta clienteId o revendedorId para confirmar el pedido. Completa esa configuracion y vuelve a intentarlo.
                </FeedbackAlert>
              ) : null}

              {crearPedido.isError ? (
                <div className="mb-3">
                  <FeedbackAlert variant="error">No se pudo confirmar el pedido. Revisa los datos y reintenta.</FeedbackAlert>
                </div>
              ) : null}

              <button
                type="button"
                className="rounded-md bg-emerald-600 px-4 py-2 text-sm font-medium text-white hover:bg-emerald-700 disabled:cursor-not-allowed disabled:opacity-70"
                onClick={confirmarPedido}
                disabled={!puedeConfirmar}
              >
                {crearPedido.isPending ? "Confirmando..." : "Confirmar pedido"}
              </button>
            </div>
          </div>
        ) : (
          <div className="rounded-lg border border-slate-200 bg-white p-6 text-sm text-slate-600 shadow-sm">
            Tu carrito esta vacio. Agrega al menos un producto para poder confirmar un pedido.{" "}
            <Link className="font-medium text-blue-700 hover:text-blue-900" to="/catalogo">
              Ir al catalogo
            </Link>
            .
          </div>
        )
      ) : null}
    </section>
  )
}
