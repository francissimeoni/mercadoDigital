import { Link } from "react-router-dom"
import { useDespachosPendientes, useRetirosPendientes } from "../../hooks/useLogistica"

function StatusBadge({ status }: { status: "Disponible" | "Parcial" | "Pendiente" }) {
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

function SummaryCard({
  label,
  value,
  detail,
  status,
}: {
  label: string
  value: string
  detail: string
  status: "Disponible" | "Parcial" | "Pendiente"
}) {
  return (
    <article className="rounded-[2rem] border border-slate-200 bg-white p-4 shadow-sm sm:p-5 md:p-6">
      <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
        <p className="text-[11px] font-semibold uppercase tracking-[0.28em] text-slate-400">{label}</p>
        <StatusBadge status={status} />
      </div>
      <p className="mt-3 text-2xl font-semibold tracking-tight text-slate-950 sm:mt-4 sm:text-3xl md:text-4xl">{value}</p>
      <p className="mt-2 text-sm text-slate-600">{detail}</p>
    </article>
  )
}

function ModuleCard({
  title,
  detail,
  status,
  to,
}: {
  title: string
  detail: string
  status: "Disponible" | "Parcial" | "Pendiente"
  to: string
}) {
  return (
    <Link
      to={to}
      className="block rounded-[2rem] border border-slate-200 bg-white p-4 shadow-sm transition hover:border-blue-200 hover:bg-blue-50 sm:p-5 md:p-6"
    >
      <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
        <div>
          <h3 className="text-lg font-semibold tracking-tight text-slate-950 sm:text-xl">{title}</h3>
          <p className="mt-2 text-sm leading-6 text-slate-600">{detail}</p>
        </div>
        <StatusBadge status={status} />
      </div>
      <p className="mt-4 text-sm font-medium text-blue-700 sm:mt-6">Abrir modulo</p>
    </Link>
  )
}

export default function LogisticaDashboardPage() {
  const retirosQuery = useRetirosPendientes()
  const despachosQuery = useDespachosPendientes()

  const hayCarga = retirosQuery.isLoading || despachosQuery.isLoading
  const hayError = retirosQuery.isError || despachosQuery.isError
  const retirosPendientes = retirosQuery.data?.length ?? 0
  const despachosPendientes = despachosQuery.data?.length ?? 0
  const proximaAccion =
    retirosPendientes > 0
      ? `Hay ${retirosPendientes} retiro${retirosPendientes === 1 ? "" : "s"} listo${retirosPendientes === 1 ? "" : "s"} para operar.`
      : despachosPendientes > 0
        ? `Hay ${despachosPendientes} despacho${despachosPendientes === 1 ? "" : "s"} esperando tracking o cierre.`
        : "No hay pendientes urgentes. Podes usar el panel para seguimiento o revision."

  return (
    <section className="space-y-6">
      <header className="rounded-[2rem] border border-slate-200 bg-gradient-to-br from-white via-slate-50 to-sky-50 p-4 shadow-sm sm:p-6 md:p-8">
        <div className="flex flex-col gap-5">
          <div className="flex flex-wrap items-start justify-between gap-4">
            <div className="max-w-4xl">
              <p className="text-xs font-semibold uppercase tracking-[0.35em] text-slate-400">Logistica MD</p>
              <h2 className="mt-2 text-2xl font-semibold tracking-tight text-slate-950 sm:mt-3 sm:text-3xl">Centro operativo logistico</h2>
              <p className="mt-2 text-sm leading-6 text-slate-600 sm:mt-3">
                Vista de entrada del rol para resolver retiros, despachos y seguimiento sin perder de vista el paso
                siguiente de cada pedido.
              </p>
            </div>
            <div className="grid w-full gap-3 sm:flex sm:w-auto sm:flex-wrap">
              <Link
                to="/logistica/envios"
                className="rounded-xl bg-slate-950 px-4 py-3 text-center text-sm font-medium text-white transition hover:bg-slate-800 sm:min-h-[44px]"
              >
                Ir a envios
              </Link>
              <Link
                to="/logistica/seguimiento"
                className="rounded-xl border border-slate-200 bg-white px-4 py-3 text-center text-sm font-medium text-slate-700 transition hover:border-blue-200 hover:bg-blue-50 hover:text-blue-700 sm:min-h-[44px]"
              >
                Seguimiento
              </Link>
            </div>
          </div>
          <div className="grid gap-3 sm:grid-cols-3">
            <article className="rounded-[1.5rem] border border-blue-200 bg-white px-4 py-4 shadow-sm">
              <p className="text-[11px] font-semibold uppercase tracking-[0.18em] text-slate-500">Foco actual</p>
              <p className="mt-2 text-sm font-semibold text-slate-900">{proximaAccion}</p>
            </article>
            <article className="rounded-[1.5rem] border border-slate-200 bg-white px-4 py-4 shadow-sm">
              <p className="text-[11px] font-semibold uppercase tracking-[0.18em] text-slate-500">Cobertura real</p>
              <p className="mt-2 text-sm text-slate-700">Retiros, despachos y rotulos ya operan con backend real.</p>
            </article>
            <article className="rounded-[1.5rem] border border-amber-200 bg-white px-4 py-4 shadow-sm">
              <p className="text-[11px] font-semibold uppercase tracking-[0.18em] text-slate-500">Pendiente</p>
              <p className="mt-2 text-sm text-slate-700">Seguimiento consolidado e incidencias aun siguen como superficie preparada.</p>
            </article>
          </div>
        </div>
      </header>

      {hayCarga ? (
        <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
          {Array.from({ length: 4 }).map((_, index) => (
            <div key={index} className="h-32 animate-pulse rounded-[2rem] border border-slate-200 bg-slate-200 sm:h-36" />
          ))}
        </div>
      ) : null}

      {hayError ? (
        <section className="rounded-[2rem] border border-red-200 bg-red-50 p-8 shadow-sm">
          <h3 className="text-lg font-semibold text-red-900">No se pudo consolidar el resumen logistico</h3>
          <p className="mt-2 text-sm text-red-800">
            Los modulos reales siguen disponibles, pero alguna consulta inicial fallo. Podes reintentar sin perder la
            navegacion del rol.
          </p>
          <button
            type="button"
            className="mt-4 rounded-xl bg-red-700 px-4 py-2 text-sm font-medium text-white hover:bg-red-800"
            onClick={() => {
              void Promise.all([retirosQuery.refetch(), despachosQuery.refetch()])
            }}
          >
            Reintentar
          </button>
        </section>
      ) : null}

      {!hayCarga && !hayError ? (
        <>
          <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
            <SummaryCard
              label="Retiros pendientes"
              value={String(retirosPendientes)}
              detail="Bandeja real disponible para operar retiro."
              status="Disponible"
            />
            <SummaryCard
              label="Despachos pendientes"
              value={String(despachosPendientes)}
              detail="Bandeja real disponible para tracking y cierre."
              status="Disponible"
            />
            <SummaryCard
              label="En transito"
              value="Sin dato"
              detail="Falta el listado backend consolidado para seguimiento."
              status="Parcial"
            />
            <SummaryCard
              label="Incidencias abiertas"
              value="Proximamente"
              detail="Superficie visible, aun sin contrato operativo."
              status="Pendiente"
            />
          </div>

          <div className="grid gap-4 xl:grid-cols-[1.15fr_0.85fr] 2xl:grid-cols-[1.3fr_1fr]">
            <section className="rounded-[2rem] border border-slate-200 bg-white p-4 shadow-sm sm:p-6 md:p-8">
              <p className="text-xs font-semibold uppercase tracking-[0.35em] text-slate-400">Flujo recomendado</p>
              <h3 className="mt-2 text-xl font-semibold tracking-tight text-slate-950 sm:text-2xl">Orden de trabajo sugerido</h3>
              <div className="mt-4 grid gap-3 sm:mt-6">
                <article className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-4">
                  <p className="text-sm font-semibold text-slate-900">1. Resolver retiros</p>
                  <p className="mt-1 text-sm text-slate-600">
                    {retirosPendientes > 0
                      ? `Hay ${retirosPendientes} retiro${retirosPendientes === 1 ? "" : "s"} listo${retirosPendientes === 1 ? "" : "s"} para mover a despacho.`
                      : "No hay retiros pendientes en este momento."}
                  </p>
                </article>
                <article className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-4">
                  <p className="text-sm font-semibold text-slate-900">2. Completar despachos</p>
                  <p className="mt-1 text-sm text-slate-600">
                    {despachosPendientes > 0
                      ? `Hay ${despachosPendientes} despacho${despachosPendientes === 1 ? "" : "s"} esperando tracking o confirmacion final.`
                      : "No hay despachos pendientes por despachar."}
                  </p>
                </article>
                <article className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-4">
                  <p className="text-sm font-semibold text-slate-900">3. Seguimiento y control</p>
                  <p className="mt-1 text-sm text-slate-600">
                    La pantalla ya esta preparada, pero el listado consolidado aun depende de una ampliacion backend.
                  </p>
                </article>
              </div>
            </section>

            <section className="rounded-[2rem] border border-slate-200 bg-white p-4 shadow-sm sm:p-6 md:p-8">
              <p className="text-xs font-semibold uppercase tracking-[0.35em] text-slate-400">Atajos rapidos</p>
              <h3 className="mt-2 text-xl font-semibold tracking-tight text-slate-950 sm:text-2xl">Acciones mas usadas</h3>
              <div className="mt-4 space-y-3 sm:mt-6">
                <Link
                  to="/logistica/envios"
                  className="block rounded-2xl border border-slate-200 bg-slate-50 px-4 py-4 text-sm font-medium text-slate-800 transition hover:border-blue-200 hover:bg-blue-50 hover:text-blue-700"
                >
                  Abrir bandeja operativa de envios
                </Link>
                <Link
                  to="/notificaciones"
                  className="block rounded-2xl border border-slate-200 bg-slate-50 px-4 py-4 text-sm font-medium text-slate-800 transition hover:border-blue-200 hover:bg-blue-50 hover:text-blue-700"
                >
                  Revisar notificaciones del rol
                </Link>
                <Link
                  to="/logistica/seguimiento"
                  className="block rounded-2xl border border-slate-200 bg-slate-50 px-4 py-4 text-sm font-medium text-slate-800 transition hover:border-blue-200 hover:bg-blue-50 hover:text-blue-700"
                >
                  Entrar al seguimiento preparado
                </Link>
              </div>
            </section>
          </div>

          <section className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
            <ModuleCard
              title="Retiros y despachos"
              detail="Bandeja operativa real con transiciones validas, selector de transportista, tracking y rotulos."
              status="Disponible"
              to="/logistica/envios"
            />
            <ModuleCard
              title="Seguimiento de envios"
              detail="Superficie preparada para visualizar envios en transito y proximos pasos de entrega."
              status="Parcial"
              to="/logistica/seguimiento"
            />
            <ModuleCard
              title="Incidencias logisticas"
              detail="Modulo futuro para retiros fallidos, problemas de tracking, cliente ausente y entregas observadas."
              status="Pendiente"
              to="/logistica/incidencias"
            />
            <ModuleCard
              title="Historial reciente"
              detail="Pantalla preparada para proximas consultas de retiros realizados, despachos y entregas confirmadas."
              status="Pendiente"
              to="/logistica/historial"
            />
            <ModuleCard
              title="Descarga de rotulos y detalle"
              detail="Disponible desde la operacion por pedido del modulo real de envios."
              status="Disponible"
              to="/logistica/envios"
            />
            <ModuleCard
              title="Notificaciones"
              detail="Acceso al buzon operativo del usuario logistico para seguimiento transversal."
              status="Disponible"
              to="/notificaciones"
            />
          </section>
        </>
      ) : null}
    </section>
  )
}
