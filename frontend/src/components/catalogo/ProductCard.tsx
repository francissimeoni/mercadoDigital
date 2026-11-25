import type { CatalogoItemResponse } from "../../types/catalogo"
import { Link } from "react-router-dom"

const currencyFormatter = new Intl.NumberFormat("es-AR", {
  style: "currency",
  currency: "ARS",
  maximumFractionDigits: 2,
})

type ProductCardProps = {
  item: CatalogoItemResponse
}

export default function ProductCard({ item }: ProductCardProps) {
  return (
    <article className="overflow-hidden rounded-lg border border-slate-200 bg-white shadow-sm">
      <div className="aspect-[4/3] w-full bg-slate-100">
        {item.imagenPrincipal ? (
          <img
            src={item.imagenPrincipal}
            alt={item.nombrePublico}
            className="h-full w-full object-cover"
            loading="lazy"
          />
        ) : (
          <div className="flex h-full items-center justify-center text-sm text-slate-500">
            Sin imagen
          </div>
        )}
      </div>

      <div className="space-y-2 p-4">
        <h3 className="min-h-12 text-base font-semibold text-slate-900">{item.nombrePublico}</h3>

        <p className="text-lg font-bold text-blue-700">
          {currencyFormatter.format(item.mejorCostoOperativoEstimado)}
        </p>
        <p className="text-xs font-medium uppercase tracking-wide text-slate-500">Costo operativo estimado</p>

        <div className="space-y-1 text-sm text-slate-600">
          <p>Demora estimada: {item.demoraEstimadaDias} dias</p>
          <p>Stock disponible: {item.stockDisponible}</p>
        </div>

        <Link
          to={`/catalogo/${item.productoCanonicoId}`}
          className="inline-block rounded-md bg-slate-900 px-3 py-2 text-sm font-medium text-white hover:bg-slate-700"
        >
          Ver detalle
        </Link>
      </div>
    </article>
  )
}
