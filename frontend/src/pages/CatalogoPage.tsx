import { useMemo } from "react"
import { useSearchParams } from "react-router-dom"
import ProductCard from "../components/catalogo/ProductCard"
import ProductCardSkeleton from "../components/catalogo/ProductCardSkeleton"
import FeedbackAlert from "../components/ui/FeedbackAlert"
import { useCatalogo } from "../hooks/useCatalogo"

function getRevendedorId(searchParams: URLSearchParams): string | null {
  const fromQuery = searchParams.get("revendedorId")
  const fromStorage = localStorage.getItem("revendedorId")
  const fromEnv = import.meta.env.VITE_REVENDEDOR_ID

  return fromQuery || fromStorage || fromEnv || null
}

export default function CatalogoPage() {
  const [searchParams] = useSearchParams()
  const revendedorId = useMemo(() => getRevendedorId(searchParams), [searchParams])
  const { data, isLoading, isError, refetch, isFetching } = useCatalogo(revendedorId)

  if (!revendedorId) {
    return (
      <section className="space-y-4">
        <header className="rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
          <h2 className="text-xl font-semibold text-slate-900">Catalogo</h2>
          <p className="mt-2 text-sm text-slate-600">Productos canonicos disponibles para compra.</p>
        </header>
        <FeedbackAlert variant="warning" title="Falta configuracion del revendedor">
          Configura `revendedorId` en la URL, en `localStorage` o en `VITE_REVENDEDOR_ID` para poder cargar el catalogo.
        </FeedbackAlert>
      </section>
    )
  }

  return (
    <section className="space-y-4">
      <header className="rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
        <h2 className="text-xl font-semibold text-slate-900">Catalogo</h2>
        <p className="mt-2 text-sm text-slate-600">Productos canonicos disponibles para compra.</p>
      </header>

      {isLoading ? (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {Array.from({ length: 6 }).map((_, index) => (
            <ProductCardSkeleton key={index} />
          ))}
        </div>
      ) : null}

      {isError ? (
        <div className="rounded-lg border border-red-300 bg-red-50 p-6 shadow-sm">
          <p className="text-sm text-red-800">No se pudo cargar el catalogo. Revisa la conexion con backend y reintenta.</p>
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
        data && data.length > 0 ? (
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {data.map((item) => (
              <ProductCard key={item.productoCanonicoId} item={item} />
            ))}
          </div>
        ) : (
          <div className="rounded-lg border border-slate-200 bg-white p-6 text-sm text-slate-600 shadow-sm">
            No hay productos disponibles para este revendedor en este momento.
          </div>
        )
      ) : null}

      {isFetching && !isLoading ? (
        <FeedbackAlert variant="info">Actualizando catalogo...</FeedbackAlert>
      ) : null}
    </section>
  )
}
