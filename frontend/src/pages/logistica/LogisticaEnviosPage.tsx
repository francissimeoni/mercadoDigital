import { useMemo, useState } from "react"
import { Link } from "react-router-dom"
import LogisticaDespachoItem from "../../components/logistica/LogisticaDespachoItem"
import LogisticaRetiroItem from "../../components/logistica/LogisticaRetiroItem"
import FeedbackAlert from "../../components/ui/FeedbackAlert"
import {
  useCrearTransportista,
  useDespacharEnvio,
  useDespachosPendientes,
  useDescargarRotulo,
  useEnvioLogistica,
  useMarcarEntregado,
  useMarcarRetirado,
  useRetirosPendientes,
  useTransportistas,
} from "../../hooks/useLogistica"

type VistaActiva = "retiros" | "despachos" | "pedido"

function OperationalStatusBadge({ status }: { status: "Disponible" | "Parcial" | "Pendiente" }) {
  const styles = {
    Disponible: "border-emerald-200 bg-emerald-50 text-emerald-700",
    Parcial: "border-amber-200 bg-amber-50 text-amber-700",
    Pendiente: "border-slate-200 bg-slate-100 text-slate-600",
  }

  return (
    <span className={`rounded-full border px-3 py-1 text-xs font-semibold uppercase tracking-[0.2em] ${styles[status]}`}>
      {status}
    </span>
  )
}

function EmptyPanel({ message }: { message: string }) {
  return (
    <div className="rounded-[1.75rem] border border-dashed border-slate-300 bg-slate-50 px-5 py-6 text-sm text-slate-500">
      {message}
    </div>
  )
}

function ViewModeButton({
  label,
  detail,
  active,
  tone,
  onClick,
}: {
  label: string
  detail: string
  active: boolean
  tone: "slate" | "blue" | "amber"
  onClick: () => void
}) {
  const toneStyles = {
    slate: active ? "border-slate-900 bg-slate-900 text-white" : "border-slate-200 bg-white text-slate-800 hover:border-slate-300",
    blue: active ? "border-blue-700 bg-blue-700 text-white" : "border-blue-200 bg-blue-50/80 text-blue-900 hover:border-blue-300",
    amber: active ? "border-amber-600 bg-amber-600 text-white" : "border-amber-200 bg-amber-50/80 text-amber-900 hover:border-amber-300",
  }

  return (
    <button
      type="button"
      onClick={onClick}
      className={`rounded-[1.5rem] border px-4 py-4 text-left transition sm:min-h-[44px] ${toneStyles[tone]}`}
    >
      <p className="text-base font-semibold">{label}</p>
      <p className={`mt-1 text-sm leading-6 ${active ? "text-white/85" : ""}`}>{detail}</p>
    </button>
  )
}

export default function LogisticaEnviosPage() {
  const [vistaActiva, setVistaActiva] = useState<VistaActiva>("retiros")
  const [pedidoSeleccionadoId, setPedidoSeleccionadoId] = useState<string | null>(null)
  const [transportistaId, setTransportistaId] = useState("")
  const [nuevoTransportista, setNuevoTransportista] = useState("")
  const [tracking, setTracking] = useState("")
  const [retirosSearch, setRetirosSearch] = useState("")
  const [despachosSearch, setDespachosSearch] = useState("")
  const [feedback, setFeedback] = useState<{ variant: "success" | "error" | "info"; message: string } | null>(null)

  const retiros = useRetirosPendientes()
  const despachos = useDespachosPendientes()
  const transportistas = useTransportistas()
  const envio = useEnvioLogistica(pedidoSeleccionadoId)

  const marcarRetirado = useMarcarRetirado()
  const crearTransportista = useCrearTransportista()
  const despachar = useDespacharEnvio()
  const entregar = useMarcarEntregado()
  const descargarRotulo = useDescargarRotulo()

  const retirosFiltrados = useMemo(
    () =>
      (retiros.data ?? []).filter((item) => {
        const haystack = `${item.pedidoId} ${item.nombreProducto} ${item.nombreProveedor} ${item.estadoEnvio ?? ""}`.toLowerCase()
        return haystack.includes(retirosSearch.toLowerCase())
      }),
    [retiros.data, retirosSearch],
  )

  const despachosFiltrados = useMemo(
    () =>
      (despachos.data ?? []).filter((item) => {
        const haystack = `${item.pedidoId} ${item.nombreProducto} ${item.nombreProveedor} ${item.tracking ?? ""}`.toLowerCase()
        return haystack.includes(despachosSearch.toLowerCase())
      }),
    [despachos.data, despachosSearch],
  )

  const hayCargaInicial = retiros.isLoading || despachos.isLoading
  const hayErrorInicial = retiros.isError || despachos.isError
  const transportistaNombreManual = nuevoTransportista.trim()
  const puedeDespachar = (!!transportistaId || !!transportistaNombreManual) && !!tracking.trim() && !despachar.isPending
  const vistaVisible = pedidoSeleccionadoId && vistaActiva === "pedido" ? "pedido" : vistaActiva
  const pedidoSeleccionadoResumen =
    (retiros.data ?? []).find((item) => item.pedidoId === pedidoSeleccionadoId) ??
    (despachos.data ?? []).find((item) => item.pedidoId === pedidoSeleccionadoId)
  const resumenPedidoActivo = pedidoSeleccionadoResumen
    ? `${pedidoSeleccionadoResumen.nombreProducto} - ${pedidoSeleccionadoResumen.nombreProveedor}`
    : "Elegi un pedido desde una de las bandejas"

  const crearYSeleccionarTransportista = async () => {
    const nombre = nuevoTransportista.trim()
    if (!nombre) {
      return
    }

    const creado = await crearTransportista.mutateAsync({ nombre })
    setTransportistaId(creado.id)
    setNuevoTransportista("")
    setFeedback({ variant: "success", message: `Transportista ${creado.nombre} creado y seleccionado.` })
  }

  const descargarPdf = async () => {
    if (!pedidoSeleccionadoId) {
      return
    }

    const blob = await descargarRotulo.mutateAsync(pedidoSeleccionadoId)
    const objectUrl = window.URL.createObjectURL(blob)
    const link = document.createElement("a")
    link.href = objectUrl
    link.download = `rotulo-${pedidoSeleccionadoId}.pdf`
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(objectUrl)
  }

  return (
    <section className="space-y-6">
      {feedback ? <FeedbackAlert variant={feedback.variant}>{feedback.message}</FeedbackAlert> : null}

      {hayCargaInicial ? (
        <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
          {Array.from({ length: 3 }).map((_, index) => (
            <div key={index} className="h-64 animate-pulse rounded-[2rem] border border-slate-200 bg-slate-200 sm:h-80 md:h-96" />
          ))}
        </div>
      ) : null}

      {hayErrorInicial ? (
        <section className="rounded-[2rem] border border-red-200 bg-red-50 p-8 shadow-sm">
          <h3 className="text-lg font-semibold text-red-900">No se pudo cargar la bandeja logistica</h3>
          <p className="mt-2 text-sm text-red-800">
            Fallo al menos una de las consultas reales. Podes reintentar sin perder el contexto operativo.
          </p>
          <button
            type="button"
            className="mt-4 rounded-xl bg-red-700 px-4 py-2 text-sm font-medium text-white hover:bg-red-800"
            onClick={() => {
              void Promise.all([retiros.refetch(), despachos.refetch()])
            }}
          >
            Reintentar
          </button>
        </section>
      ) : null}

      {!hayCargaInicial && !hayErrorInicial ? (
        <section className="rounded-[2rem] border border-slate-200 bg-gradient-to-br from-white via-slate-50 to-sky-50 p-4 shadow-sm sm:p-6">
          <div className="flex flex-col gap-4">
            <div>
              <p className="text-xs font-semibold uppercase tracking-[0.3em] text-slate-400">Operacion diaria</p>
              <h2 className="mt-2 text-2xl font-semibold tracking-tight text-slate-950">Bandeja unificada de retiros y despachos</h2>
              <p className="mt-3 text-sm leading-6 text-slate-600">
                Prioriza retiros, despacha cuando el pedido ya fue retirado y cierra la entrega sin perder el contexto del pedido activo.
              </p>
            </div>
            <div className="grid gap-3 sm:grid-cols-3">
              <HeroMetric
                label="Retiros"
                value={String(retirosFiltrados.length)}
                detail={retirosFiltrados.length > 0 ? "Pendientes para operar" : "Sin pendientes ahora"}
                tone="blue"
              />
              <HeroMetric
                label="Despachos"
                value={String(despachosFiltrados.length)}
                detail={despachosFiltrados.length > 0 ? "Esperando tracking o entrega" : "Sin pendientes ahora"}
                tone="amber"
              />
              <HeroMetric
                label="Pedido activo"
                value={pedidoSeleccionadoId ? "1" : "0"}
                detail={pedidoSeleccionadoId ? "Hay un pedido abierto en detalle" : "Aun no seleccionaste uno"}
                tone="slate"
              />
            </div>
            <div className="rounded-[1.5rem] border border-slate-200 bg-white px-4 py-4 shadow-sm">
              <p className="text-[11px] font-semibold uppercase tracking-[0.18em] text-slate-500">Flujo corto</p>
              <p className="mt-2 text-sm text-slate-700">
                1. Elegi una bandeja. 2. Abri un pedido. 3. Ejecuta la accion siguiente sin salir de la misma pantalla.
              </p>
            </div>
          </div>
        </section>
      ) : null}

      {!hayCargaInicial && !hayErrorInicial && (
        <section className="grid gap-3 sm:grid-cols-3">
          <ViewModeButton
            label="Retiros"
            detail={`${retirosFiltrados.length} visibles para operar retiro.`}
            active={vistaVisible === "retiros"}
            tone="blue"
            onClick={() => {
              setVistaActiva("retiros")
            }}
          />
          <ViewModeButton
            label="Despachos"
            detail={`${despachosFiltrados.length} visibles para tracking y transportista.`}
            active={vistaVisible === "despachos"}
            tone="amber"
            onClick={() => {
              setVistaActiva("despachos")
            }}
          />
          <ViewModeButton
            label="Pedido activo"
            detail={pedidoSeleccionadoId ? `Trabajando sobre ${pedidoSeleccionadoId}.` : "Todavia no seleccionaste un pedido."}
            active={vistaVisible === "pedido"}
            tone="slate"
            onClick={() => {
              setVistaActiva("pedido")
            }}
          />
        </section>
      )}

      {!hayCargaInicial && !hayErrorInicial && vistaVisible === "retiros" ? (
        <section className="space-y-4">
          <div className="rounded-[2rem] border border-blue-200 bg-gradient-to-br from-blue-50/80 to-white p-4 shadow-sm sm:p-6">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div>
                <h3 className="text-xl font-bold tracking-tight text-slate-950 sm:text-2xl">Retiros pendientes</h3>
                <p className="mt-1 text-sm text-slate-600">
                  {retirosFiltrados.length > 0
                    ? `${retirosFiltrados.length} pedido${retirosFiltrados.length > 1 ? "s" : ""} listo${retirosFiltrados.length > 1 ? "s" : ""} para operar`
                    : "No hay retiros pendientes"}
                </p>
              </div>
              <OperationalStatusBadge status="Disponible" />
            </div>
          </div>

          <div className="rounded-2xl border border-slate-200 bg-white p-3 shadow-sm sm:rounded-[1.5rem] sm:p-4">
            <input
              className="w-full rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm outline-none transition placeholder:text-slate-400 focus:border-blue-400 focus:bg-white focus:ring-2 focus:ring-blue-100"
              placeholder="Buscar por pedido, producto, proveedor o estado"
              value={retirosSearch}
              onChange={(event) => {
                setRetirosSearch(event.target.value)
              }}
            />
          </div>

          <div className="space-y-3">
            {retirosFiltrados.length > 0 ? (
              retirosFiltrados.map((item) => (
                <LogisticaRetiroItem
                  key={`${item.pedidoId}-${item.productoId}`}
                  item={item}
                  isMutating={marcarRetirado.isPending}
                  isSelected={pedidoSeleccionadoId === item.pedidoId}
                  onVerDetalle={(pedidoId) => {
                    setPedidoSeleccionadoId(pedidoId)
                    setVistaActiva("pedido")
                    setFeedback({ variant: "info", message: `Pedido ${pedidoId} seleccionado para revisar el detalle del envio.` })
                  }}
                  onMarcarRetirado={(pedidoId) => {
                    setFeedback(null)
                    marcarRetirado.mutate(pedidoId, {
                      onSuccess: () => {
                        setPedidoSeleccionadoId(pedidoId)
                        setVistaActiva("pedido")
                        setFeedback({ variant: "success", message: `Pedido ${pedidoId} marcado como retirado.` })
                      },
                      onError: () => {
                        setFeedback({ variant: "error", message: "No se pudo marcar el pedido como retirado. Reintenta." })
                      },
                    })
                  }}
                />
              ))
            ) : (
              <EmptyPanel message="No hay retiros pendientes visibles con el filtro actual." />
            )}
          </div>
        </section>
      ) : null}

      {!hayCargaInicial && !hayErrorInicial && vistaVisible === "despachos" ? (
        <section className="space-y-4">
          <div className="rounded-[2rem] border border-amber-200 bg-gradient-to-br from-amber-50/80 to-white p-4 shadow-sm sm:p-6">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div>
                <h3 className="text-xl font-bold tracking-tight text-slate-950 sm:text-2xl">Despachos pendientes</h3>
                <p className="mt-1 text-sm text-slate-600">
                  {despachosFiltrados.length > 0
                    ? `${despachosFiltrados.length} pedido${despachosFiltrados.length > 1 ? "s" : ""} listo${despachosFiltrados.length > 1 ? "s" : ""} para tracking`
                    : "No hay despachos pendientes"}
                </p>
              </div>
              <OperationalStatusBadge status="Disponible" />
            </div>
          </div>

          <div className="rounded-2xl border border-slate-200 bg-white p-3 shadow-sm sm:rounded-[1.5rem] sm:p-4">
            <input
              className="w-full rounded-xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm outline-none transition placeholder:text-slate-400 focus:border-amber-400 focus:bg-white focus:ring-2 focus:ring-amber-100"
              placeholder="Buscar por pedido, producto, proveedor o tracking"
              value={despachosSearch}
              onChange={(event) => {
                setDespachosSearch(event.target.value)
              }}
            />
          </div>

          <div className="space-y-3">
            {despachosFiltrados.length > 0 ? (
              despachosFiltrados.map((item) => (
                <LogisticaDespachoItem
                  key={`${item.pedidoId}-${item.productoId}`}
                  item={item}
                  isSelected={pedidoSeleccionadoId === item.pedidoId}
                  onVerDetalle={(pedidoId) => {
                    setPedidoSeleccionadoId(pedidoId)
                    setVistaActiva("pedido")
                    setFeedback({ variant: "info", message: `Pedido ${pedidoId} seleccionado para revisar el detalle del envio.` })
                  }}
                  onSeleccionarPedido={(pedidoId) => {
                    setPedidoSeleccionadoId(pedidoId)
                    setVistaActiva("pedido")
                    setFeedback({ variant: "info", message: `Pedido ${pedidoId} seleccionado para operar tracking, despacho y entrega.` })
                  }}
                />
              ))
            ) : (
              <EmptyPanel message="No hay despachos pendientes visibles con el filtro actual." />
            )}
          </div>
        </section>
      ) : null}

      {!hayCargaInicial && !hayErrorInicial && vistaVisible === "pedido" ? (
        <section className="space-y-4">
          <div className="rounded-[2rem] border border-slate-200 bg-gradient-to-br from-slate-50 to-white p-4 shadow-sm sm:p-6">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div>
                <h3 className="text-xl font-bold tracking-tight text-slate-950 sm:text-2xl">Pedido activo</h3>
                {pedidoSeleccionadoId ? (
                  <p className="mt-1 text-sm text-slate-600">
                    Operando sobre <span className="font-semibold text-slate-900">{pedidoSeleccionadoId}</span>
                  </p>
                ) : (
                  <p className="mt-1 text-sm text-slate-600">Selecciona un pedido para comenzar</p>
                )}
              </div>
              {pedidoSeleccionadoId && (
                <button
                  type="button"
                  className="min-h-[44px] rounded-xl border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-700 transition hover:bg-slate-100"
                  onClick={() => {
                    setPedidoSeleccionadoId(null)
                    setTracking("")
                    setTransportistaId("")
                    setNuevoTransportista("")
                    setVistaActiva("retiros")
                  }}
                >
                  Cerrar
                </button>
              )}
            </div>
            <div className="mt-4 rounded-[1.25rem] border border-slate-200 bg-white px-4 py-4">
              <p className="text-[11px] font-semibold uppercase tracking-[0.18em] text-slate-500">Resumen rapido</p>
              <p className="mt-2 text-sm text-slate-800">{resumenPedidoActivo}</p>
            </div>
          </div>

          {!pedidoSeleccionadoId ? (
            <div className="rounded-[1.5rem] border-2 border-dashed border-slate-300 bg-slate-50 px-6 py-10 text-center text-sm text-slate-600">
              <p className="font-medium">Sin pedido seleccionado</p>
              <p className="mt-1 text-slate-500">Elegi un pedido desde Retiros o Despachos para operar</p>
            </div>
          ) : null}

          {pedidoSeleccionadoId && envio.isLoading && <p className="text-sm text-slate-600">Cargando envio...</p>}

          {pedidoSeleccionadoId && envio.isError && (
            <div className="rounded-2xl border border-red-200 bg-red-50 p-4 text-sm text-red-800">
              No se pudo cargar el envio seleccionado.
            </div>
          )}

          {pedidoSeleccionadoId && envio.data && (
            <div className="space-y-4">
              {pedidoSeleccionadoResumen ? (
                <section className="grid gap-3 sm:grid-cols-2">
                  <OrderSnapshotCard label="Producto" value={pedidoSeleccionadoResumen.nombreProducto} />
                  <OrderSnapshotCard label="Proveedor" value={pedidoSeleccionadoResumen.nombreProveedor} />
                  <OrderSnapshotCard label="Cantidad" value={String(pedidoSeleccionadoResumen.cantidad)} />
                  <OrderSnapshotCard
                    label="Proxima accion"
                    value={buildSiguienteAccionLogistica(envio.data.estadoEnvio)}
                    tone="highlight"
                  />
                </section>
              ) : null}

              <section className="overflow-hidden rounded-[1.5rem] border border-slate-200 bg-slate-50">
                <div className="border-b border-slate-200 bg-white px-4 py-4">
                  <h4 className="text-base font-semibold text-slate-900">Seguimiento del envio</h4>
                </div>
                <div className="divide-y divide-slate-200">
                  <TrackingDetailRow label="Estado de envio" value={envio.data.estadoEnvio} />
                  <TrackingDetailRow label="Tracking" value={envio.data.tracking || "Sin tracking"} />
                  <TrackingDetailRow label="Transportista" value={envio.data.transportistaNombre || "Sin asignar"} />
                  <TrackingDetailRow label="Historial" value="Pendiente de ampliacion backend para timeline de cambios de estado." />
                </div>
              </section>

              {envio.data.estadoEnvio === "RETIRADO" && (
                <div className="space-y-3 rounded-[1.75rem] border border-blue-200 bg-blue-50 p-4 sm:p-5">
                  <p className="text-sm font-semibold text-slate-900">Despachar envio</p>
                  <p className="text-sm leading-6 text-slate-700">
                    Elegi un transportista existente o segui con nombre manual. El backend resuelve o crea el transportista al despachar.
                  </p>
                  <select
                    className="min-h-[44px] w-full rounded-xl border border-blue-200 bg-white p-3 text-sm outline-none focus:border-blue-500"
                    value={transportistaId}
                    onChange={(event) => setTransportistaId(event.target.value)}
                  >
                    <option value="">Seleccionar transportista</option>
                    {transportistas.data?.map((transportista) => (
                      <option key={transportista.id} value={transportista.id}>{transportista.nombre}</option>
                    ))}
                  </select>
                  {transportistas.isError && (
                    <div className="rounded-xl border border-amber-200 bg-amber-50 px-4 py-3 text-sm text-amber-900">
                      No se pudo cargar la lista de transportistas. Podes despachar igual escribiendo un nombre manual.
                    </div>
                  )}
                  <div className="grid gap-3">
                    <div className="grid gap-2 sm:grid-cols-[1fr_auto]">
                      <input
                        className="min-h-[44px] w-full rounded-xl border border-blue-200 bg-white p-3 text-sm outline-none focus:border-blue-500"
                        placeholder="Nombre manual o nuevo transportista"
                        value={nuevoTransportista}
                        onChange={(event) => setNuevoTransportista(event.target.value)}
                      />
                      <button
                        type="button"
                        className="min-h-[44px] rounded-xl bg-slate-900 px-4 py-3 text-sm font-medium text-white hover:bg-slate-700 disabled:cursor-not-allowed disabled:opacity-70"
                        disabled={!nuevoTransportista.trim() || crearTransportista.isPending}
                        onClick={() => {
                          setFeedback(null)
                          void crearYSeleccionarTransportista().catch(() => {
                            setFeedback({ variant: "error", message: "No se pudo crear el transportista." })
                          })
                        }}
                      >
                        {crearTransportista.isPending ? "Creando..." : "Crear"}
                      </button>
                    </div>
                    <input
                      className="min-h-[44px] w-full rounded-xl border border-blue-200 bg-white p-3 text-sm outline-none focus:border-blue-500"
                      placeholder="Tracking"
                      value={tracking}
                      onChange={(event) => setTracking(event.target.value)}
                    />
                  </div>
                  <button
                    type="button"
                    className="min-h-[44px] w-full rounded-xl bg-blue-700 px-4 py-3 text-sm font-medium text-white hover:bg-blue-800 disabled:cursor-not-allowed disabled:opacity-70"
                    disabled={!puedeDespachar}
                    onClick={() => {
                      if (!pedidoSeleccionadoId) return
                      setFeedback(null)
                      despachar.mutate({
                        pedidoId: pedidoSeleccionadoId,
                        payload: {
                          transportistaId: transportistaId || undefined,
                          transportistaNombre: transportistaNombreManual || undefined,
                          tracking: tracking.trim(),
                        },
                      }, {
                        onSuccess: () => setFeedback({ variant: "success", message: `Envio ${pedidoSeleccionadoId} despachado.` }),
                        onError: () => setFeedback({ variant: "error", message: "No se pudo despachar. Revisa los datos." }),
                      })
                    }}
                  >
                    {despachar.isPending ? "Despachando..." : "Marcar despachado"}
                  </button>
                </div>
              )}

              {envio.data.estadoEnvio === "DESPACHADO" && (
                <button
                  type="button"
                  className="min-h-[44px] w-full rounded-xl bg-emerald-600 px-4 py-3 text-sm font-medium text-white hover:bg-emerald-700 disabled:cursor-not-allowed disabled:opacity-70"
                  disabled={entregar.isPending}
                  onClick={() => {
                    if (!pedidoSeleccionadoId) return
                    setFeedback(null)
                    entregar.mutate(pedidoSeleccionadoId, {
                      onSuccess: () => setFeedback({ variant: "success", message: `Envio ${pedidoSeleccionadoId} entregado.` }),
                      onError: () => setFeedback({ variant: "error", message: "No se pudo marcar como entregado." }),
                    })
                  }}
                >
                  {entregar.isPending ? "Procesando..." : "Marcar entregado"}
                </button>
              )}

              <button
                type="button"
                className="min-h-[44px] w-full rounded-xl bg-slate-900 px-4 py-3 text-sm font-medium text-white hover:bg-slate-700 disabled:cursor-not-allowed disabled:opacity-70"
                disabled={descargarRotulo.isPending}
                onClick={() => {
                  setFeedback(null)
                  void descargarPdf()
                    .then(() => setFeedback({ variant: "success", message: "Rotulo descargado." }))
                    .catch(() => setFeedback({ variant: "error", message: "No se pudo descargar el rotulo." }))
                }}
              >
                {descargarRotulo.isPending ? "Descargando..." : "Descargar rotulo PDF"}
              </button>
            </div>
          )}

          {pedidoSeleccionadoId && (
            <section className="rounded-[2rem] border border-slate-200 bg-slate-50 p-4 shadow-sm sm:p-6 md:p-8">
              <div className="flex items-start justify-between gap-3">
                <div>
                  <h3 className="text-lg font-semibold tracking-tight text-slate-950 sm:text-xl">Superficies futuras</h3>
                  <p className="mt-1 text-sm text-slate-600">
                    Quedan visibles, pero fuera del flujo principal para no mezclar operacion real con modulos preparados.
                  </p>
                </div>
                <OperationalStatusBadge status="Pendiente" />
              </div>
              <div className="mt-4 grid gap-3 sm:mt-6 sm:grid-cols-3">
                <Link to="/logistica/seguimiento" className="block min-h-[44px] rounded-2xl border border-slate-200 bg-white px-4 py-4 text-sm font-medium text-slate-700 transition hover:border-blue-200 hover:bg-blue-50 hover:text-blue-700">Seguimiento</Link>
                <Link to="/logistica/incidencias" className="block min-h-[44px] rounded-2xl border border-slate-200 bg-white px-4 py-4 text-sm font-medium text-slate-700 transition hover:border-blue-200 hover:bg-blue-50 hover:text-blue-700">Incidencias</Link>
                <Link to="/logistica/historial" className="block min-h-[44px] rounded-2xl border border-slate-200 bg-white px-4 py-4 text-sm font-medium text-slate-700 transition hover:border-blue-200 hover:bg-blue-50 hover:text-blue-700">Historial</Link>
              </div>
            </section>
          )}
        </section>
      ) : null}
    </section>
  )
}

function TrackingDetailRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex flex-col gap-1 px-4 py-3 sm:flex-row sm:items-start sm:gap-4">
      <dt className="text-xs font-semibold uppercase tracking-[0.18em] text-slate-500 sm:w-40 sm:flex-shrink-0 sm:text-left">
        {label}
      </dt>
      <dd className="text-sm text-slate-700 sm:flex-1">{value}</dd>
    </div>
  )
}

function HeroMetric({
  label,
  value,
  detail,
  tone,
}: {
  label: string
  value: string
  detail: string
  tone: "blue" | "amber" | "slate"
}) {
  const tones = {
    blue: "border-blue-200 bg-white",
    amber: "border-amber-200 bg-white",
    slate: "border-slate-200 bg-white",
  }

  return (
    <article className={`rounded-[1.5rem] border px-4 py-4 shadow-sm ${tones[tone]}`}>
      <p className="text-[11px] font-semibold uppercase tracking-[0.18em] text-slate-500">{label}</p>
      <p className="mt-2 text-3xl font-semibold tracking-tight text-slate-950">{value}</p>
      <p className="mt-2 text-sm text-slate-600">{detail}</p>
    </article>
  )
}

function OrderSnapshotCard({
  label,
  value,
  tone = "default",
}: {
  label: string
  value: string
  tone?: "default" | "highlight"
}) {
  return (
    <article className={`rounded-[1.5rem] border px-4 py-4 ${tone === "highlight" ? "border-blue-200 bg-blue-50" : "border-slate-200 bg-white"}`}>
      <p className="text-[11px] font-semibold uppercase tracking-[0.18em] text-slate-500">{label}</p>
      <p className={`mt-2 text-sm ${tone === "highlight" ? "font-semibold text-blue-900" : "text-slate-800"}`}>{value}</p>
    </article>
  )
}

function buildSiguienteAccionLogistica(estadoEnvio: string) {
  switch (estadoEnvio) {
    case "PENDIENTE_PREPARACION":
    case "LISTO_PARA_RETIRO":
      return "Retirar del proveedor"
    case "RETIRADO":
      return "Despachar con tracking"
    case "DESPACHADO":
      return "Confirmar entrega"
    case "ENTREGADO":
      return "Caso logistico cerrado"
    default:
      return "Revisar estado"
  }
}
