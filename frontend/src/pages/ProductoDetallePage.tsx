import { useEffect, useState } from "react"
import { Link, useNavigate, useParams } from "react-router-dom"
import ProductoDetalleSkeleton from "../components/catalogo/ProductoDetalleSkeleton"
import FeedbackAlert from "../components/ui/FeedbackAlert"
import { useAuth } from "../auth/useAuth"
import { useCrearPedido } from "../hooks/useCrearPedido"
import { useProductoDetalle } from "../hooks/useProductoDetalle"

const currencyFormatter = new Intl.NumberFormat("es-AR", {
  style: "currency",
  currency: "ARS",
  maximumFractionDigits: 2,
})

export default function ProductoDetallePage() {
  const { productoId } = useParams()
  const navigate = useNavigate()
  const { session } = useAuth()
  const [feedback, setFeedback] = useState<{ variant: "success" | "error" | "warning"; message: string } | null>(null)
  const [cantidad, setCantidad] = useState(1)
  const { data, isLoading, isError, refetch } = useProductoDetalle(productoId)
  const crearPedido = useCrearPedido()
  const revendedorId = session?.revendedorId || localStorage.getItem("revendedorId") || import.meta.env.VITE_REVENDEDOR_ID || null
  const clienteId = session?.clienteId || localStorage.getItem("clienteId") || import.meta.env.VITE_CLIENTE_ID || null

  useEffect(() => {
    setFeedback(null)
    setCantidad(1)
  }, [productoId])

  if (!productoId) {
    return (
      <section className="space-y-4">
        <header className="rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
          <h2 className="text-xl font-semibold text-slate-900">Detalle de producto</h2>
        </header>
        <FeedbackAlert variant="warning" title="Producto no disponible">
          No se encontro un identificador de producto valido.
        </FeedbackAlert>
      </section>
    )
  }

  if (!revendedorId) {
    return (
      <section className="space-y-4">
        <header className="rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
          <h2 className="text-xl font-semibold text-slate-900">Detalle de producto</h2>
        </header>
        <FeedbackAlert variant="warning" title="Falta configuracion del revendedor">
          Para ver el detalle del producto, primero debe existir `revendedorId` en sesion local.
        </FeedbackAlert>
      </section>
    )
  }

  if (!clienteId) {
    return (
      <section className="space-y-4">
        <header className="rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
          <h2 className="text-xl font-semibold text-slate-900">Detalle de producto</h2>
        </header>
        <FeedbackAlert variant="warning" title="Falta configuracion del cliente">
          Para comprar en esta etapa debe existir `clienteId` en la sesion local del usuario revendedor.
        </FeedbackAlert>
      </section>
    )
  }

  if (isLoading) {
    return <ProductoDetalleSkeleton />
  }

  if (isError || !data) {
    return (
      <section className="rounded-lg border border-red-300 bg-red-50 p-6 shadow-sm">
        <h2 className="text-xl font-semibold text-slate-900">Detalle de producto</h2>
        <p className="mt-2 text-sm text-red-800">No se pudo cargar el detalle del producto. Reintenta desde el catalogo.</p>
        <div className="mt-4 flex gap-3">
          <button
            type="button"
            className="rounded-md bg-red-700 px-4 py-2 text-sm font-medium text-white hover:bg-red-800"
            onClick={() => {
              void refetch()
            }}
          >
            Reintentar
          </button>
          <Link
            to="/catalogo"
            className="rounded-md bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-700"
          >
            Volver al catalogo
          </Link>
        </div>
      </section>
    )
  }

  return (
    <section className="rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
      <div className="mb-4">
        <Link to="/catalogo" className="text-sm font-medium text-blue-700 hover:text-blue-900">
          {"<-"} Volver al catalogo
        </Link>
      </div>

      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <div className="overflow-hidden rounded-lg border border-slate-200 bg-slate-100">
          {data.imagenPrincipal ? (
            <img
              src={data.imagenPrincipal}
              alt={data.nombrePublico}
              className="h-full w-full object-cover"
            />
          ) : (
            <div className="flex aspect-[4/3] items-center justify-center text-sm text-slate-500">
              Todavia no hay una imagen cargada para este producto.
            </div>
          )}
        </div>

        <div className="space-y-4">
          {feedback ? <FeedbackAlert variant={feedback.variant}>{feedback.message}</FeedbackAlert> : null}
          <h1 className="text-2xl font-semibold text-slate-900">{data.nombrePublico}</h1>
          <p className="text-sm text-slate-700">{data.descripcion || "Sin descripcion disponible."}</p>
          <p className="text-2xl font-bold text-blue-700">
            {currencyFormatter.format(data.mejorCostoOperativoEstimado)}
          </p>
          <p className="text-xs font-medium uppercase tracking-wide text-slate-500">Costo operativo estimado</p>
          <p className="text-sm text-slate-600">Demora estimada: {data.demoraEstimadaDias} dias</p>
          <p className="text-sm text-slate-600">Stock disponible: {data.stockDisponible}</p>

          <div className="rounded-lg border border-slate-200 bg-slate-50 p-4">
            <p className="text-sm font-medium text-slate-700">Cantidad</p>
            <div className="mt-3 flex items-center gap-3">
              <button
                type="button"
                className="h-10 w-10 rounded-full border border-slate-300 text-lg font-semibold text-slate-700"
                onClick={() => setCantidad((current) => Math.max(1, current - 1))}
              >
                -
              </button>
              <input
                type="number"
                min={1}
                max={data.stockDisponible}
                className="w-24 rounded-lg border border-slate-300 px-4 py-2 text-center text-sm outline-none transition focus:border-slate-500"
                value={cantidad}
                onChange={(event) => {
                  const next = Number(event.target.value) || 1
                  setCantidad(Math.max(1, Math.min(data.stockDisponible, next)))
                }}
              />
              <button
                type="button"
                className="h-10 w-10 rounded-full border border-slate-300 text-lg font-semibold text-slate-700"
                onClick={() => setCantidad((current) => Math.min(data.stockDisponible, current + 1))}
              >
                +
              </button>
            </div>
          </div>

          <button
            type="button"
            className="rounded-md bg-emerald-600 px-4 py-2 text-sm font-medium text-white hover:bg-emerald-700 disabled:cursor-not-allowed disabled:opacity-70"
            disabled={crearPedido.isPending || data.stockDisponible < 1}
            onClick={() => {
              setFeedback(null)
              if (cantidad > data.stockDisponible) {
                setFeedback({ variant: "warning", message: "La cantidad elegida supera el stock disponible." })
                return
              }

              crearPedido.mutate(
                {
                  clienteId,
                  revendedorId,
                  items: [
                    {
                      productoCanonicoId: productoId,
                      cantidad,
                      tieneSeguro: false,
                    },
                  ],
                },
                {
                  onSuccess: (pedido) => {
                    navigate(`/pedidos/confirmado/${pedido.id}`, { replace: true })
                  },
                  onError: () => {
                    setFeedback({ variant: "error", message: "No se pudo crear el pedido directo. Reintenta en unos segundos." })
                  },
                },
              )
            }}
          >
            {crearPedido.isPending ? "Creando pedido..." : "Comprar este producto"}
          </button>
          <p className="text-sm text-slate-500">
            Esta compra interna del revendedor usa costo operativo y va directo a pedido para un solo producto, sin carrito intermedio.
          </p>
        </div>
      </div>
    </section>
  )
}
