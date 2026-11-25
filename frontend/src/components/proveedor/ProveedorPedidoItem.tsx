import type { PedidoProveedorResponse } from "../../types/proveedor"

function formatFecha(value: string) {
  return new Date(value).toLocaleString("es-AR")
}

function formatAntiguedad(value: string) {
  const diffMs = Date.now() - new Date(value).getTime()
  const diffHours = Math.max(0, Math.floor(diffMs / (1000 * 60 * 60)))

  if (diffHours < 24) {
    return `${diffHours} h`
  }

  const diffDays = Math.floor(diffHours / 24)
  return `${diffDays} d`
}

type ProveedorPedidoItemProps = {
  item: PedidoProveedorResponse
  onMarcarListo: (pedidoId: string) => void
  isMutating: boolean
  actionLabel?: string
  actionDisabled?: boolean
  isExpanded?: boolean
  onToggleExpanded?: () => void
  variant?: "default" | "compact"
}

export default function ProveedorPedidoItem({
  item,
  onMarcarListo,
  isMutating,
  actionLabel = "Marcar listo para retiro",
  actionDisabled = false,
  isExpanded = false,
  onToggleExpanded,
  variant = "default",
}: ProveedorPedidoItemProps) {
  const puedeMarcarListo = normalizeEstado(item.estadoEnvio) === "PENDIENTE_PREPARACION"
  const actionBlocked = isMutating || actionDisabled || !puedeMarcarListo

  if (variant === "compact") {
    return (
      <article className="group flex items-center gap-4 rounded-xl border border-slate-100 bg-slate-50/50 p-3 transition-colors hover:border-slate-200 hover:bg-slate-50">
        <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-white text-xs font-bold text-slate-500 ring-1 ring-slate-200 shadow-sm">
          {item.cantidad}
        </div>
        <div className="min-w-0 flex-1">
          <p className="truncate text-sm font-bold text-slate-950 leading-tight">
            {item.nombreProducto}
          </p>
          <div className="mt-1 flex items-center gap-2 text-[10px] font-bold uppercase tracking-wider text-slate-400">
            <span>#{item.pedidoId.slice(0, 8)}</span>
            <span className="text-slate-300">•</span>
            <span className="text-blue-500">{formatAntiguedad(item.fechaCreacionPedido)}</span>
          </div>
        </div>
        <div className="shrink-0">
          <span className="inline-flex rounded-lg bg-white px-2 py-1 text-[10px] font-bold uppercase tracking-tight text-slate-500 ring-1 ring-inset ring-slate-200">
            {formatEstado(item.estadoEnvio)}
          </span>
        </div>
      </article>
    )
  }

  return (
    <article className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm transition hover:border-slate-300">
      <div className="flex flex-col gap-4 xl:flex-row xl:items-start xl:justify-between">
        <div className="min-w-0 flex-1">
          <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
            <div className="min-w-0">
              <p className="text-[10px] font-bold uppercase tracking-widest text-slate-400">Referencia #{item.pedidoId.slice(0,8)}</p>
              <p className="mt-2 text-lg font-bold text-slate-950 leading-tight">{item.nombreProducto}</p>
              <p className="mt-1.5 text-sm font-medium text-slate-500">
                {item.direccionEnvio ? item.direccionEnvio : "Dirección de retiro por definir"}
              </p>
            </div>

            <div className="flex shrink-0 flex-wrap gap-2">
              <span className="inline-flex rounded-lg bg-slate-100 px-2.5 py-1 text-[10px] font-bold uppercase tracking-wider text-slate-600">
                {formatEstado(item.estadoEnvio)}
              </span>
              <span className="inline-flex rounded-lg bg-blue-50 px-2.5 py-1 text-[10px] font-bold uppercase tracking-wider text-blue-600">
                T. de espera: {formatAntiguedad(item.fechaCreacionPedido)}
              </span>
            </div>
          </div>

          <div className="mt-4 grid grid-cols-1 gap-3 text-sm text-slate-700 sm:grid-cols-3">
            <div className="rounded-xl bg-slate-50 px-3 py-3">
              <p className="text-[11px] font-semibold uppercase tracking-[0.18em] text-slate-500">Cantidad</p>
              <p className="mt-1 font-semibold text-slate-900">{item.cantidad}</p>
            </div>
            <div className="rounded-xl bg-slate-50 px-3 py-3">
              <p className="text-[11px] font-semibold uppercase tracking-[0.18em] text-slate-500">Fecha alta</p>
              <p className="mt-1 font-medium text-slate-900">{formatFecha(item.fechaCreacionPedido)}</p>
            </div>
            <div className="rounded-xl bg-slate-50 px-3 py-3">
              <p className="text-[11px] font-semibold uppercase tracking-[0.18em] text-slate-500">Entrega</p>
              <p className="mt-1 font-medium text-slate-900">{item.direccionEnvio ? "Con direccion" : "Sin direccion"}</p>
            </div>
          </div>

          {isExpanded ? (
            <div className="mt-4 grid gap-3 rounded-2xl border border-slate-200 bg-slate-50 p-4 text-sm text-slate-700 sm:grid-cols-2">
              <div>
                <p className="text-[11px] font-semibold uppercase tracking-[0.18em] text-slate-500">Item</p>
                <p className="mt-1 break-all font-medium text-slate-900">{item.itemId}</p>
              </div>
              <div>
                <p className="text-[11px] font-semibold uppercase tracking-[0.18em] text-slate-500">Producto id</p>
                <p className="mt-1 break-all font-medium text-slate-900">{item.productoId}</p>
              </div>
              <div className="sm:col-span-2">
                <p className="text-[11px] font-semibold uppercase tracking-[0.18em] text-slate-500">Direccion / retiro</p>
                <p className="mt-1 font-medium text-slate-900">
                  {item.direccionEnvio || "No hay direccion informada en la respuesta actual del backend."}
                </p>
              </div>
            </div>
          ) : null}
        </div>

        <div className="flex shrink-0 flex-row gap-2 xl:flex-col xl:items-end xl:pl-4">
          {onToggleExpanded ? (
            <button
              type="button"
              className="rounded-xl border border-slate-300 px-3 py-2 text-sm font-semibold text-slate-700 transition hover:border-slate-400 hover:text-slate-900"
              onClick={onToggleExpanded}
            >
              {isExpanded ? "Ocultar detalle" : "Ver detalle"}
            </button>
          ) : null}
          <button
            type="button"
            className="rounded-xl bg-emerald-600 px-4 py-2.5 text-sm font-semibold text-white shadow-sm transition hover:bg-emerald-700 disabled:cursor-not-allowed disabled:opacity-70"
            onClick={() => {
              onMarcarListo(item.pedidoId)
            }}
            disabled={actionBlocked}
          >
            {isMutating ? "Procesando..." : puedeMarcarListo ? actionLabel : "Ya procesado"}
          </button>
        </div>
      </div>
    </article>
  )
}

function formatEstado(value: string | null) {
  const normalized = normalizeEstado(value)

  if (!normalized) {
    return "Sin estado"
  }

  return normalized.replaceAll("_", " ")
}

function normalizeEstado(value: string | null) {
  return value?.trim().toUpperCase() || null
}
