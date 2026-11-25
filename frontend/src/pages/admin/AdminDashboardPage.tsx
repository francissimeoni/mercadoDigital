import { Link } from "react-router-dom"
import { useAuth } from "../../auth/useAuth"
import { useAdminRevendedores } from "../../hooks/useAdmin"
import { useNotificaciones } from "../../hooks/useNotificaciones"
import { useRmas } from "../../hooks/useRma"

function formatDateTime(value: string) {
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString("es-AR")
}

function getRmaTone(estado: string) {
  const normalized = estado.toUpperCase()
  if (normalized.includes("RESUELTO")) {
    return "bg-emerald-50 text-emerald-700"
  }
  if (normalized.includes("RECHAZ")) {
    return "bg-slate-200 text-slate-600"
  }
  if (normalized.includes("RECIB")) {
    return "bg-blue-50 text-blue-700"
  }
  return "bg-amber-50 text-amber-700"
}

export default function AdminDashboardPage() {
  const { session } = useAuth()
  const rmasQuery = useRmas()
  const revendedoresQuery = useAdminRevendedores()
  const notificacionesQuery = useNotificaciones(session?.userId ?? null)

  const rmas = rmasQuery.data ?? []
  const revendedores = revendedoresQuery.data ?? []
  const notificaciones = notificacionesQuery.data ?? []

  const rmasAbiertos = rmas.filter((rma) => {
    const estado = rma.estadoRma.toUpperCase()
    return !estado.includes("RESUELTO") && !estado.includes("RECHAZ")
  })
  const revendedoresActivos = revendedores.filter((revendedor) => revendedor.activo)
  const storefrontsPublicadas = revendedores.filter((revendedor) => revendedor.storefront?.publicada)
  const storefrontsPendientes = revendedores.filter((revendedor) => !revendedor.storefront?.publicada)
  const notificacionesNoLeidas = notificaciones.filter((notificacion) => !notificacion.leida)
  const revendedorPendiente = storefrontsPendientes[0] ?? null
  const rmaPrioritario = [...rmasAbiertos]
    .sort((left, right) => new Date(left.createdAt).getTime() - new Date(right.createdAt).getTime())
    .at(0) ?? null
  const actividadReciente = [
    ...rmas.slice(0, 3).map((rma) => ({
      id: rma.id,
      title: `RMA ${rma.estadoRma}`,
      description: rma.motivo,
      timestamp: rma.updatedAt,
      link: `/admin/rma?search=${encodeURIComponent(rma.id)}&open=1`,
    })),
    ...notificaciones.slice(0, 3).map((notificacion) => ({
      id: notificacion.id,
      title: notificacion.titulo,
      description: notificacion.mensaje,
      timestamp: notificacion.createdAt,
      link: "/admin/notificaciones",
    })),
    ...revendedores.slice(0, 2).map((revendedor) => ({
      id: revendedor.id,
      title: `Revendedor ${revendedor.nombre}`,
      description: revendedor.storefront?.publicada ? "Storefront publicada" : "Storefront pendiente",
      timestamp: revendedor.updatedAt,
      link: `/admin/storefronts?revendedorId=${revendedor.id}`,
    })),
  ]
    .sort((left, right) => new Date(right.timestamp).getTime() - new Date(left.timestamp).getTime())
    .slice(0, 6)

  const loadingAny = rmasQuery.isLoading || revendedoresQuery.isLoading || notificacionesQuery.isLoading
  const errorAny = rmasQuery.isError || revendedoresQuery.isError || notificacionesQuery.isError
  const focoOperativo = rmaPrioritario
    ? `El frente mas urgente hoy es RMA: ${rmasAbiertos.length} caso${rmasAbiertos.length === 1 ? "" : "s"} abierto${rmasAbiertos.length === 1 ? "" : "s"}.`
    : revendedorPendiente
      ? `No hay RMA criticos. Conviene cerrar la storefront pendiente de ${revendedorPendiente.nombre}.`
      : "Sin bloqueos fuertes: podes usar el panel para seguimiento comercial y control fino."

  return (
    <section className="space-y-6">
      <header className="rounded-[2rem] border border-slate-200 bg-gradient-to-br from-white via-slate-50 to-sky-50 p-4 shadow-sm sm:p-6 md:p-8">
        <div className="flex flex-col gap-5">
          <div className="flex flex-wrap items-start justify-between gap-4">
            <div className="max-w-4xl">
              <p className="text-xs font-semibold uppercase tracking-[0.35em] text-slate-400">Admin MD</p>
              <h2 className="mt-2 text-2xl font-semibold tracking-tight text-slate-950 sm:mt-3 sm:text-3xl">Centro de operaciones</h2>
              <p className="mt-2 text-sm leading-6 text-slate-600 sm:mt-3">
                Entrada ejecutiva del panel admin para decidir rapido que cerrar primero entre postventa, red comercial,
                publicaciones y control del sistema.
              </p>
            </div>
            <div className="grid w-full gap-3 sm:flex sm:w-auto sm:flex-wrap">
              <Link
                to="/admin/rma?estado=ABIERTOS&open=1"
                className="rounded-xl bg-slate-950 px-4 py-3 text-center text-sm font-medium text-white transition hover:bg-slate-800 sm:min-h-[44px]"
              >
                Abrir RMA
              </Link>
              <Link
                to="/admin/storefronts"
                className="rounded-xl border border-slate-200 bg-white px-4 py-3 text-center text-sm font-medium text-slate-700 transition hover:border-blue-200 hover:bg-blue-50 hover:text-blue-700 sm:min-h-[44px]"
              >
                Gestionar storefronts
              </Link>
            </div>
          </div>

          <div className="grid gap-3 sm:grid-cols-3">
            <HeroNote label="Foco actual" detail={focoOperativo} tone="blue" />
            <HeroNote
              label="Cobertura real"
              detail="RMA, revendedores, storefronts, notificaciones y configuracion MD ya usan contratos reales."
              tone="slate"
            />
            <HeroNote
              label="Pendiente"
              detail="Pedidos, pagos, auditoria y supervision logistica siguen como superficies preparadas."
              tone="amber"
            />
          </div>
        </div>
      </header>

      {loadingAny ? (
        <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-5">
          {Array.from({ length: 5 }).map((_, index) => (
            <div key={index} className="h-32 animate-pulse rounded-[2rem] border border-slate-200 bg-slate-200 sm:h-36" />
          ))}
        </div>
      ) : null}

      {errorAny ? (
        <section className="rounded-[2rem] border border-red-200 bg-red-50 p-8 shadow-sm">
          <h3 className="text-lg font-semibold text-red-900">No se pudo consolidar el dashboard admin</h3>
          <p className="mt-2 text-sm text-red-800">
            Al menos una de las fuentes reales fallo al responder. Podes reintentar sin perder navegacion ni contexto.
          </p>
          <div className="mt-4 flex flex-wrap gap-3">
            <button
              type="button"
              className="rounded-xl bg-red-700 px-4 py-2 text-sm font-medium text-white hover:bg-red-800"
              onClick={() => {
                void Promise.all([rmasQuery.refetch(), revendedoresQuery.refetch(), notificacionesQuery.refetch()])
              }}
            >
              Reintentar
            </button>
            <Link
              to="/admin/notificaciones"
              className="rounded-xl border border-red-300 bg-white px-4 py-2 text-sm font-medium text-red-700 hover:bg-red-100"
            >
              Ver notificaciones
            </Link>
          </div>
        </section>
      ) : null}

      {!loadingAny && !errorAny ? (
        <>
          <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-5">
            <KpiCard label="RMA abiertos" value={String(rmasAbiertos.length)} tone="amber" detail="Casos aun no resueltos." />
            <KpiCard label="Revendedores activos" value={String(revendedoresActivos.length)} tone="blue" detail="Entidades comerciales habilitadas." />
            <KpiCard label="Storefronts publicadas" value={String(storefrontsPublicadas.length)} tone="emerald" detail="Tiendas white-label visibles." />
            <KpiCard label="No leidas" value={String(notificacionesNoLeidas.length)} tone="slate" detail="Alertas del usuario admin actual." />
            <KpiCard label="Revendedores totales" value={String(revendedores.length)} tone="blue" detail="Base administrativa visible." />
          </div>

          <div className="grid gap-4 xl:grid-cols-[1.2fr_0.8fr]">
            <section className="rounded-[2rem] border border-slate-200 bg-white p-4 shadow-sm sm:p-6 md:p-8">
              <p className="text-xs font-semibold uppercase tracking-[0.35em] text-slate-400">Orden sugerido</p>
              <h3 className="mt-2 text-xl font-semibold tracking-tight text-slate-950 sm:text-2xl">Que cerrar primero</h3>
              <div className="mt-4 grid gap-3 sm:mt-6">
                <PriorityLink
                  to="/admin/rma?estado=ABIERTOS&open=1"
                  title="1. Postventa y RMA"
                  detail={`${rmasAbiertos.length} caso${rmasAbiertos.length === 1 ? "" : "s"} abierto${rmasAbiertos.length === 1 ? "" : "s"} para revisar o resolver.`}
                  tone="amber"
                />
                <PriorityLink
                  to={revendedorPendiente ? `/admin/storefronts?revendedorId=${revendedorPendiente.id}` : "/admin/storefronts"}
                  title="2. Publicaciones pendientes"
                  detail={`${storefrontsPendientes.length} storefront${storefrontsPendientes.length === 1 ? "" : "s"} sin publicacion o sin cerrar onboarding.`}
                  tone="blue"
                />
                <PriorityLink
                  to="/admin/notificaciones"
                  title="3. Alertas del panel"
                  detail={`${notificacionesNoLeidas.length} evento${notificacionesNoLeidas.length === 1 ? "" : "s"} sin leer en la cuenta actual.`}
                  tone="slate"
                />
              </div>
            </section>

            <section className="rounded-[2rem] border border-slate-200 bg-white p-4 shadow-sm sm:p-6 md:p-8">
              <p className="text-xs font-semibold uppercase tracking-[0.35em] text-slate-400">Atajos reales</p>
              <h3 className="mt-2 text-xl font-semibold tracking-tight text-slate-950 sm:text-2xl">Acciones mas usadas</h3>
              <div className="mt-4 space-y-3 sm:mt-6">
                <QuickLink to="/admin/revendedores" title="Revisar revendedores" detail="Listado real con estado, dominio y storefront." />
                <QuickLink to="/admin/storefronts" title="Editar storefronts" detail="Onboarding, branding, SMTP y Mercado Pago por revendedor." />
                <QuickLink to="/admin/canonizacion" title="Aprobar canonizacion" detail="Bandeja real para asociar productos proveedor al catalogo canonico." />
                <QuickLink to="/admin/configuracion" title="Ajustar costo MD" detail="Configuracion operativa real por categoria de paquete." />
                <QuickLink to="/admin/notificaciones" title="Abrir notificaciones" detail="Bandeja real del usuario admin actual." />
              </div>
            </section>
          </div>

          <div className="grid gap-4 xl:grid-cols-[1.15fr_0.85fr]">
            <section className="rounded-[2rem] border border-slate-200 bg-white p-4 shadow-sm sm:p-6 md:p-8">
              <div className="flex flex-wrap items-start justify-between gap-4">
                <div>
                  <p className="text-xs font-semibold uppercase tracking-[0.35em] text-slate-400">Actividad reciente</p>
                  <h3 className="mt-2 text-xl font-semibold tracking-tight text-slate-950 sm:text-2xl">Movimientos del panel</h3>
                </div>
                <Link
                  to="/admin/notificaciones"
                  className="rounded-xl border border-slate-200 bg-slate-50 px-4 py-2 text-sm font-medium text-slate-700 transition hover:border-blue-200 hover:bg-blue-50 hover:text-blue-700"
                >
                  Ver notificaciones
                </Link>
              </div>

              <div className="mt-6 space-y-3">
                {actividadReciente.length > 0 ? (
                  actividadReciente.map((item) => (
                    <Link
                      key={item.id}
                      to={item.link}
                      className="block rounded-2xl border border-slate-200 bg-slate-50 px-4 py-4 transition hover:border-blue-200 hover:bg-blue-50"
                    >
                      <p className="text-sm font-semibold text-slate-900">{item.title}</p>
                      <p className="mt-1 text-sm text-slate-600">{item.description}</p>
                      <p className="mt-2 text-xs text-slate-500">{formatDateTime(item.timestamp)}</p>
                    </Link>
                  ))
                ) : (
                  <EmptyCard message="Todavia no hay actividad suficiente para mostrar en esta bandeja." />
                )}
              </div>
            </section>

            <section className="rounded-[2rem] border border-slate-200 bg-white p-4 shadow-sm sm:p-6 md:p-8">
              <p className="text-xs font-semibold uppercase tracking-[0.35em] text-slate-400">RMA prioritarios</p>
              <h3 className="mt-2 text-xl font-semibold tracking-tight text-slate-950 sm:text-2xl">Casos para mirar ahora</h3>
              <div className="mt-6 space-y-3">
                {rmasAbiertos.length > 0 ? (
                  rmasAbiertos
                    .sort((left, right) => new Date(left.createdAt).getTime() - new Date(right.createdAt).getTime())
                    .slice(0, 4)
                    .map((rma) => (
                      <Link
                        key={rma.id}
                        to={`/admin/rma?search=${encodeURIComponent(rma.id)}&open=1`}
                        className="block rounded-2xl border border-slate-200 bg-slate-50 px-4 py-4 transition hover:border-blue-200 hover:bg-blue-50"
                      >
                        <div className="flex flex-wrap items-start justify-between gap-3">
                          <div>
                            <p className="text-sm font-semibold text-slate-900">{rma.motivo}</p>
                            <p className="mt-1 break-all text-xs text-slate-500">{rma.id}</p>
                          </div>
                          <span className={`rounded-full px-3 py-1 text-xs font-semibold uppercase tracking-[0.2em] ${getRmaTone(rma.estadoRma)}`}>
                            {rma.estadoRma}
                          </span>
                        </div>
                        <p className="mt-2 text-xs text-slate-500">{formatDateTime(rma.createdAt)}</p>
                      </Link>
                    ))
                ) : (
                  <EmptyCard message="No hay RMA abiertos en este momento." />
                )}
              </div>
            </section>
          </div>
        </>
      ) : null}
    </section>
  )
}

function HeroNote({ label, detail, tone }: { label: string; detail: string; tone: "blue" | "slate" | "amber" }) {
  const tones = {
    blue: "border-blue-200",
    slate: "border-slate-200",
    amber: "border-amber-200",
  }

  return (
    <article className={`rounded-[1.5rem] border bg-white px-4 py-4 shadow-sm ${tones[tone]}`}>
      <p className="text-[11px] font-semibold uppercase tracking-[0.18em] text-slate-500">{label}</p>
      <p className="mt-2 text-sm text-slate-800">{detail}</p>
    </article>
  )
}

function KpiCard({
  label,
  value,
  detail,
  tone,
}: {
  label: string
  value: string
  detail: string
  tone: "amber" | "blue" | "emerald" | "slate"
}) {
  const tones = {
    amber: "border-amber-200 bg-gradient-to-br from-white to-amber-50",
    blue: "border-blue-200 bg-gradient-to-br from-white to-blue-50",
    emerald: "border-emerald-200 bg-gradient-to-br from-white to-emerald-50",
    slate: "border-slate-200 bg-gradient-to-br from-white to-slate-50",
  }

  return (
    <article className={`rounded-[2rem] border p-4 shadow-sm sm:p-5 ${tones[tone]}`}>
      <p className="text-[11px] font-semibold uppercase tracking-[0.28em] text-slate-400">{label}</p>
      <p className="mt-3 text-3xl font-semibold tracking-tight text-slate-950 sm:text-4xl">{value}</p>
      <p className="mt-2 text-sm text-slate-600">{detail}</p>
    </article>
  )
}

function PriorityLink({
  to,
  title,
  detail,
  tone,
}: {
  to: string
  title: string
  detail: string
  tone: "amber" | "blue" | "slate"
}) {
  const tones = {
    amber: "border-amber-200 bg-amber-50/70",
    blue: "border-blue-200 bg-blue-50/70",
    slate: "border-slate-200 bg-slate-50",
  }

  return (
    <Link to={to} className={`block rounded-2xl border px-4 py-4 transition hover:border-slate-300 hover:bg-white ${tones[tone]}`}>
      <p className="text-sm font-semibold text-slate-900">{title}</p>
      <p className="mt-1 text-sm text-slate-600">{detail}</p>
    </Link>
  )
}

function EmptyCard({ message }: { message: string }) {
  return <div className="rounded-2xl border border-dashed border-slate-300 bg-slate-50 px-4 py-5 text-sm text-slate-500">{message}</div>
}

function QuickLink({ to, title, detail }: { to: string; title: string; detail: string }) {
  return (
    <Link
      to={to}
      className="block rounded-2xl border border-slate-200 bg-slate-50 px-4 py-4 transition hover:border-blue-200 hover:bg-blue-50"
    >
      <p className="text-sm font-semibold text-slate-900">{title}</p>
      <p className="mt-1 text-sm text-slate-600">{detail}</p>
    </Link>
  )
}
