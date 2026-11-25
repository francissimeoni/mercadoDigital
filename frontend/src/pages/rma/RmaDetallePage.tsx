import { useState } from "react"
import { Link, useLocation, useParams } from "react-router-dom"
import FeedbackAlert from "../../components/ui/FeedbackAlert"
import {
  useAgregarMensajeRma,
  useAprobarRma,
  useDescargarRotuloDevolucionRma,
  useDescargarRotuloReposicionRma,
  useEntregarReposicionClienteRma,
  useReenviarReposicionClienteRma,
  useRecibirReposicionMdRma,
  useRecibirRmaEnDeposito,
  useResolverRma,
  useRmaDetalle,
  useSolicitarReposicionProveedorRma,
  useSolicitarEnvioRma,
} from "../../hooks/useRma"
import { useCrearTransportista, useTransportistas } from "../../hooks/useLogistica"

const currencyFormatter = new Intl.NumberFormat("es-AR", {
  style: "currency",
  currency: "ARS",
  maximumFractionDigits: 2,
})

function formatFecha(value: string) {
  return new Date(value).toLocaleString("es-AR")
}

function formatEstadoRma(estado: string | null | undefined) {
  const normalized = (estado ?? "").toUpperCase()
  switch (normalized) {
    case "SOLICITADO":
      return "Caso recibido"
    case "APROBADO":
      return "Garantia aprobada"
    case "EN_TRANSITO_DEVOLUCION":
      return "Esperando devolucion"
    case "RECIBIDO_EN_DEPOSITO":
      return "Recibido en deposito"
    case "REEMBOLSADO":
      return "Devolucion de dinero"
    case "RESUELTO":
      return "Caso cerrado con solucion"
    case "RECHAZADO":
      return "Caso rechazado"
    default:
      return estado || "-"
  }
}

function formatResolucionFinal(value: string | null | undefined) {
  const normalized = (value ?? "").toUpperCase()
  switch (normalized) {
    case "REEMBOLSADO":
      return "Devolucion de dinero"
    case "RESUELTO":
      return "Reposicion o solucion aplicada"
    case "RECHAZADO":
      return "Reclamo rechazado"
    default:
      return value || "Sin resolver"
  }
}

function formatEstadoReposicion(value: string | null | undefined) {
  const normalized = (value ?? "").toUpperCase()
  switch (normalized) {
    case "PENDIENTE_SOLICITUD_PROVEEDOR":
      return "Pendiente de pedir al proveedor"
    case "ESPERANDO_INGRESO_MD":
      return "Esperando ingreso en MD"
    case "LISTA_PARA_REENVIO":
      return "Lista para reenviar"
    case "EN_CAMINO_AL_CLIENTE":
      return "En camino al cliente"
    case "ENTREGADA_CLIENTE":
      return "Entregada al cliente"
    case "NO_APLICA":
      return "No aplica"
    default:
      return value || "-"
  }
}

function normalizeEstadoRma(value: string | null | undefined) {
  return (value ?? "").toUpperCase()
}

function canApproveRma(estado: string | null | undefined) {
  return normalizeEstadoRma(estado) === "SOLICITADO"
}

function canReceiveRmaInDeposito(estado: string | null | undefined) {
  return normalizeEstadoRma(estado) === "EN_TRANSITO_DEVOLUCION"
}

function canResolveRma(estado: string | null | undefined) {
  return normalizeEstadoRma(estado) === "RECIBIDO_EN_DEPOSITO"
}

export default function RmaDetallePage() {
  const { rmaId } = useParams()
  const location = useLocation()
  const detalle = useRmaDetalle(rmaId)
  const aprobar = useAprobarRma()
  const solicitarEnvio = useSolicitarEnvioRma()
  const recibir = useRecibirRmaEnDeposito()
  const resolver = useResolverRma()
  const agregarMensaje = useAgregarMensajeRma()
  const descargarRotuloDevolucion = useDescargarRotuloDevolucionRma()
  const descargarRotuloReposicion = useDescargarRotuloReposicionRma()
  const solicitarReposicionProveedor = useSolicitarReposicionProveedorRma()
  const recibirReposicionMd = useRecibirReposicionMdRma()
  const reenviarReposicionCliente = useReenviarReposicionClienteRma()
  const entregarReposicionCliente = useEntregarReposicionClienteRma()
  const transportistas = useTransportistas()
  const crearTransportista = useCrearTransportista()

  const [resolucionFinal, setResolucionFinal] = useState("RESUELTO")
  const [observacionesInspeccion, setObservacionesInspeccion] = useState("")
  const [responsableProbable, setResponsableProbable] = useState("")
  const [montoCompensacion, setMontoCompensacion] = useState("")
  const [referenciaCompensacion, setReferenciaCompensacion] = useState("")
  const [imagenesCierre, setImagenesCierre] = useState<File[]>([])
  const [observacionReposicion, setObservacionReposicion] = useState("")
  const [transportistaReposicionId, setTransportistaReposicionId] = useState("")
  const [nuevoTransportista, setNuevoTransportista] = useState("")
  const [trackingReposicion, setTrackingReposicion] = useState("")
  const [mensajeNuevo, setMensajeNuevo] = useState("")
  const [mensajeInterno, setMensajeInterno] = useState(false)
  const [imagenesMensaje, setImagenesMensaje] = useState<File[]>([])
  const [feedback, setFeedback] = useState<{ variant: "success" | "error"; message: string } | null>(null)
  const [openSections, setOpenSections] = useState<Record<string, boolean>>({
    resumen: true,
    recorrido: false,
    bitacora: false,
    compra: false,
    operacion: true,
    conversacion: false,
  })
  const backPath = location.pathname.startsWith("/admin/rma") ? "/admin/rma" : "/rma"
  const backLabel = location.pathname.startsWith("/admin/rma") ? "Volver a RMA admin" : "Volver a RMA"
  const isClosed = !!detalle.data && ["REEMBOLSADO", "RESUELTO", "RECHAZADO"].includes(detalle.data.estadoRma.toUpperCase())
  const reposicionManualActiva = !!detalle.data && detalle.data.estadoRma.toUpperCase() === "RESUELTO"
  const canApprove = canApproveRma(detalle.data?.estadoRma)
  const canReceive = canReceiveRmaInDeposito(detalle.data?.estadoRma)
  const canResolve = canResolveRma(detalle.data?.estadoRma)
  const transportistaReposicionNombre = nuevoTransportista.trim()
  const puedeReenviarReposicion =
    !!detalle.data?.permiteReenviarReposicionCliente &&
    !reenviarReposicionCliente.isPending &&
    !!trackingReposicion.trim() &&
    (!!transportistaReposicionId || !!transportistaReposicionNombre)

  const crearYSeleccionarTransportista = async () => {
    const nombre = nuevoTransportista.trim()
    if (!nombre) {
      return
    }
    const creado = await crearTransportista.mutateAsync({ nombre })
    setTransportistaReposicionId(creado.id)
    setNuevoTransportista("")
    setFeedback({ variant: "success", message: `Transportista ${creado.nombre} creado y seleccionado.` })
  }

  if (!rmaId) {
    return (
      <section className="rounded-lg border border-amber-300 bg-amber-50 p-6 shadow-sm">
        <h2 className="text-xl font-semibold text-slate-900">Detalle RMA</h2>
        <p className="mt-2 text-sm text-amber-900">No se encontro un identificador de RMA valido.</p>
      </section>
    )
  }

  const resumenRows: Array<[string, string]> = detalle.data
    ? [
        ["RMA ID", detalle.data.id],
        ["Pedido", detalle.data.pedidoId || "-"],
        ["Pedido item", detalle.data.pedidoItemId],
        ["Estado RMA", formatEstadoRma(detalle.data.estadoRma)],
        ["Estado reposicion", formatEstadoReposicion(detalle.data.estadoReposicion)],
        ["Motivo", detalle.data.motivo],
        ["Resolucion final", formatResolucionFinal(detalle.data.resolucionFinal)],
        ["Responsable probable", detalle.data.responsableProbable || "No informado"],
        [
          "Compensacion",
          detalle.data.montoCompensacion != null ? formatCurrency(detalle.data.montoCompensacion) : "No registrada",
        ],
        ["Referencia / comprobante", detalle.data.referenciaCompensacion || "No registrada"],
        ["Creado", formatFecha(detalle.data.createdAt)],
        ["Actualizado", formatFecha(detalle.data.updatedAt)],
        ["Siguiente paso", detalle.data.instruccionesSiguientePaso || "-"],
      ]
    : []

  const compraRows: Array<[string, string]> = detalle.data
    ? [
        ["Cliente", detalle.data.clienteNombre || "-"],
        ["Email cliente", detalle.data.clienteEmail || "-"],
        ["Storefront", detalle.data.storefrontNombre || detalle.data.revendedorNombre || "-"],
        ["Dominio storefront", detalle.data.storefrontDominio || "-"],
        ["Producto", detalle.data.productoNombre || "-"],
        ["Cantidad", String(detalle.data.cantidad ?? "-")],
        ["Proveedor", detalle.data.proveedorNombre || "-"],
        ["Email proveedor", detalle.data.proveedorEmail || "-"],
        ["Precio item", formatCurrency(detailsafe(detalle.data.precioItemFinal))],
        ["Costo item", formatCurrency(detailsafe(detalle.data.precioCosto))],
        ["Seguro", detalle.data.tieneSeguro ? "Si" : "No"],
        ["Monto seguro", formatCurrency(detailsafe(detalle.data.montoSeguro))],
        ["Total envio", formatCurrency(detailsafe(detalle.data.totalEnvioPedido))],
        ["Direccion", detalle.data.direccionEnvio || "-"],
        ["Estado pedido", detalle.data.estadoPedido || "-"],
        ["Estado envio", detalle.data.estadoEnvio || "-"],
        ["Tracking logistico", detalle.data.trackingLogistica || "-"],
        ["Transportista", detalle.data.transportistaNombre || "-"],
        ["Tracking publico", detalle.data.trackingToken || "-"],
      ]
    : []

  const resumenPreview = detalle.data
    ? `${formatEstadoRma(detalle.data.estadoRma)} · ${detalle.data.motivo || "Sin motivo"}`
    : "Estado y motivo del caso"
  const recorridoPreview = detalle.data
    ? `${buildAdminRmaSteps(detalle.data).length} hitos operativos`
    : "Linea de tiempo del reclamo"
  const bitacoraPreview = detalle.data?.reposicionBitacora.length
    ? `${detalle.data.reposicionBitacora.length} eventos de reemplazo`
    : "Sin eventos de reposicion"
  const compraPreview = detalle.data
    ? `${detalle.data.productoNombre || "Producto"} · ${detalle.data.clienteNombre || "Cliente"}`
    : "Producto, cliente y pedido"
  const operacionPreview = isClosed
    ? formatResolucionFinal(detalle.data?.resolucionFinal)
    : detalle.data?.instruccionesSiguientePaso || "Acciones disponibles segun backend"
  const conversacionPreview = detalle.data
    ? `${detalle.data.mensajes.length} mensajes registrados`
    : "Mensajes y notas internas"

  return (
    <section className="space-y-4">
      <header className="rounded-[1.75rem] border border-slate-200 bg-gradient-to-br from-white via-slate-50 to-blue-50 p-5 shadow-sm sm:p-6">
        <Link className="text-sm font-medium text-blue-700 hover:text-blue-900" to={backPath}>
          {"<-"} {backLabel}
        </Link>
        <div className="mt-3 flex flex-wrap items-start justify-between gap-3">
          <div>
            <h2 className="break-all text-2xl font-semibold tracking-tight text-slate-900">RMA {rmaId}</h2>
            <p className="mt-2 text-sm text-slate-600">Detalle operativo del caso y acciones disponibles segun backend.</p>
          </div>
          {detalle.data ? (
            <span className="rounded-full border border-slate-200 bg-white px-3 py-1 text-xs font-semibold uppercase tracking-[0.18em] text-slate-700">
              {formatEstadoRma(detalle.data.estadoRma)}
            </span>
          ) : null}
        </div>
      </header>

      {detalle.data ? (
        <section className="grid gap-3 sm:grid-cols-2">
          <SnapshotCard label="Estado actual" value={formatEstadoRma(detalle.data.estadoRma)} />
          <SnapshotCard
            label="Siguiente paso"
            value={detalle.data.instruccionesSiguientePaso || "Revisar el estado del caso"}
            tone="highlight"
          />
          <SnapshotCard label="Resolucion" value={formatResolucionFinal(detalle.data.resolucionFinal)} />
          <SnapshotCard label="Reposicion" value={formatEstadoReposicion(detalle.data.estadoReposicion)} />
        </section>
      ) : null}

      {detalle.isLoading ? (
        <div className="space-y-3">
          {Array.from({ length: 3 }).map((_, index) => (
            <div key={index} className="h-24 animate-pulse rounded-lg border border-slate-200 bg-slate-200" />
          ))}
        </div>
      ) : null}

      {detalle.isError ? (
        <div className="rounded-lg border border-red-300 bg-red-50 p-6 shadow-sm">
          <p className="text-sm text-red-800">No se pudo cargar el detalle del RMA.</p>
          <button
            type="button"
            className="mt-4 rounded-md bg-red-700 px-4 py-2 text-sm font-medium text-white hover:bg-red-800"
            onClick={() => {
              void detalle.refetch()
            }}
          >
            Reintentar
          </button>
        </div>
      ) : null}

      {!detalle.isLoading && !detalle.isError && !detalle.data ? (
        <div className="rounded-lg border border-amber-300 bg-amber-50 p-6 text-sm text-amber-900 shadow-sm">
          No se encontro un RMA visible con ese identificador.
        </div>
      ) : null}

      {detalle.data ? (
        <div className="grid gap-4 xl:grid-cols-[1.15fr_0.85fr]">
          <section className="space-y-4">
            <section className="rounded-[1.75rem] border border-slate-200 bg-gradient-to-br from-white to-slate-50 p-5 shadow-sm sm:p-6">
              <AccordionHeader
                title="Resumen del caso"
                preview={resumenPreview}
                open={Boolean(openSections.resumen)}
                tone="slate"
                onToggle={() => toggleSection("resumen", setOpenSections)}
              />
              {openSections.resumen ? (
                <>
                  <DetailTable rows={resumenRows} className="mt-4" />
                  {["REEMBOLSADO", "RESUELTO", "RECHAZADO"].includes(detalle.data.estadoRma.toUpperCase()) ? (
                    <div className="mt-4 rounded-[1.25rem] border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm font-medium text-emerald-800">
                      Caso cerrado. El backend ya tomo la resolucion final y este reclamo no deberia seguir en la cola operativa.
                    </div>
                  ) : null}
                </>
              ) : null}
            </section>

            <section className="rounded-[1.75rem] border border-blue-200 bg-gradient-to-br from-white to-blue-50 p-5 shadow-sm sm:p-6">
              <AccordionHeader
                title="Recorrido del caso"
                preview={recorridoPreview}
                open={Boolean(openSections.recorrido)}
                tone="blue"
                onToggle={() => toggleSection("recorrido", setOpenSections)}
              />
              {openSections.recorrido ? (
                <>
                  <p className="mt-2 text-sm text-slate-600">
                    Vista operativa del reclamo, separando devolucion del producto y cierre final.
                  </p>
                  <div className="mt-4 space-y-4">
                    {buildAdminRmaSteps(detalle.data).map((step, index, list) => (
                      <AdminTimelineRow key={step.id} step={step} isLast={index === list.length - 1} />
                    ))}
                  </div>
                </>
              ) : null}
            </section>

            {detalle.data.reposicionBitacora.length > 0 ? (
              <section className="rounded-[1.75rem] border border-emerald-200 bg-gradient-to-br from-white to-emerald-50 p-5 shadow-sm sm:p-6">
                <AccordionHeader
                  title="Bitacora de reposicion"
                  preview={bitacoraPreview}
                  open={Boolean(openSections.bitacora)}
                  tone="emerald"
                  onToggle={() => toggleSection("bitacora", setOpenSections)}
                />
                {openSections.bitacora ? (
                  <>
                    <p className="mt-2 text-sm text-slate-600">
                      Registro manual de los hitos del reemplazo para dejar trazado interno y seguimiento real al cliente.
                    </p>
                    <div className="mt-4 space-y-3">
                      {detalle.data.reposicionBitacora.map((paso) => (
                        <ReposicionBitacoraCard key={paso.id} paso={paso} />
                      ))}
                    </div>
                  </>
                ) : null}
              </section>
            ) : null}

            <section className="rounded-[1.75rem] border border-amber-200 bg-gradient-to-br from-white to-amber-50 p-5 shadow-sm sm:p-6">
              <AccordionHeader
                title="Contexto de compra"
                preview={compraPreview}
                open={Boolean(openSections.compra)}
                tone="amber"
                onToggle={() => toggleSection("compra", setOpenSections)}
              />
              {openSections.compra ? <DetailTable rows={compraRows} className="mt-4" /> : null}
            </section>

            <section className="rounded-[1.75rem] border border-slate-300 bg-gradient-to-br from-white to-slate-100 p-5 shadow-sm sm:p-6">
              <AccordionHeader
                title="Operacion del caso"
                preview={operacionPreview}
                open={Boolean(openSections.operacion)}
                tone="slate"
                onToggle={() => toggleSection("operacion", setOpenSections)}
              />
              {openSections.operacion ? (
                <>
                  <p className="mt-2 text-sm text-slate-600">
                    {isClosed
                      ? reposicionManualActiva
                        ? "El reclamo ya fue resuelto. Desde aca puedes terminar la reposicion manual hasta la entrega al cliente."
                        : "El caso ya quedo cerrado. Solo se conserva la trazabilidad final del reclamo."
                      : "Ejecuta solo transiciones validas segun estado real del backend."}
                  </p>
                  {feedback ? <div className="mt-4"><FeedbackAlert variant={feedback.variant}>{feedback.message}</FeedbackAlert></div> : null}
                  {!isClosed ? (
                    <div className="mt-4 rounded-[1.25rem] border border-blue-200 bg-blue-50 px-4 py-3 text-sm text-blue-900">
                      <span className="font-semibold">Siguiente paso recomendado:</span> {detalle.data.instruccionesSiguientePaso || "Revisar el estado del caso antes de accionar."}
                    </div>
                  ) : null}

                  {isClosed ? (
                    <div className="mt-4 rounded-[1.25rem] border border-slate-200 bg-slate-50 p-4">
                      <p className="text-sm font-semibold text-slate-900">Cierre asentado</p>
                      <div className="mt-3 grid grid-cols-1 gap-3 sm:grid-cols-2">
                        <InfoRow label="Estado final" value={formatEstadoRma(detalle.data.estadoRma)} />
                        <InfoRow label="Resolucion aplicada" value={formatResolucionFinal(detalle.data.resolucionFinal)} />
                        <InfoRow
                          label="Compensacion registrada"
                          value={detalle.data.montoCompensacion != null ? formatCurrency(detalle.data.montoCompensacion) : "No informada"}
                        />
                        <InfoRow label="Referencia / comprobante" value={detalle.data.referenciaCompensacion || "No informado"} />
                      </div>
                    </div>
                  ) : (
                    <>
                      <div className="mt-4 grid gap-2 sm:flex sm:flex-wrap">
                    <button
                      type="button"
                      className="min-h-[44px] w-full rounded-md bg-blue-700 px-4 py-3 text-sm font-medium text-white hover:bg-blue-800 disabled:cursor-not-allowed disabled:opacity-70 sm:w-auto"
                      disabled={!canApprove || aprobar.isPending}
                      onClick={() => {
                        setFeedback(null)
                        aprobar.mutate(rmaId, {
                          onSuccess: () => {
                            setFeedback({ variant: "success", message: `RMA ${rmaId} aprobado correctamente.` })
                          },
                          onError: () => {
                            setFeedback({ variant: "error", message: "No se pudo aprobar el RMA. Verifica que la transicion sea valida." })
                          },
                        })
                      }}
                    >
                      {aprobar.isPending ? "Procesando..." : "Aprobar"}
                    </button>
                    <button
                      type="button"
                      className="min-h-[44px] w-full rounded-md bg-slate-800 px-4 py-3 text-sm font-medium text-white hover:bg-slate-700 disabled:cursor-not-allowed disabled:opacity-70 sm:w-auto"
                      disabled={!detalle.data.permiteSolicitarEnvio || solicitarEnvio.isPending}
                      onClick={() => {
                        setFeedback(null)
                        solicitarEnvio.mutate(rmaId, {
                          onSuccess: () => {
                            setFeedback({ variant: "success", message: `Caso ${rmaId} marcado como envio solicitado.` })
                          },
                          onError: () => {
                            setFeedback({ variant: "error", message: "No se pudo solicitar el envio de devolucion. Revisa el estado actual." })
                          },
                        })
                      }}
                    >
                      {solicitarEnvio.isPending ? "Procesando..." : "Solicitar envio"}
                    </button>

                    <button
                      type="button"
                      className="min-h-[44px] w-full rounded-md bg-amber-600 px-4 py-3 text-sm font-medium text-white hover:bg-amber-700 disabled:cursor-not-allowed disabled:opacity-70 sm:w-auto"
                      disabled={!canReceive || recibir.isPending}
                      onClick={() => {
                        setFeedback(null)
                        recibir.mutate(rmaId, {
                          onSuccess: () => {
                            setFeedback({ variant: "success", message: `RMA ${rmaId} recibido en deposito.` })
                          },
                          onError: () => {
                            setFeedback({ variant: "error", message: "No se pudo registrar la recepcion en deposito. Reintenta." })
                          },
                        })
                      }}
                    >
                      {recibir.isPending ? "Procesando..." : "Recibir en deposito"}
                    </button>
                    <button
                      type="button"
                      className="min-h-[44px] w-full rounded-md border border-slate-300 px-4 py-3 text-sm font-medium text-slate-700 hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-70 sm:w-auto"
                      disabled={!detalle.data.permiteDescargarRotuloDevolucion || descargarRotuloDevolucion.isPending}
                      onClick={() => {
                        setFeedback(null)
                        descargarRotuloDevolucion.mutate(rmaId, {
                          onSuccess: (blob) => {
                            openBlobInNewTab(blob)
                          },
                          onError: () => {
                            setFeedback({ variant: "error", message: "No se pudo generar el rotulo de devolucion." })
                          },
                        })
                      }}
                    >
                      {descargarRotuloDevolucion.isPending ? "Generando..." : "Imprimir rotulo devolucion"}
                    </button>
                    <button
                      type="button"
                      className="min-h-[44px] w-full rounded-md border border-slate-300 px-4 py-3 text-sm font-medium text-slate-700 hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-70 sm:w-auto"
                      disabled={!detalle.data.permiteDescargarRotuloReposicion || descargarRotuloReposicion.isPending}
                      onClick={() => {
                        setFeedback(null)
                        descargarRotuloReposicion.mutate(rmaId, {
                          onSuccess: (blob) => {
                            openBlobInNewTab(blob)
                          },
                          onError: () => {
                            setFeedback({ variant: "error", message: "No se pudo generar la etiqueta de reposicion." })
                          },
                        })
                      }}
                    >
                      {descargarRotuloReposicion.isPending ? "Generando..." : "Imprimir etiqueta reposicion"}
                    </button>
                  </div>
                  <p className="mt-3 text-xs text-slate-500">
                    `Aprobar` solo se habilita en casos recien solicitados. `Recibir en deposito` se habilita cuando la devolucion ya esta en transito.
                  </p>

                  <div className="mt-5 space-y-3 rounded-lg border border-slate-200 bg-slate-50 p-4">
                    <h4 className="text-sm font-semibold text-slate-900">Resolver</h4>
                    <p className="text-sm text-slate-600">
                      El cierre final queda habilitado cuando el producto ya fue registrado como recibido en deposito.
                    </p>
                    <select
                      className="w-full rounded-md border border-slate-300 p-2 text-sm outline-none focus:border-blue-500"
                      value={resolucionFinal}
                      onChange={(event) => {
                        setResolucionFinal(event.target.value)
                      }}
                    >
                      <option value="RESUELTO">RESUELTO</option>
                      <option value="REEMBOLSADO">REEMBOLSADO</option>
                      <option value="RECHAZADO">RECHAZADO</option>
                    </select>
                    <input
                      className="w-full rounded-md border border-slate-300 p-2 text-sm outline-none focus:border-blue-500"
                      placeholder="observacionesInspeccion (opcional)"
                      value={observacionesInspeccion}
                      onChange={(event) => {
                        setObservacionesInspeccion(event.target.value)
                      }}
                    />
                    <input
                      className="w-full rounded-md border border-slate-300 p-2 text-sm outline-none focus:border-blue-500"
                      placeholder="responsableProbable (opcional)"
                      value={responsableProbable}
                      onChange={(event) => {
                        setResponsableProbable(event.target.value)
                      }}
                    />
                    <input
                      className="w-full rounded-md border border-slate-300 p-2 text-sm outline-none focus:border-blue-500"
                      placeholder="montoCompensacion (opcional)"
                      value={montoCompensacion}
                      onChange={(event) => {
                        setMontoCompensacion(event.target.value)
                      }}
                    />
                    <input
                      className="w-full rounded-md border border-slate-300 p-2 text-sm outline-none focus:border-blue-500"
                      placeholder="codigo de transferencia o referencia (opcional)"
                      value={referenciaCompensacion}
                      onChange={(event) => {
                        setReferenciaCompensacion(event.target.value)
                      }}
                    />
                    <label className="block rounded-lg border border-dashed border-slate-300 bg-white px-3 py-3 text-sm text-slate-700">
                      Adjuntar comprobante o evidencia del cierre
                      <input
                        type="file"
                        accept="image/png,image/jpeg,image/webp,image/gif"
                        multiple
                        className="mt-3 block w-full text-sm"
                        onChange={(event) => {
                          setImagenesCierre(Array.from(event.target.files ?? []))
                        }}
                      />
                    </label>
                    {imagenesCierre.length > 0 ? <SelectedFilesList files={imagenesCierre} /> : null}
                    <button
                      type="button"
                      className="rounded-md bg-emerald-700 px-4 py-2 text-sm font-medium text-white hover:bg-emerald-800 disabled:cursor-not-allowed disabled:opacity-70"
                      disabled={!canResolve || resolver.isPending}
                      onClick={() => {
                        setFeedback(null)
                        resolver.mutate(
                          {
                            rmaId,
                            payload: {
                              resolucionFinal,
                              observacionesInspeccion: observacionesInspeccion || undefined,
                              responsableProbable: responsableProbable || undefined,
                              montoCompensacion: montoCompensacion.trim() ? Number(montoCompensacion) : undefined,
                              referenciaCompensacion: referenciaCompensacion || undefined,
                              archivos: imagenesCierre,
                            },
                          },
                          {
                            onSuccess: () => {
                              setImagenesCierre([])
                              setObservacionesInspeccion("")
                              setResponsableProbable("")
                              setMontoCompensacion("")
                              setReferenciaCompensacion("")
                              setFeedback({ variant: "success", message: `RMA ${rmaId} resuelto correctamente.` })
                            },
                            onError: () => {
                              setFeedback({ variant: "error", message: "No se pudo resolver el RMA. Revisa los datos y reintenta." })
                            },
                          },
                        )
                      }}
                    >
                      {resolver.isPending ? "Procesando..." : "Resolver RMA"}
                    </button>
                      </div>
                    </>
                  )}

                  {reposicionManualActiva ? (
                    <div className="mt-5 space-y-4 rounded-lg border border-slate-200 bg-slate-50 p-4">
                  <h4 className="text-sm font-semibold text-slate-900">Gestion manual de reposicion</h4>
                  <p className="text-sm text-slate-600">
                    Este bloque deja trazado el reemplazo sin crear un pedido nuevo: solicitud al proveedor, ingreso en MD, reenvio y entrega final al cliente.
                  </p>
                  <div className="rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-4">
                    <div className="flex flex-wrap items-center justify-between gap-3">
                      <p className="text-sm font-semibold text-emerald-950">Estado actual del reemplazo</p>
                      <span className="rounded-full bg-white px-3 py-1 text-xs font-semibold uppercase tracking-[0.16em] text-emerald-800">
                        {formatEstadoReposicion(detalle.data.estadoReposicion)}
                      </span>
                    </div>
                    <p className="mt-2 text-sm leading-6 text-emerald-900">{detalle.data.resumenReposicion}</p>
                  </div>
                  <div className="grid gap-3 sm:grid-cols-2">
                    <InfoRow label="Tracking de reenvio" value={detalle.data.reposicionTracking || "Todavia no cargado"} />
                    <InfoRow label="Transportista de reenvio" value={detalle.data.reposicionTransportistaNombre || "Todavia no cargado"} />
                  </div>
                  <div className="rounded-md border border-blue-200 bg-blue-50 px-3 py-3 text-sm text-blue-900">
                    Puedes usar un transportista existente o seguir con nombre manual. Si no existe, el backend lo crea o reutiliza al registrar el reenvio.
                  </div>
                  <textarea
                    className="min-h-24 w-full rounded-md border border-slate-300 p-2 text-sm outline-none focus:border-blue-500"
                    placeholder="Observacion operativa para este paso (opcional)"
                    value={observacionReposicion}
                    onChange={(event) => {
                      setObservacionReposicion(event.target.value)
                    }}
                  />
                  <div className="grid gap-3 sm:grid-cols-2">
                    <select
                      className="w-full rounded-md border border-slate-300 p-2 text-sm outline-none focus:border-blue-500"
                      value={transportistaReposicionId}
                      onChange={(event) => {
                        setTransportistaReposicionId(event.target.value)
                      }}
                    >
                      <option value="">Selecciona transportista para reenvio</option>
                      {transportistas.data?.map((transportista) => (
                        <option key={transportista.id} value={transportista.id}>
                          {transportista.nombre}
                        </option>
                      ))}
                    </select>
                    <input
                      className="w-full rounded-md border border-slate-300 p-2 text-sm outline-none focus:border-blue-500"
                      placeholder="Tracking de reenvio al cliente"
                      value={trackingReposicion}
                      onChange={(event) => {
                        setTrackingReposicion(event.target.value)
                      }}
                    />
                  </div>
                  <div className="space-y-2">
                    {transportistas.isError ? (
                      <div className="rounded-md border border-amber-200 bg-amber-50 px-3 py-3 text-sm text-amber-900">
                        No se pudo cargar la lista de transportistas. Puedes seguir igual escribiendo un nombre manual y el sistema lo registrara al reenviar.
                      </div>
                    ) : null}
                    {transportistas.data && transportistas.data.length === 0 ? (
                      <div className="rounded-md border border-amber-200 bg-amber-50 px-3 py-3 text-sm text-amber-900">
                        No hay transportistas cargados todavia. Puedes crear uno rapido desde aca para seguir con el reenvio.
                      </div>
                    ) : null}
                    <div className="grid gap-2 sm:grid-cols-[1fr_auto]">
                      <input
                        className="w-full rounded-md border border-slate-300 p-2 text-sm outline-none focus:border-blue-500"
                        placeholder="Nombre manual o nuevo transportista"
                        value={nuevoTransportista}
                        onChange={(event) => {
                          setNuevoTransportista(event.target.value)
                        }}
                      />
                      <button
                        type="button"
                        className="rounded-md bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-70"
                        disabled={!nuevoTransportista.trim() || crearTransportista.isPending}
                        onClick={() => {
                          setFeedback(null)
                          void crearYSeleccionarTransportista().catch(() => {
                            setFeedback({ variant: "error", message: "No se pudo crear el transportista. Reintenta." })
                          })
                        }}
                      >
                        {crearTransportista.isPending ? "Creando..." : "Crear transportista"}
                      </button>
                    </div>
                  </div>
                  <div className="grid gap-2 sm:grid-cols-2 xl:grid-cols-1">
                    <button
                      type="button"
                      className="w-full rounded-md bg-slate-900 px-4 py-3 text-sm font-medium text-white hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-70"
                      disabled={!detalle.data.permiteSolicitarReposicionProveedor || solicitarReposicionProveedor.isPending}
                      onClick={() => {
                        setFeedback(null)
                        solicitarReposicionProveedor.mutate(
                          { rmaId, payload: { observacion: observacionReposicion || undefined } },
                          {
                            onSuccess: () => {
                              setObservacionReposicion("")
                              setFeedback({ variant: "success", message: "Reposicion solicitada al proveedor." })
                            },
                            onError: () => {
                              setFeedback({ variant: "error", message: "No se pudo registrar la solicitud al proveedor." })
                            },
                          },
                        )
                      }}
                    >
                      {solicitarReposicionProveedor.isPending ? "Procesando..." : "Solicitar reposicion a proveedor"}
                    </button>
                    <button
                      type="button"
                      className="w-full rounded-md bg-amber-700 px-4 py-3 text-sm font-medium text-white hover:bg-amber-800 disabled:cursor-not-allowed disabled:opacity-70"
                      disabled={!detalle.data.permiteRegistrarRecepcionReposicionMd || recibirReposicionMd.isPending}
                      onClick={() => {
                        setFeedback(null)
                        recibirReposicionMd.mutate(
                          { rmaId, payload: { observacion: observacionReposicion || undefined } },
                          {
                            onSuccess: () => {
                              setObservacionReposicion("")
                              setFeedback({ variant: "success", message: "Reposicion recibida por MD." })
                            },
                            onError: () => {
                              setFeedback({ variant: "error", message: "No se pudo registrar la recepcion de la reposicion." })
                            },
                          },
                        )
                      }}
                    >
                      {recibirReposicionMd.isPending ? "Procesando..." : "Recibir reposicion en MD"}
                    </button>
                    <button
                      type="button"
                      className="w-full rounded-md bg-blue-700 px-4 py-3 text-sm font-medium text-white hover:bg-blue-800 disabled:cursor-not-allowed disabled:opacity-70"
                      disabled={!puedeReenviarReposicion}
                      onClick={() => {
                        setFeedback(null)
                        reenviarReposicionCliente.mutate(
                          {
                            rmaId,
                            payload: {
                              transportistaId: transportistaReposicionId || undefined,
                              transportistaNombre: transportistaReposicionNombre || undefined,
                              tracking: trackingReposicion.trim(),
                              observacion: observacionReposicion || undefined,
                            },
                          },
                          {
                            onSuccess: () => {
                              setObservacionReposicion("")
                              setTransportistaReposicionId("")
                              setTrackingReposicion("")
                              setFeedback({ variant: "success", message: "Reposicion reenviada al cliente con tracking registrado." })
                            },
                            onError: () => {
                              setFeedback({ variant: "error", message: "No se pudo registrar el reenvio de la reposicion." })
                            },
                          },
                        )
                      }}
                    >
                      {reenviarReposicionCliente.isPending ? "Procesando..." : "Reenviar al cliente"}
                    </button>
                    <button
                      type="button"
                      className="w-full rounded-md bg-emerald-700 px-4 py-3 text-sm font-medium text-white hover:bg-emerald-800 disabled:cursor-not-allowed disabled:opacity-70"
                      disabled={!detalle.data.permiteEntregarReposicionCliente || entregarReposicionCliente.isPending}
                      onClick={() => {
                        setFeedback(null)
                        entregarReposicionCliente.mutate(
                          { rmaId, payload: { observacion: observacionReposicion || undefined } },
                          {
                            onSuccess: () => {
                              setObservacionReposicion("")
                              setFeedback({ variant: "success", message: "Reposicion marcada como entregada al cliente." })
                            },
                            onError: () => {
                              setFeedback({ variant: "error", message: "No se pudo registrar la entrega de la reposicion." })
                            },
                          },
                        )
                      }}
                    >
                      {entregarReposicionCliente.isPending ? "Procesando..." : "Marcar entregada"}
                    </button>
                  </div>
                    </div>
                  ) : null}
                </>
              ) : null}
            </section>
          </section>

          <section className="rounded-[1.75rem] border border-indigo-200 bg-gradient-to-br from-white to-indigo-50 p-5 shadow-sm sm:p-6">
            <AccordionHeader
              title="Conversacion del caso"
              preview={conversacionPreview}
              open={Boolean(openSections.conversacion)}
              tone="indigo"
              onToggle={() => toggleSection("conversacion", setOpenSections)}
            />
            {openSections.conversacion ? (
              <>
                <div className="mt-4 space-y-3">
                  {detalle.data.mensajes.map((mensaje) => (
                    <article
                      key={mensaje.id}
                      className={`rounded-lg border px-4 py-4 ${mensaje.interno ? "border-amber-200 bg-amber-50" : "border-slate-200 bg-slate-50"}`}
                    >
                      <div className="flex flex-wrap items-center justify-between gap-3">
                        <p className="text-sm font-semibold text-slate-900">
                          {mensaje.autorNombre || mensaje.autorTipo}
                        </p>
                        <p className="text-xs text-slate-500">{formatFecha(mensaje.createdAt)}</p>
                      </div>
                      <p className="mt-2 text-sm leading-6 text-slate-700">{mensaje.mensaje}</p>
                      {mensaje.adjuntos.length > 0 ? (
                        <div className="mt-4 grid gap-3 sm:grid-cols-2">
                          {mensaje.adjuntos.map((adjunto) => (
                            <a
                              key={adjunto.id}
                              href={adjunto.dataUrl}
                              target="_blank"
                              rel="noreferrer"
                              className="overflow-hidden rounded-lg border border-slate-200 bg-white"
                            >
                              <img src={adjunto.dataUrl} alt={adjunto.nombreArchivo} className="h-36 w-full object-cover" />
                              <div className="px-3 py-2 text-xs text-slate-600">{adjunto.nombreArchivo}</div>
                            </a>
                          ))}
                        </div>
                      ) : null}
                      {mensaje.interno ? <p className="mt-2 text-xs font-semibold uppercase tracking-wide text-amber-700">Nota interna</p> : null}
                    </article>
                  ))}
                </div>

                <div className="mt-5 space-y-3 rounded-lg border border-slate-200 bg-slate-50 p-4">
                  <h4 className="text-sm font-semibold text-slate-900">Nuevo mensaje</h4>
                  <textarea
                    className="min-h-28 w-full rounded-md border border-slate-300 p-2 text-sm outline-none focus:border-blue-500"
                    placeholder="Escribe una respuesta para el cliente o una nota interna"
                    value={mensajeNuevo}
                    onChange={(event) => {
                      setMensajeNuevo(event.target.value)
                    }}
                  />
                  <label className="block rounded-lg border border-dashed border-slate-300 bg-white px-3 py-3 text-sm text-slate-700">
                    Adjuntar imagenes
                    <input
                      type="file"
                      accept="image/png,image/jpeg,image/webp,image/gif"
                      multiple
                      className="mt-3 block w-full text-sm"
                      onChange={(event) => {
                        setImagenesMensaje(Array.from(event.target.files ?? []))
                      }}
                    />
                  </label>
                  {imagenesMensaje.length > 0 ? <SelectedFilesList files={imagenesMensaje} /> : null}
                  <label className="flex items-center gap-2 text-sm text-slate-700">
                    <input
                      type="checkbox"
                      checked={mensajeInterno}
                      onChange={(event) => {
                        setMensajeInterno(event.target.checked)
                      }}
                    />
                    Marcar como nota interna
                  </label>
                  <button
                    type="button"
                    className="rounded-md bg-slate-900 px-4 py-2 text-sm font-medium text-white hover:bg-slate-700 disabled:cursor-not-allowed disabled:opacity-70"
                    disabled={!mensajeNuevo.trim() || agregarMensaje.isPending}
                    onClick={() => {
                      setFeedback(null)
                      agregarMensaje.mutate(
                        {
                          rmaId,
                          payload: {
                            mensaje: mensajeNuevo.trim(),
                            interno: mensajeInterno,
                            archivos: imagenesMensaje,
                          },
                        },
                        {
                          onSuccess: () => {
                            setMensajeNuevo("")
                            setMensajeInterno(false)
                            setImagenesMensaje([])
                            setFeedback({ variant: "success", message: "Mensaje agregado correctamente al caso." })
                          },
                          onError: () => {
                            setFeedback({ variant: "error", message: "No se pudo agregar el mensaje. Reintenta." })
                          },
                        },
                      )
                    }}
                  >
                    {agregarMensaje.isPending ? "Enviando..." : "Agregar mensaje"}
                  </button>
                </div>
              </>
            ) : null}
          </section>
        </div>
      ) : null}
    </section>
  )
}

function InfoRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-md border border-slate-200 bg-slate-50 px-3 py-3">
      <p className="text-xs uppercase tracking-wide text-slate-500">{label}</p>
      <p className="mt-1 break-words text-sm text-slate-800">{value}</p>
    </div>
  )
}

function SnapshotCard({
  label,
  value,
  tone = "default",
}: {
  label: string
  value: string
  tone?: "default" | "highlight"
}) {
  return (
    <article className={`rounded-[1.5rem] border px-4 py-4 shadow-sm ${tone === "highlight" ? "border-blue-200 bg-blue-50" : "border-slate-200 bg-white"}`}>
      <p className="text-[11px] font-semibold uppercase tracking-[0.18em] text-slate-500">{label}</p>
      <p className={`mt-2 text-sm leading-6 ${tone === "highlight" ? "font-semibold text-blue-900" : "text-slate-800"}`}>{value}</p>
    </article>
  )
}

function DetailTable({ rows, className = "" }: { rows: Array<[string, string]>; className?: string }) {
  return (
    <div className={`overflow-hidden rounded-[1.25rem] border border-slate-200 bg-slate-50 ${className}`.trim()}>
      <div className="overflow-x-auto">
        <table className="min-w-full text-sm">
          <tbody>
            {rows.map(([label, value]) => (
              <tr key={label} className="border-b border-slate-200 last:border-b-0">
                <th className="w-40 bg-slate-50 px-4 py-3 text-left text-xs font-semibold uppercase tracking-[0.18em] text-slate-500 sm:w-52">
                  {label}
                </th>
                <td className="bg-white px-4 py-3 text-sm text-slate-800">{value}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}

function AccordionHeader({
  title,
  preview,
  open,
  tone,
  onToggle,
}: {
  title: string
  preview: string
  open: boolean
  tone: "slate" | "blue" | "emerald" | "amber" | "indigo"
  onToggle: () => void
}) {
  const toneClass =
    tone === "blue"
      ? "bg-blue-100 text-blue-700"
      : tone === "emerald"
        ? "bg-emerald-100 text-emerald-700"
        : tone === "amber"
          ? "bg-amber-100 text-amber-700"
          : tone === "indigo"
            ? "bg-indigo-100 text-indigo-700"
            : "bg-slate-200 text-slate-700"

  return (
    <button
      type="button"
      onClick={onToggle}
      className="flex w-full items-start justify-between gap-3 rounded-[1.25rem] border border-white/80 bg-white/70 px-4 py-3 text-left transition hover:bg-white"
    >
      <div className="min-w-0">
        <div className="flex flex-wrap items-center gap-2">
          <span className="text-base font-semibold text-slate-900">{title}</span>
          <span className={`rounded-full px-2.5 py-1 text-[11px] font-semibold uppercase tracking-[0.18em] ${toneClass}`}>
            {open ? "Abierto" : "Cerrado"}
          </span>
        </div>
        <p className="mt-1 text-sm text-slate-600">{preview}</p>
      </div>
      <span className={`mt-1 text-sm text-slate-500 transition-transform ${open ? "rotate-180" : ""}`}>▼</span>
    </button>
  )
}

export function SectionToggle({
  title,
  open,
  onToggle,
}: {
  title: string
  open: boolean
  onToggle: () => void
}) {
  return (
    <button
      type="button"
      onClick={onToggle}
      className="flex w-full items-center justify-between gap-3 text-left"
    >
      <span className="text-base font-semibold text-slate-900">{title}</span>
      <span className={`text-sm text-slate-500 transition-transform ${open ? "rotate-180" : ""}`}>▼</span>
    </button>
  )
}

function toggleSection(
  key: string,
  setOpenSections: React.Dispatch<React.SetStateAction<Record<string, boolean>>>,
) {
  setOpenSections((current) => ({
    ...current,
    [key]: !current[key],
  }))
}

function formatCurrency(value: number) {
  return currencyFormatter.format(value)
}

function detailsafe(value: number | null) {
  return value ?? 0
}

function SelectedFilesList({ files }: { files: File[] }) {
  return (
    <div className="rounded-lg border border-slate-200 bg-white px-3 py-3">
      <p className="text-xs font-semibold uppercase tracking-wide text-slate-500">Imagenes listas para subir</p>
      <div className="mt-2 space-y-1">
        {files.map((file) => (
          <p key={`${file.name}-${file.size}`} className="text-sm text-slate-700">
            {file.name}
          </p>
        ))}
      </div>
    </div>
  )
}

function openBlobInNewTab(blob: Blob) {
  const url = window.URL.createObjectURL(blob)
  window.open(url, "_blank", "noopener,noreferrer")
  window.setTimeout(() => window.URL.revokeObjectURL(url), 60_000)
}

function ReposicionBitacoraCard({ paso }: { paso: import("../../types/rma").ReposicionBitacoraPasoResponse }) {
  return (
    <article className="rounded-lg border border-slate-200 bg-slate-50 px-4 py-4">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <p className="text-sm font-semibold text-slate-900">{paso.titulo}</p>
          <p className="mt-1 text-xs uppercase tracking-[0.16em] text-slate-500">{formatFecha(paso.fecha)}</p>
        </div>
        {paso.tracking ? (
          <span className="rounded-full bg-blue-50 px-3 py-1 text-xs font-semibold text-blue-700">
            Tracking {paso.tracking}
          </span>
        ) : null}
      </div>
      {paso.observacion ? <p className="mt-3 text-sm leading-6 text-slate-700">{paso.observacion}</p> : null}
      {paso.transportistaNombre ? (
        <p className="mt-2 text-sm text-slate-600">
          Transportista: <span className="font-medium text-slate-800">{paso.transportistaNombre}</span>
        </p>
      ) : null}
    </article>
  )
}

type AdminRmaStep = {
  id: string
  title: string
  detail: string
  state: "completed" | "current" | "upcoming"
}

function buildAdminRmaSteps(rma: import("../../types/rma").RmaResponse): AdminRmaStep[] {
  const estado = (rma.estadoRma ?? "").toUpperCase()
  const cerrado = ["REEMBOLSADO", "RESUELTO", "RECHAZADO"].includes(estado)
  const reposicionManual = estado === "RESUELTO"

  const steps: AdminRmaStep[] = [
    {
      id: "solicitado",
      title: "Caso recibido",
      detail: `Ingreso original del cliente el ${formatFecha(rma.createdAt)}.`,
      state: "completed",
    },
    {
      id: "aprobado",
      title: "Garantia aprobada",
      detail: rma.fechaAprobacion
        ? `Aprobado el ${formatFecha(rma.fechaAprobacion)}.`
        : "Pendiente de aprobacion formal.",
      state: rma.fechaAprobacion ? "completed" : estado === "SOLICITADO" ? "current" : "upcoming",
    },
    {
      id: "devolucion",
      title: "Circuito de devolucion",
      detail: rma.rotuloDevolucionDescargado
        ? "El cliente ya descargo el rotulo de devolucion."
        : rma.fechaEnvioSolicitado
          ? "Devolucion solicitada; esperando descarga del rotulo o traslado."
          : "Todavia no se solicito el envio del producto.",
      state: rma.rotuloDevolucionDescargado ? "completed" : rma.fechaEnvioSolicitado ? "current" : "upcoming",
    },
    {
      id: "deposito",
      title: "Producto recibido en deposito",
      detail: rma.fechaRecibidoDeposito
        ? `Ingreso confirmado el ${formatFecha(rma.fechaRecibidoDeposito)}.`
        : "Pendiente de recepcion fisica en deposito.",
      state: rma.fechaRecibidoDeposito ? (cerrado ? "completed" : "current") : "upcoming",
    },
    {
      id: "cierre",
      title: buildAdminClosingTitle(rma),
      detail: buildAdminClosingDetail(rma),
      state: cerrado ? "completed" : "upcoming",
    },
  ]

  if (!reposicionManual) {
    return steps
  }

  return [
    ...steps,
    {
      id: "reposicion-proveedor",
      title: "Reposicion solicitada a proveedor",
      detail: rma.fechaReposicionSolicitadaProveedor
        ? `Pedido de reposicion asentado el ${formatFecha(rma.fechaReposicionSolicitadaProveedor)}.`
        : "Pendiente de pedir la reposicion al proveedor.",
      state: rma.fechaReposicionSolicitadaProveedor ? "completed" : "current",
    },
    {
      id: "reposicion-recibida-md",
      title: "Reposicion recibida por MD",
      detail: rma.fechaReposicionRecibidaMd
        ? `MD recibio la reposicion el ${formatFecha(rma.fechaReposicionRecibidaMd)}.`
        : "Pendiente de ingreso fisico del reemplazo en MD.",
      state: rma.fechaReposicionRecibidaMd
        ? "completed"
        : rma.fechaReposicionSolicitadaProveedor
          ? "current"
          : "upcoming",
    },
    {
      id: "reposicion-reenviada",
      title: "Reposicion reenviada al cliente",
      detail: rma.fechaReposicionReenviadaCliente
        ? `Reenvio registrado con ${rma.reposicionTransportistaNombre || "transportista"}${rma.reposicionTracking ? ` y tracking ${rma.reposicionTracking}` : ""}.`
        : "Pendiente de despacho del reemplazo al cliente.",
      state: rma.fechaReposicionReenviadaCliente
        ? "completed"
        : rma.fechaReposicionRecibidaMd
          ? "current"
          : "upcoming",
    },
    {
      id: "reposicion-entregada",
      title: "Reposicion entregada",
      detail: rma.fechaReposicionEntregadaCliente
        ? `Entrega final asentada el ${formatFecha(rma.fechaReposicionEntregadaCliente)}.`
        : "Pendiente de confirmar entrega del reemplazo.",
      state: rma.fechaReposicionEntregadaCliente
        ? "completed"
        : rma.fechaReposicionReenviadaCliente
          ? "current"
          : "upcoming",
    },
  ]
}

function buildAdminClosingTitle(rma: import("../../types/rma").RmaResponse) {
  const estado = (rma.estadoRma ?? "").toUpperCase()
  const resolucion = (rma.resolucionFinal ?? "").toUpperCase()
  if (estado === "REEMBOLSADO" || resolucion === "REEMBOLSADO") {
    return "Devolucion de dinero cerrada"
  }
  if (estado === "RECHAZADO" || resolucion === "RECHAZADO") {
    return "Caso cerrado sin reposicion"
  }
  if (rma.rotuloReposicionGenerado) {
    return "Reposicion preparada"
  }
  if (estado === "RESUELTO" || resolucion === "RESUELTO") {
    return "Reposicion aprobada"
  }
  return "Cierre final"
}

function buildAdminClosingDetail(rma: import("../../types/rma").RmaResponse) {
  const estado = (rma.estadoRma ?? "").toUpperCase()
  const resolucion = (rma.resolucionFinal ?? "").toUpperCase()
  if (estado === "REEMBOLSADO" || resolucion === "REEMBOLSADO") {
    return rma.montoCompensacion != null
      ? `Compensacion registrada por ${formatCurrency(rma.montoCompensacion)}.${rma.referenciaCompensacion ? ` Ref: ${rma.referenciaCompensacion}.` : ""}`
      : "Caso cerrado con devolucion de dinero."
  }
  if (estado === "RECHAZADO" || resolucion === "RECHAZADO") {
    return "El reclamo se cerro como rechazado y queda solo para trazabilidad."
  }
  if (rma.rotuloReposicionGenerado) {
    return "La etiqueta de reposicion ya fue emitida y el reemplazo quedo listo para continuar operativamente."
  }
  if (estado === "RESUELTO" || resolucion === "RESUELTO") {
    return "El caso quedo resuelto con reposicion o solucion operativa."
  }
  return "Todavia no se definio el cierre final del caso."
}

function AdminTimelineRow({ step, isLast }: { step: AdminRmaStep; isLast: boolean }) {
  const circleClass =
    step.state === "completed"
      ? "border-transparent bg-emerald-600 text-white"
      : step.state === "current"
        ? "border-slate-300 bg-white text-slate-900"
        : "border-slate-200 bg-white text-slate-300"

  return (
    <div className="grid grid-cols-[36px_1fr] gap-3">
      <div className="flex flex-col items-center">
        <div className={`flex h-9 w-9 items-center justify-center rounded-full border text-sm font-semibold ${circleClass}`}>
          {step.state === "completed" ? "✓" : "•"}
        </div>
        {!isLast ? <div className="mt-2 w-px flex-1 bg-slate-200" /> : null}
      </div>
      <div className={`rounded-lg border px-4 py-4 ${step.state === "current" ? "border-slate-300 bg-slate-50" : "border-slate-200 bg-white"}`}>
        <div className="flex flex-wrap items-center gap-3">
          <p className="text-sm font-semibold text-slate-900">{step.title}</p>
          {step.state === "current" ? (
            <span className="rounded-full bg-slate-900 px-3 py-1 text-[11px] font-semibold uppercase tracking-[0.16em] text-white">
              Ahora
            </span>
          ) : null}
        </div>
        <p className="mt-2 text-sm leading-6 text-slate-600">{step.detail}</p>
      </div>
    </div>
  )
}
