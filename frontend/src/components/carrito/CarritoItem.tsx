import type { CarritoItemResponse } from "../../types/carrito"

const currencyFormatter = new Intl.NumberFormat("es-AR", {
  style: "currency",
  currency: "ARS",
  maximumFractionDigits: 2,
})

type CarritoItemProps = {
  item: CarritoItemResponse
}

export default function CarritoItem({ item }: CarritoItemProps) {
  return (
    <article className="grid grid-cols-1 gap-4 rounded-lg border border-slate-200 bg-white p-4 shadow-sm sm:grid-cols-[96px_1fr]">
      <div className="h-24 w-24 overflow-hidden rounded-md border border-slate-200 bg-slate-100">
        {item.imagenPrincipal ? (
          <img src={item.imagenPrincipal} alt={item.nombre} className="h-full w-full object-cover" />
        ) : (
          <div className="flex h-full items-center justify-center text-xs text-slate-500">Sin imagen</div>
        )}
      </div>

      <div className="space-y-1">
        <h3 className="text-base font-semibold text-slate-900">{item.nombre}</h3>
        <p className="text-sm text-slate-600">Precio: {currencyFormatter.format(item.precio)}</p>
        <p className="text-sm text-slate-600">Cantidad: {item.cantidad}</p>
        <p className="text-sm font-semibold text-slate-900">
          Subtotal: {currencyFormatter.format(item.subtotal)}
        </p>
      </div>
    </article>
  )
}
