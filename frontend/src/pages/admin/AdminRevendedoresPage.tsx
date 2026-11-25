import { useEffect, useMemo, useState } from "react"
import { Link, useSearchParams } from "react-router-dom"
import { useAdminRevendedores } from "../../hooks/useAdmin"

function formatDate(value: string) {
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? value : date.toLocaleDateString("es-AR")
}

export default function AdminRevendedoresPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const [search, setSearch] = useState(searchParams.get("search") ?? "")
  const [filter, setFilter] = useState<"TODOS" | "ACTIVOS" | "INACTIVOS" | "PUBLICADOS">(
    getFilterValue(searchParams.get("filter")),
  )
  const revendedoresQuery = useAdminRevendedores()
  const revendedores = revendedoresQuery.data ?? []
  const activos = revendedores.filter((revendedor) => revendedor.activo)
  const publicados = revendedores.filter((revendedor) => revendedor.storefront?.publicada)
  const filtrados = useMemo(
    () =>
      revendedores.filter((revendedor) => {
        if (filter === "ACTIVOS" && !revendedor.activo) {
          return false
        }
        if (filter === "INACTIVOS" && revendedor.activo) {
          return false
        }
        if (filter === "PUBLICADOS" && !revendedor.storefront?.publicada) {
          return false
        }
        const haystack = `${revendedor.nombre} ${revendedor.id} ${revendedor.dominio ?? ""}`.toLowerCase()
        return haystack.includes(search.toLowerCase())
      }),
    [filter, revendedores, search],
  )

  useEffect(() => {
    const nextParams = new URLSearchParams(searchParams)
    if (search.trim()) {
      nextParams.set("search", search.trim())
    } else {
      nextParams.delete("search")
    }
    if (filter !== "TODOS") {
      nextParams.set("filter", filter)
    } else {
      nextParams.delete("filter")
    }
    setSearchParams(nextParams, { replace: true })
  }, [filter, search, searchParams, setSearchParams])

  return (
    <section className="space-y-6">
      <header className="rounded-[2rem] border border-slate-200 bg-gradient-to-br from-white via-slate-50 to-sky-50 p-4 shadow-sm sm:p-6 md:p-8">
        <div className="flex flex-col gap-5">
          <div className="flex flex-wrap items-start justify-between gap-4">
            <div className="max-w-3xl">
              <p className="text-xs font-semibold uppercase tracking-[0.35em] text-slate-400">Admin MD</p>
              <h2 className="mt-2 text-2xl font-semibold tracking-tight text-slate-950 sm:mt-3 sm:text-3xl">Revendedores</h2>
              <p className="mt-2 text-sm leading-6 text-slate-600 sm:mt-3">
                Bandeja administrativa conectada a `GET /admin/revendedores`. Resume identidad comercial, estado,
                dominio y salida directa al editor real de storefront.
              </p>
            </div>
            <div className="grid w-full gap-3 sm:flex sm:w-auto sm:flex-wrap">
              <Link
                to="/admin/storefronts"
                className="rounded-xl bg-slate-950 px-4 py-3 text-center text-sm font-medium text-white transition hover:bg-slate-800 sm:min-h-[44px]"
              >
                Abrir storefronts
              </Link>
              <Link
                to="/admin"
                className="rounded-xl border border-slate-200 bg-white px-4 py-3 text-center text-sm font-medium text-slate-700 transition hover:border-blue-200 hover:bg-blue-50 hover:text-blue-700 sm:min-h-[44px]"
              >
                Volver al admin
              </Link>
            </div>
          </div>

          <div className="grid gap-3 sm:grid-cols-3">
            <HeroNote label="Foco actual" detail={`${Math.max(revendedores.length - publicados.length, 0)} tienda(s) siguen pendientes de cierre comercial o publicacion.`} />
            <HeroNote label="Cobertura real" detail="Listado, seleccion y edicion de storefront ya conectan con backend real." />
            <HeroNote label="Uso sugerido" detail="Revisa primero dominios, publicacion y onboarding antes de abrir modulos preparados." />
          </div>
        </div>
      </header>

      <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
        <InfoSummaryCard label="Totales" value={String(revendedores.length)} detail="Revendedores visibles para ADMIN_MD." />
        <InfoSummaryCard label="Activos" value={String(activos.length)} detail="Entidades comerciales habilitadas." />
        <InfoSummaryCard label="Publicadas" value={String(publicados.length)} detail="Tiendas publicadas hoy." />
        <InfoSummaryCard
          label="Pendientes"
          value={String(Math.max(revendedores.length - publicados.length, 0))}
          detail="Casos con storefront no publicada o no configurada."
        />
      </div>

      {!revendedoresQuery.isLoading && !revendedoresQuery.isError && revendedores.length > 0 ? (
        <section className="rounded-[2rem] border border-slate-200 bg-white p-4 shadow-sm sm:p-6">
          <div className="grid gap-3 lg:grid-cols-[minmax(0,1fr)_auto]">
            <input
              className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm outline-none transition focus:border-blue-300 focus:bg-white"
              placeholder="Buscar por nombre, id o dominio"
              value={search}
              onChange={(event) => {
                setSearch(event.target.value)
              }}
            />
            <div className="flex flex-wrap gap-2">
              {(["TODOS", "ACTIVOS", "INACTIVOS", "PUBLICADOS"] as const).map((option) => (
                <button
                  key={option}
                  type="button"
                  onClick={() => {
                    setFilter(option)
                  }}
                  className={`rounded-full px-3 py-2 text-xs font-semibold uppercase tracking-[0.2em] transition ${
                    filter === option ? "bg-slate-950 text-white" : "bg-slate-100 text-slate-600 hover:bg-slate-200"
                  }`}
                >
                  {option}
                </button>
              ))}
            </div>
          </div>
        </section>
      ) : null}

      {revendedoresQuery.isLoading ? (
        <div className="grid gap-4 xl:grid-cols-2">
          {Array.from({ length: 4 }).map((_, index) => (
            <div key={index} className="h-40 animate-pulse rounded-[2rem] border border-slate-200 bg-slate-200" />
          ))}
        </div>
      ) : null}

      {revendedoresQuery.isError ? (
        <section className="rounded-[2rem] border border-red-200 bg-red-50 p-8 shadow-sm">
          <h3 className="text-lg font-semibold text-red-900">No se pudo cargar la bandeja de revendedores</h3>
          <p className="mt-2 text-sm text-red-800">
            Reintenta la lectura del backend para volver a ver los datos reales del panel comercial.
          </p>
          <button
            type="button"
            className="mt-4 rounded-xl bg-red-700 px-4 py-2 text-sm font-medium text-white hover:bg-red-800"
            onClick={() => {
              void revendedoresQuery.refetch()
            }}
          >
            Reintentar
          </button>
        </section>
      ) : null}

      {!revendedoresQuery.isLoading && !revendedoresQuery.isError && revendedores.length === 0 ? (
        <section className="rounded-[2rem] border border-slate-200 bg-white p-8 text-sm text-slate-600 shadow-sm">
          No hay revendedores cargados todavia. Cuando existan, esta bandeja mostrara su estado, storefront y salida
          directa al editor real.
        </section>
      ) : null}

      {!revendedoresQuery.isLoading && !revendedoresQuery.isError && revendedores.length > 0 ? (
        <div className="grid gap-4 xl:grid-cols-2">
          {filtrados.map((revendedor) => (
            <article key={revendedor.id} className="rounded-[2rem] border border-slate-200 bg-white p-5 shadow-sm sm:p-6">
              <div className="flex flex-wrap items-start justify-between gap-3">
                <div>
                  <p className="text-xs font-semibold uppercase tracking-[0.3em] text-slate-400">Revendedor</p>
                  <h3 className="mt-2 text-xl font-semibold tracking-tight text-slate-950">{revendedor.nombre}</h3>
                  <p className="mt-1 break-all text-sm text-slate-500">{revendedor.id}</p>
                </div>
                <div className="flex flex-wrap gap-2">
                  <span
                    className={`rounded-full px-3 py-1 text-xs font-semibold uppercase tracking-[0.2em] ${
                      revendedor.activo ? "bg-emerald-50 text-emerald-700" : "bg-slate-200 text-slate-600"
                    }`}
                  >
                    {revendedor.activo ? "Activo" : "Inactivo"}
                  </span>
                  <span
                    className={`rounded-full px-3 py-1 text-xs font-semibold uppercase tracking-[0.2em] ${
                      revendedor.storefront?.publicada ? "bg-blue-50 text-blue-700" : "bg-amber-50 text-amber-700"
                    }`}
                  >
                    {revendedor.storefront?.publicada ? "Publicada" : "Pendiente"}
                  </span>
                </div>
              </div>

              <div className="mt-5 grid gap-3 sm:grid-cols-2">
                <InfoCell label="Dominio" value={revendedor.dominio ?? "Sin dominio"} />
                <InfoCell label="Nombre comercial" value={revendedor.storefront?.nombreComercial ?? "Sin branding configurado"} />
                <InfoCell label="Alta" value={formatDate(revendedor.createdAt)} />
                <InfoCell label="Ultima actualizacion" value={formatDate(revendedor.updatedAt)} />
              </div>

              <div className="mt-5 grid gap-3 sm:grid-cols-2">
                <ActionLink
                  to={`/admin/storefronts?revendedorId=${revendedor.id}`}
                  title="Editar storefront"
                  detail="Abrir onboarding, branding y publicacion con este revendedor ya seleccionado."
                  primary
                />
                <ActionLink
                  to={`/admin/storefronts?revendedorId=${revendedor.id}`}
                  title="Cobros y SMTP"
                  detail="Entrar directo al panel real para administrar el catalogo y los mails por tienda."
                />
              </div>
            </article>
          ))}
        </div>
      ) : null}
    </section>
  )
}

function getFilterValue(value: string | null): "TODOS" | "ACTIVOS" | "INACTIVOS" | "PUBLICADOS" {
  if (value === "ACTIVOS" || value === "INACTIVOS" || value === "PUBLICADOS") {
    return value
  }
  return "TODOS"
}

function HeroNote({ label, detail }: { label: string; detail: string }) {
  return (
    <article className="rounded-[1.5rem] border border-slate-200 bg-white px-4 py-4 shadow-sm">
      <p className="text-[11px] font-semibold uppercase tracking-[0.18em] text-slate-500">{label}</p>
      <p className="mt-2 text-sm text-slate-800">{detail}</p>
    </article>
  )
}

function InfoSummaryCard({ label, value, detail }: { label: string; value: string; detail: string }) {
  return (
    <article className="rounded-[2rem] border border-slate-200 bg-white p-4 shadow-sm sm:p-5">
      <p className="text-[11px] font-semibold uppercase tracking-[0.28em] text-slate-400">{label}</p>
      <p className="mt-3 text-3xl font-semibold tracking-tight text-slate-950 sm:text-4xl">{value}</p>
      <p className="mt-2 text-sm text-slate-600">{detail}</p>
    </article>
  )
}

function InfoCell({ label, value }: { label: string; value: string }) {
  return (
    <article className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-4">
      <p className="text-[11px] font-semibold uppercase tracking-[0.28em] text-slate-400">{label}</p>
      <p className="mt-2 break-words text-sm font-medium text-slate-800">{value}</p>
    </article>
  )
}

function ActionLink({
  to,
  title,
  detail,
  primary = false,
}: {
  to: string
  title: string
  detail: string
  primary?: boolean
}) {
  return (
    <Link
      to={to}
      className={`block rounded-2xl border px-4 py-4 transition ${
        primary
          ? "border-slate-900 bg-slate-900 text-white hover:bg-slate-800"
          : "border-slate-200 bg-slate-50 text-slate-900 hover:border-blue-200 hover:bg-blue-50"
      }`}
    >
      <p className="text-sm font-semibold">{title}</p>
      <p className={`mt-1 text-sm ${primary ? "text-white/80" : "text-slate-600"}`}>{detail}</p>
    </Link>
  )
}
