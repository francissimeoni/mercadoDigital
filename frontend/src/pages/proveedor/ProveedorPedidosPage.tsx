import { useState, type Dispatch, type SetStateAction } from "react"
import { useSearchParams } from "react-router-dom"
import ProveedorMetricCard from "../../components/proveedor/ProveedorMetricCard"
import ProveedorModuleHeader from "../../components/proveedor/ProveedorModuleHeader"
import FeedbackAlert from "../../components/ui/FeedbackAlert"
import ProveedorPedidoItem from "../../components/proveedor/ProveedorPedidoItem"
import { useProveedorContext } from "../../hooks/useProveedorContext"
import { useMarcarListoParaRetiro, usePedidosProveedor } from "../../hooks/useProveedorPedidos"
import type { PedidoProveedorResponse } from "../../types/proveedor"

type FiltroBandeja = "todos" | "pendientes" | "con-direccion" | "sin-direccion"
type OrdenBandeja = "antiguos" | "recientes"

export default function ProveedorPedidosPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const [feedback, setFeedback] = useState<{ variant: "success" | "error"; message: string } | null>(null)
  const [query, setQuery] = useState("")
  const [filtro, setFiltro] = useState<FiltroBandeja>(resolveInitialFiltro(searchParams.get("filtro")))
  const [orden, setOrden] = useState<OrdenBandeja>("antiguos")
  const [expandedItemId, setExpandedItemId] = useState<string | null>(null)
  const [pendingPedidoId, setPendingPedidoId] = useState<string | null>(null)
  const { proveedorId } = useProveedorContext()
  const { data, isLoading, isError, refetch } = usePedidosProveedor(proveedorId)
  const marcarListo = useMarcarListoParaRetiro(proveedorId)
  const pedidos = data ?? []
  const pedidosConDireccion = pedidos.filter((item) => Boolean(item.direccionEnvio)).length
  const pedidosFiltrados = resolvePedidosFiltrados(pedidos, query, filtro, orden)
  const pedidosPorProcesar = pedidosFiltrados.filter((item) => !isPedidoProcesado(item))
  const pedidosProcesados = pedidosFiltrados.filter((item) => isPedidoProcesado(item))

  function onChangeFiltro(nextFiltro: FiltroBandeja) {
    setFiltro(nextFiltro)
    setSearchParams((current) => {
      const next = new URLSearchParams(current)
      if (nextFiltro === "todos") {
        next.delete("filtro")
      } else {
        next.set("filtro", nextFiltro)
      }
      return next
    })
  }

  return (
    <section className="space-y-6">
      <ProveedorModuleHeader
        eyebrow="Proveedor"
        title="Pedidos pendientes"
        description="Seccion de pedidos pendientes."
        proveedorId={proveedorId}
        aside={
          <div className="rounded-full border border-slate-200 bg-slate-50 px-4 py-2 text-sm text-slate-600">
            {isLoading ? "Cargando bandeja..." : `${pedidos.length} items pendientes`}
          </div>
        }
      />

      {!proveedorId ? (
        <FeedbackAlert variant="warning" title="Proveedor no resuelto">
          Falta `proveedorId` para cargar los pedidos pendientes del proveedor.
        </FeedbackAlert>
      ) : null}

      {proveedorId && isLoading ? (
        <div className="space-y-3">
          {Array.from({ length: 4 }).map((_, index) => (
            <div key={index} className="h-24 animate-pulse rounded-2xl border border-slate-200 bg-slate-200" />
          ))}
        </div>
      ) : null}

      {proveedorId && isError ? (
        <FeedbackAlert variant="error" title="No se pudieron cargar los pedidos">
          <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
            <p>Reintenta para recuperar la bandeja operativa del proveedor.</p>
            <button
              type="button"
              className="rounded-full bg-red-700 px-4 py-2 text-sm font-semibold text-white hover:bg-red-800"
              onClick={() => {
                void refetch()
              }}
            >
              Reintentar
            </button>
          </div>
        </FeedbackAlert>
      ) : null}

      {feedback ? <FeedbackAlert variant={feedback.variant}>{feedback.message}</FeedbackAlert> : null}

      {proveedorId && !isLoading && !isError ? (
        pedidos.length > 0 ? (
          <>
            <div className="grid gap-4 md:grid-cols-3">
              <ProveedorMetricCard
                label="Por procesar"
                value={String(pedidosPorProcesar.length)}
                helper="Items visibles que todavia requieren accion operativa."
                tone="blue"
              />
              <ProveedorMetricCard
                label="Procesados"
                value={String(pedidosProcesados.length)}
                helper="Items visibles ya marcados como listos para retiro."
                tone="emerald"
              />
              <ProveedorMetricCard
                label="Con direccion"
                value={String(pedidosConDireccion)}
                helper="Items con direccion de envio disponible en backend."
                tone="amber"
              />
            </div>

            <section className="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm">
              <div className="grid gap-4 xl:grid-cols-[minmax(0,1.4fr)_auto_auto] xl:items-end">
                <label className="block">
                  <span className="text-xs font-semibold uppercase tracking-[0.22em] text-slate-500">Busqueda</span>
                  <input
                    type="search"
                    value={query}
                    onChange={(event) => setQuery(event.target.value)}
                    placeholder="Buscar por pedido, producto o direccion visible"
                    className="mt-2 w-full rounded-2xl border border-slate-300 bg-white px-4 py-3 text-sm text-slate-900 outline-none transition placeholder:text-slate-400 focus:border-slate-400 focus:ring-2 focus:ring-slate-200"
                  />
                </label>

                <label className="block">
                  <span className="text-xs font-semibold uppercase tracking-[0.22em] text-slate-500">Filtro</span>
                  <select
                    value={filtro}
                    onChange={(event) => onChangeFiltro(event.target.value as FiltroBandeja)}
                    className="mt-2 w-full rounded-2xl border border-slate-300 bg-white px-4 py-3 text-sm text-slate-900 outline-none transition focus:border-slate-400 focus:ring-2 focus:ring-slate-200"
                  >
                    <option value="todos">Todos</option>
                    <option value="pendientes">Pendientes</option>
                    <option value="con-direccion">Con direccion</option>
                    <option value="sin-direccion">Sin direccion</option>
                  </select>
                </label>

                <label className="block">
                  <span className="text-xs font-semibold uppercase tracking-[0.22em] text-slate-500">Orden</span>
                  <select
                    value={orden}
                    onChange={(event) => setOrden(event.target.value as OrdenBandeja)}
                    className="mt-2 w-full rounded-2xl border border-slate-300 bg-white px-4 py-3 text-sm text-slate-900 outline-none transition focus:border-slate-400 focus:ring-2 focus:ring-slate-200"
                  >
                    <option value="antiguos">Mas antiguos primero</option>
                    <option value="recientes">Mas recientes primero</option>
                  </select>
                </label>
              </div>

              <div className="mt-4 flex flex-wrap gap-2">
                <span className="rounded-full bg-slate-100 px-3 py-1 text-xs font-semibold text-slate-700">
                  {pedidosFiltrados.length} visibles
                </span>
                <span className="rounded-full bg-blue-50 px-3 py-1 text-xs font-semibold text-blue-700">
                  {pedidosPorProcesar.length} por procesar
                </span>
                <span className="rounded-full bg-emerald-50 px-3 py-1 text-xs font-semibold text-emerald-700">
                  {pedidosProcesados.length} procesados
                </span>
                <button
                  type="button"
                  className="rounded-full border border-slate-300 px-3 py-1 text-xs font-semibold text-slate-700 transition hover:border-slate-400 hover:text-slate-900"
                  onClick={() => {
                    setQuery("")
                    onChangeFiltro("todos")
                    setOrden("antiguos")
                  }}
                >
                  Limpiar filtros
                </button>
              </div>
            </section>

            {pedidosFiltrados.length > 0 ? (
              <div className="space-y-6">
                <PedidoGroupSection
                  title="Por procesar"
                  description="Items que siguen requiriendo preparacion antes del retiro logistico."
                  tone="blue"
                  items={pedidosPorProcesar}
                  expandedItemId={expandedItemId}
                  pendingPedidoId={pendingPedidoId}
                  isPendingMutation={marcarListo.isPending}
                  onToggleExpanded={setExpandedItemId}
                  onMarcarListo={(pedidoId) => {
                    setFeedback(null)
                    setPendingPedidoId(pedidoId)
                    marcarListo.mutate(pedidoId, {
                      onSuccess: () => {
                        setFeedback({ variant: "success", message: `Pedido ${pedidoId} marcado como listo para retiro.` })
                        setPendingPedidoId(null)
                      },
                      onError: () => {
                        setFeedback({ variant: "error", message: "No se pudo marcar el pedido como listo para retiro. Reintenta." })
                        setPendingPedidoId(null)
                      },
                    })
                  }}
                />

                <PedidoGroupSection
                  title="Procesados"
                  description="Items que ya fueron marcados como listos para retiro dentro de la bandeja visible."
                  tone="emerald"
                  items={pedidosProcesados}
                  expandedItemId={expandedItemId}
                  pendingPedidoId={pendingPedidoId}
                  isPendingMutation={marcarListo.isPending}
                  onToggleExpanded={setExpandedItemId}
                  onMarcarListo={(pedidoId) => {
                    setFeedback(null)
                    setPendingPedidoId(pedidoId)
                    marcarListo.mutate(pedidoId, {
                      onSuccess: () => {
                        setFeedback({ variant: "success", message: `Pedido ${pedidoId} marcado como listo para retiro.` })
                        setPendingPedidoId(null)
                      },
                      onError: () => {
                        setFeedback({ variant: "error", message: "No se pudo marcar el pedido como listo para retiro. Reintenta." })
                        setPendingPedidoId(null)
                      },
                    })
                  }}
                />
              </div>
            ) : (
              <div className="rounded-3xl border border-slate-200 bg-white p-8 text-sm leading-6 text-slate-600 shadow-sm">
                No hay resultados para los criterios actuales. Ajusta la busqueda o los filtros para volver a explorar la bandeja.
              </div>
            )}
          </>
        ) : (
          <div className="rounded-3xl border border-slate-200 bg-white p-8 text-sm text-slate-600 shadow-sm">
            No hay pedidos pendientes de preparacion. Cuando haya uno listo para operar, aparecera en esta bandeja.
          </div>
        )
      ) : null}
    </section>
  )
}

function resolvePedidosFiltrados(
  pedidos: PedidoProveedorResponse[],
  query: string,
  filtro: FiltroBandeja,
  orden: OrdenBandeja,
) {
  const queryNormalizada = normalizeText(query)

  const filtrados = pedidos.filter((item) => {
    const matchesQuery =
      queryNormalizada.length === 0 ||
      normalizeText(item.pedidoId).includes(queryNormalizada) ||
      normalizeText(item.nombreProducto).includes(queryNormalizada) ||
      normalizeText(item.direccionEnvio || "").includes(queryNormalizada)

    const matchesFiltro =
      filtro === "todos" ? true :
      filtro === "pendientes" ? normalizeText(item.estadoEnvio || "").includes("pendiente") :
      filtro === "con-direccion" ? Boolean(item.direccionEnvio) :
      !item.direccionEnvio

    return matchesQuery && matchesFiltro
  })

  return filtrados.sort((a, b) => {
    const diff = new Date(a.fechaCreacionPedido).getTime() - new Date(b.fechaCreacionPedido).getTime()
    return orden === "antiguos" ? diff : -diff
  })
}

function resolveInitialFiltro(value: string | null): FiltroBandeja {
  const validValues: FiltroBandeja[] = ["todos", "pendientes", "con-direccion", "sin-direccion"]
  return validValues.includes(value as FiltroBandeja) ? (value as FiltroBandeja) : "todos"
}

function normalizeText(value: string) {
  return value
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase()
    .trim()
}

function isPedidoProcesado(item: PedidoProveedorResponse) {
  return normalizeText(item.estadoEnvio || "") === "listo_para_retiro"
}

type PedidoGroupSectionProps = {
  title: string
  description: string
  tone: "blue" | "emerald"
  items: PedidoProveedorResponse[]
  expandedItemId: string | null
  pendingPedidoId: string | null
  isPendingMutation: boolean
  onToggleExpanded: Dispatch<SetStateAction<string | null>>
  onMarcarListo: (pedidoId: string) => void
}

function PedidoGroupSection({
  title,
  description,
  tone,
  items,
  expandedItemId,
  pendingPedidoId,
  isPendingMutation,
  onToggleExpanded,
  onMarcarListo,
}: PedidoGroupSectionProps) {
  const toneStyles =
    tone === "blue"
      ? {
          badge: "bg-blue-50 text-blue-700",
          shell: "border-blue-200/70 bg-[linear-gradient(180deg,#eff6ff_0%,#ffffff_32%)]",
        }
      : {
          badge: "bg-emerald-50 text-emerald-700",
          shell: "border-emerald-200/70 bg-[linear-gradient(180deg,#ecfdf5_0%,#ffffff_32%)]",
        }

  return (
    <section className={`rounded-3xl border p-5 shadow-sm ${toneStyles.shell}`}>
      <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <p className="text-xs font-semibold uppercase tracking-[0.22em] text-slate-500">{title}</p>
          <p className="mt-2 text-sm leading-6 text-slate-600">{description}</p>
        </div>
        <span className={`rounded-full px-3 py-1 text-xs font-semibold ${toneStyles.badge}`}>{items.length} items</span>
      </div>

      <div className="mt-5 space-y-3">
        {items.length > 0 ? (
          items.map((item) => (
            <ProveedorPedidoItem
              key={item.itemId}
              item={item}
              isExpanded={expandedItemId === item.itemId}
              onToggleExpanded={() => {
                onToggleExpanded((current) => (current === item.itemId ? null : item.itemId))
              }}
              onMarcarListo={onMarcarListo}
              isMutating={pendingPedidoId === item.pedidoId && isPendingMutation}
              actionDisabled={Boolean(pendingPedidoId && pendingPedidoId !== item.pedidoId)}
            />
          ))
        ) : (
          <div className="rounded-2xl border border-dashed border-slate-300 bg-white/70 p-4 text-sm text-slate-500">
            Ningun item coincide con este grupo para los filtros actuales.
          </div>
        )}
      </div>
    </section>
  )
}
