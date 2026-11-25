import type { PedidoLogisticaResponse } from "../../types/logistica"

function formatFecha(value: string) {
  return new Date(value).toLocaleString("es-AR")
}

type LogisticaRetiroItemProps = {
  item: PedidoLogisticaResponse
  onMarcarRetirado: (pedidoId: string) => void
  onVerDetalle: (pedidoId: string) => void
  isMutating: boolean
  isSelected?: boolean
}

export default function LogisticaRetiroItem({
  item,
  onMarcarRetirado,
  onVerDetalle,
  isMutating,
  isSelected = false,
}: LogisticaRetiroItemProps) {
  return (
    <article
      className={`cursor-pointer rounded-[1.75rem] border p-4 shadow-sm transition sm:p-5 ${
        isSelected
          ? "border-blue-300 bg-blue-50/70 shadow-md"
          : "border-slate-200 bg-white hover:border-blue-200 hover:shadow-md"
      }`}
      onClick={() => {
        onVerDetalle(item.pedidoId)
      }}
    >
      <div className="flex flex-col gap-4">
        <div className="flex flex-col gap-3 sm:flex-row sm:flex-wrap sm:items-start sm:justify-between">
          <div className="min-w-0 flex-1">
            <div className="flex flex-wrap items-center gap-2">
              <span className="rounded-full border border-blue-200 bg-blue-100 px-3 py-1 text-[11px] font-semibold uppercase tracking-[0.18em] text-blue-800">
                Retiro
              </span>
              {item.estadoEnvio ? (
                <span className="rounded-full border border-slate-200 bg-white px-3 py-1 text-[11px] font-semibold uppercase tracking-[0.16em] text-slate-600">
                  {item.estadoEnvio}
                </span>
              ) : null}
            </div>
            <p className="mt-3 break-all text-base font-semibold text-slate-900">{item.nombreProducto}</p>
            <p className="mt-1 text-sm text-slate-500">Pedido {item.pedidoId}</p>
          </div>
          <div className="grid gap-2 sm:flex sm:flex-wrap">
            <button
              type="button"
              className="min-h-[44px] rounded-xl border border-slate-300 bg-white px-3 py-3 text-sm font-medium text-slate-700 hover:bg-slate-50"
              onClick={(event) => {
                event.stopPropagation()
                onVerDetalle(item.pedidoId)
              }}
            >
              Ver detalle
            </button>
            <button
              type="button"
              className="min-h-[44px] rounded-xl bg-blue-700 px-3 py-3 text-sm font-medium text-white hover:bg-blue-800 disabled:cursor-not-allowed disabled:opacity-70"
              disabled={isMutating}
              onClick={(event) => {
                event.stopPropagation()
                onMarcarRetirado(item.pedidoId)
              }}
            >
              {isMutating ? "Procesando..." : "Marcar retirado"}
            </button>
          </div>
        </div>

        <div className="grid gap-3 sm:grid-cols-2">
          <InfoPill label="Proveedor" value={item.nombreProveedor} />
          <InfoPill label="Siguiente paso" value="Retirar producto del proveedor" highlight />
        </div>

        <div className="overflow-hidden rounded-[1.25rem] border border-slate-200 bg-slate-50">
          <div className="divide-y divide-slate-200">
            <DetailRow label="Cantidad" value={String(item.cantidad)} />
            <DetailRow label="Fecha" value={formatFecha(item.fechaCreacionPedido)} />
            <DetailRow label="Estado" value={item.estadoEnvio || "Sin estado"} />
          </div>
        </div>
      </div>
    </article>
  )
}

function InfoPill({ label, value, highlight = false }: { label: string; value: string; highlight?: boolean }) {
  return (
    <div className={`rounded-[1.25rem] border px-4 py-3 ${highlight ? "border-blue-200 bg-blue-50" : "border-slate-200 bg-slate-50"}`}>
      <p className="text-[11px] font-semibold uppercase tracking-[0.18em] text-slate-500">{label}</p>
      <p className={`mt-2 text-sm ${highlight ? "font-semibold text-blue-900" : "text-slate-800"}`}>{value}</p>
    </div>
  )
}

function DetailRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex flex-col gap-1 px-4 py-3 sm:flex-row sm:items-start sm:gap-4">
      <dt className="text-xs font-semibold uppercase tracking-[0.18em] text-slate-500 sm:w-28 sm:flex-shrink-0">
        {label}
      </dt>
      <dd className="text-sm text-slate-800">{value}</dd>
    </div>
  )
}
