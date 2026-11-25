import { useState } from "react"
import { Link, useNavigate } from "react-router-dom"
import FeedbackAlert from "../../components/ui/FeedbackAlert"
import { useRmas, useSolicitarRma } from "../../hooks/useRma"

function formatFecha(value: string) {
  return new Date(value).toLocaleString("es-AR")
}

export default function RmaPage() {
  const [pedidoItemId, setPedidoItemId] = useState("")
  const [motivo, setMotivo] = useState("")
  const [descripcion, setDescripcion] = useState("")
  const [rmaIdBusqueda, setRmaIdBusqueda] = useState("")
  const [busqueda, setBusqueda] = useState("")
  const [feedback, setFeedback] = useState<{ variant: "success" | "error" | "info"; message: string } | null>(null)
  const navigate = useNavigate()

  const rmas = useRmas()
  const solicitar = useSolicitarRma()
  const listaRmas = rmas.data ?? []
  const rmasFiltrados = listaRmas.filter((rma) => {
    const haystack = `${rma.id} ${rma.pedidoItemId} ${rma.motivo} ${rma.estadoRma} ${rma.productoNombre ?? ""} ${rma.clienteNombre ?? ""}`.toLowerCase()
    return haystack.includes(busqueda.toLowerCase())
  })
  const abiertos = listaRmas.filter((rma) => !["REEMBOLSADO", "RESUELTO", "RECHAZADO"].includes(rma.estadoRma.toUpperCase())).length
  const cerrados = listaRmas.length - abiertos

  const onSolicitar = async () => {
    if (!pedidoItemId || !motivo) {
      return
    }

    await solicitar.mutateAsync({
      pedidoItemId,
      motivo,
      descripcion: descripcion || undefined,
    })
    setDescripcion("")
    setMotivo("")
    setPedidoItemId("")
    setFeedback({ variant: "success", message: "RMA solicitado correctamente. Ya puedes revisar su detalle o seguir operando." })
  }

  return (
    <section className="space-y-4">
      <header className="rounded-[2rem] border border-slate-200 bg-gradient-to-br from-white via-slate-50 to-emerald-50 p-5 shadow-sm sm:p-6">
        <div className="flex flex-col gap-4">
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.3em] text-slate-400">Postventa y garantias</p>
            <h2 className="mt-2 text-2xl font-semibold tracking-tight text-slate-950">Bandeja RMA operativa</h2>
            <p className="mt-3 text-sm leading-6 text-slate-600">
              Flujo real para solicitar, aprobar, pedir devolucion, recibir en deposito y cerrar con reposicion, reembolso o rechazo.
            </p>
          </div>
          <div className="grid gap-3 sm:grid-cols-3">
            <SummaryMetric label="Casos visibles" value={String(listaRmas.length)} detail="Consulta real al backend" />
            <SummaryMetric label="Abiertos" value={String(abiertos)} detail="Todavia requieren operacion" tone="amber" />
            <SummaryMetric label="Cerrados" value={String(cerrados)} detail="Quedan para trazabilidad" tone="emerald" />
          </div>
        </div>
      </header>

      <div className="grid gap-4 xl:grid-cols-[0.95fr_1.05fr]">
        <section className="rounded-[2rem] border border-slate-200 bg-white p-5 shadow-sm sm:p-6">
          <h3 className="text-base font-semibold text-slate-900">Solicitar RMA</h3>
          <p className="mt-2 text-sm leading-6 text-slate-600">
            Requiere `pedidoItemId` real. En demo local puedes usar `bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbb03`.
          </p>

          {feedback ? (
            <div className="mt-4">
              <FeedbackAlert variant={feedback.variant}>{feedback.message}</FeedbackAlert>
            </div>
          ) : null}

          <div className="mt-4 grid gap-3">
            <input
              className="rounded-xl border border-slate-300 p-3 text-sm outline-none focus:border-blue-500"
              placeholder="pedidoItemId"
              value={pedidoItemId}
              onChange={(event) => {
                setPedidoItemId(event.target.value)
              }}
            />
            <input
              className="rounded-xl border border-slate-300 p-3 text-sm outline-none focus:border-blue-500"
              placeholder="motivo"
              value={motivo}
              onChange={(event) => {
                setMotivo(event.target.value)
              }}
            />
            <textarea
              className="min-h-28 rounded-xl border border-slate-300 p-3 text-sm outline-none focus:border-blue-500"
              placeholder="descripcion (opcional)"
              value={descripcion}
              onChange={(event) => {
                setDescripcion(event.target.value)
              }}
            />
            <button
              type="button"
              className="min-h-[44px] w-full rounded-xl bg-blue-700 px-4 py-3 text-sm font-medium text-white hover:bg-blue-800 disabled:cursor-not-allowed disabled:opacity-70 sm:w-fit"
              disabled={!pedidoItemId || !motivo || solicitar.isPending}
              onClick={() => {
                setFeedback(null)
                void onSolicitar().catch(() => {
                  setFeedback({ variant: "error", message: "No se pudo solicitar el RMA. Verifica los datos y el estado del pedido." })
                })
              }}
            >
              {solicitar.isPending ? "Solicitando..." : "Solicitar RMA"}
            </button>
          </div>
        </section>

        <section className="rounded-[2rem] border border-slate-200 bg-white p-5 shadow-sm sm:p-6">
          <h3 className="text-base font-semibold text-slate-900">Abrir detalle por ID</h3>
          <p className="mt-2 text-sm leading-6 text-slate-600">
            Si ya tienes el identificador del caso, entra directo al detalle sin buscarlo manualmente en la bandeja.
          </p>
          <div className="mt-4 grid gap-3">
            <input
              className="w-full rounded-xl border border-slate-300 p-3 text-sm outline-none focus:border-blue-500"
              placeholder="rmaId"
              value={rmaIdBusqueda}
              onChange={(event) => {
                setRmaIdBusqueda(event.target.value)
              }}
            />
            <button
              type="button"
              className="min-h-[44px] rounded-xl bg-slate-900 px-4 py-3 text-sm font-medium text-white hover:bg-slate-700 disabled:cursor-not-allowed disabled:opacity-70 sm:w-fit"
              disabled={!rmaIdBusqueda}
              onClick={() => {
                setFeedback({ variant: "info", message: `Abriendo detalle del RMA ${rmaIdBusqueda}.` })
                navigate(`/rma/${rmaIdBusqueda}`)
              }}
            >
              Ver detalle
            </button>
          </div>

          <div className="mt-6 rounded-[1.5rem] border border-slate-200 bg-slate-50 p-4">
            <p className="text-sm font-semibold text-slate-900">Lectura rapida</p>
            <div className="mt-3 space-y-2 text-sm text-slate-600">
              <p>Abiertos: casos que siguen esperando aprobacion, devolucion, revision o cierre.</p>
              <p>Cerrados: reembolso, rechazo o resolucion final ya asentada.</p>
              <p>Desde cada tarjeta puedes entrar al detalle y continuar la operacion.</p>
            </div>
          </div>
        </section>
      </div>

      <section className="space-y-3">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
          <div>
            <h3 className="text-lg font-semibold text-slate-900">Bandeja RMA</h3>
            <p className="mt-1 text-sm text-slate-600">
              {rmasFiltrados.length} caso{rmasFiltrados.length === 1 ? "" : "s"} visible{rmasFiltrados.length === 1 ? "" : "s"} con el filtro actual.
            </p>
          </div>
          <input
            className="w-full rounded-xl border border-slate-300 bg-white px-4 py-3 text-sm outline-none focus:border-blue-500 sm:max-w-sm"
            placeholder="Buscar por RMA, item, motivo, producto o estado"
            value={busqueda}
            onChange={(event) => {
              setBusqueda(event.target.value)
            }}
          />
        </div>

        {rmas.isLoading ? (
          <div className="space-y-3">
            {Array.from({ length: 3 }).map((_, index) => (
              <div key={index} className="h-32 animate-pulse rounded-[1.75rem] border border-slate-200 bg-slate-200" />
            ))}
          </div>
        ) : null}

        {rmas.isError ? (
          <div className="rounded-[1.75rem] border border-red-300 bg-red-50 p-6 shadow-sm">
            <p className="text-sm text-red-800">No se pudo cargar la bandeja RMA desde backend.</p>
            <button
              type="button"
              className="mt-4 rounded-xl bg-red-700 px-4 py-3 text-sm font-medium text-white hover:bg-red-800"
              onClick={() => {
                void rmas.refetch()
              }}
            >
              Reintentar
            </button>
          </div>
        ) : null}

        {!rmas.isLoading && !rmas.isError ? (
          rmasFiltrados.length > 0 ? (
            <div className="grid gap-4 xl:grid-cols-2">
              {rmasFiltrados.map((rma) => (
                <article key={rma.id} className="rounded-[1.75rem] border border-slate-200 bg-white p-5 shadow-sm transition hover:border-slate-300 hover:shadow-md">
                  <div className="flex flex-col gap-3">
                    <div className="flex flex-wrap items-center gap-2">
                      <StatusChip value={rma.estadoRma} />
                      {rma.resolucionFinal ? <ResolutionChip value={rma.resolucionFinal} /> : null}
                    </div>
                    <p className="break-all text-sm font-semibold text-slate-900">{rma.id}</p>
                    <div>
                      <p className="text-base font-semibold text-slate-950">{rma.productoNombre || rma.motivo}</p>
                      <p className="mt-1 text-sm text-slate-600">{rma.motivo}</p>
                    </div>
                  </div>

                  <div className="mt-4 grid gap-3 sm:grid-cols-2">
                    <MiniInfo label="Pedido item" value={rma.pedidoItemId} />
                    <MiniInfo label="Fecha" value={formatFecha(rma.createdAt)} />
                    <MiniInfo label="Storefront" value={rma.storefrontNombre || rma.revendedorNombre || "-"} />
                    <MiniInfo label="Siguiente paso" value={rma.instruccionesSiguientePaso || "Revisar detalle"} highlight />
                  </div>

                  <div className="mt-4 flex flex-col gap-2 sm:flex-row">
                    <Link
                      to={`/rma/${rma.id}`}
                      className="inline-flex min-h-[44px] items-center justify-center rounded-xl bg-slate-900 px-4 py-3 text-sm font-medium text-white hover:bg-slate-700"
                    >
                      Ver detalle
                    </Link>
                    <button
                      type="button"
                      className="inline-flex min-h-[44px] items-center justify-center rounded-xl border border-slate-300 bg-white px-4 py-3 text-sm font-medium text-slate-700 hover:bg-slate-50"
                      onClick={() => {
                        setRmaIdBusqueda(rma.id)
                        setFeedback({ variant: "info", message: `RMA ${rma.id} listo para abrirse desde la caja de acceso directo.` })
                      }}
                    >
                      Preparar acceso directo
                    </button>
                  </div>
                </article>
              ))}
            </div>
          ) : (
            <div className="rounded-[1.75rem] border border-slate-200 bg-white p-6 text-sm text-slate-600 shadow-sm">
              No hay RMAs visibles para el usuario actual con el filtro actual. Puedes solicitar uno nuevo o buscarlo por ID.
            </div>
          )
        ) : null}
      </section>
    </section>
  )
}

function SummaryMetric({
  label,
  value,
  detail,
  tone = "default",
}: {
  label: string
  value: string
  detail: string
  tone?: "default" | "amber" | "emerald"
}) {
  const toneClass =
    tone === "amber" ? "border-amber-200 bg-white" : tone === "emerald" ? "border-emerald-200 bg-white" : "border-slate-200 bg-white"

  return (
    <article className={`rounded-[1.5rem] border px-4 py-4 shadow-sm ${toneClass}`}>
      <p className="text-[11px] font-semibold uppercase tracking-[0.18em] text-slate-500">{label}</p>
      <p className="mt-2 text-3xl font-semibold tracking-tight text-slate-950">{value}</p>
      <p className="mt-2 text-sm text-slate-600">{detail}</p>
    </article>
  )
}

function StatusChip({ value }: { value: string }) {
  const normalized = value.toUpperCase()
  const styles =
    normalized === "SOLICITADO"
      ? "border-sky-200 bg-sky-100 text-sky-800"
      : normalized === "APROBADO" || normalized === "EN_TRANSITO_DEVOLUCION" || normalized === "RECIBIDO_EN_DEPOSITO"
        ? "border-amber-200 bg-amber-100 text-amber-800"
        : "border-emerald-200 bg-emerald-100 text-emerald-800"

  return <span className={`rounded-full border px-3 py-1 text-[11px] font-semibold uppercase tracking-[0.16em] ${styles}`}>{value}</span>
}

function ResolutionChip({ value }: { value: string }) {
  const normalized = value.toUpperCase()
  const styles =
    normalized === "RECHAZADO"
      ? "border-rose-200 bg-rose-100 text-rose-800"
      : normalized === "REEMBOLSADO"
        ? "border-violet-200 bg-violet-100 text-violet-800"
        : "border-emerald-200 bg-emerald-100 text-emerald-800"

  return <span className={`rounded-full border px-3 py-1 text-[11px] font-semibold uppercase tracking-[0.16em] ${styles}`}>{value}</span>
}

function MiniInfo({ label, value, highlight = false }: { label: string; value: string; highlight?: boolean }) {
  return (
    <div className={`rounded-[1.25rem] border px-4 py-3 ${highlight ? "border-blue-200 bg-blue-50" : "border-slate-200 bg-slate-50"}`}>
      <p className="text-[11px] font-semibold uppercase tracking-[0.18em] text-slate-500">{label}</p>
      <p className={`mt-2 text-sm ${highlight ? "font-medium text-blue-900" : "text-slate-800"}`}>{value}</p>
    </div>
  )
}
