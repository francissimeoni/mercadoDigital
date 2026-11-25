import { useEffect, useMemo, useState } from "react"
import { Link, useSearchParams } from "react-router-dom"
import { useRmas } from "../../hooks/useRma"

function formatDateTime(value: string) {
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString("es-AR")
}

function formatEstadoRma(estado: string) {
  const normalized = estado.toUpperCase()
  if (normalized.includes("REEMBOLS")) {
    return "Devolucion de dinero"
  }
  if (normalized.includes("RESUELTO")) {
    return "Cerrado con solucion"
  }
  if (normalized.includes("RECHAZ")) {
    return "Rechazado"
  }
  if (normalized.includes("RECIB")) {
    return "Recibido en deposito"
  }
  if (normalized.includes("TRANSITO")) {
    return "Esperando devolucion"
  }
  if (normalized.includes("APROB")) {
    return "Garantia aprobada"
  }
  if (normalized.includes("SOLICIT")) {
    return "Caso recibido"
  }
  return estado
}

function getRmaTone(estado: string) {
  const normalized = estado.toUpperCase()
  if (normalized.includes("RESUELTO") || normalized.includes("REEMBOLS")) {
    return "border-emerald-200 bg-emerald-50 text-emerald-700"
  }
  if (normalized.includes("RECHAZ")) {
    return "border-slate-200 bg-slate-100 text-slate-600"
  }
  if (normalized.includes("RECIB")) {
    return "border-blue-200 bg-blue-50 text-blue-700"
  }
  if (normalized.includes("APROB")) {
    return "border-indigo-200 bg-indigo-50 text-indigo-700"
  }
  return "border-amber-200 bg-amber-50 text-amber-700"
}

export default function AdminRmaPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const [search, setSearch] = useState(searchParams.get("search") ?? "")
  const [estadoFilter, setEstadoFilter] = useState<"TODOS" | "ABIERTOS" | "RESUELTOS" | "RECHAZADOS">(
    getFilterValue(searchParams.get("estado")),
  )
  const [bandejaAbierta, setBandejaAbierta] = useState(searchParams.get("open") === "1")
  const rmasQuery = useRmas()

  const rmas = rmasQuery.data ?? []
  const abiertos = rmas.filter((rma) => {
    const estado = rma.estadoRma.toUpperCase()
    return !estado.includes("RESUELTO") && !estado.includes("REEMBOLS") && !estado.includes("RECHAZ")
  })
  const resueltos = rmas.filter((rma) => {
    const estado = rma.estadoRma.toUpperCase()
    return estado.includes("RESUELTO") || estado.includes("REEMBOLS")
  })
  const rechazados = rmas.filter((rma) => rma.estadoRma.toUpperCase().includes("RECHAZ"))

  const filtered = useMemo(() => {
    return rmas
      .filter((rma) => {
        if (estadoFilter === "ABIERTOS") {
          return abiertos.some((item) => item.id === rma.id)
        }
        if (estadoFilter === "RESUELTOS") {
          return resueltos.some((item) => item.id === rma.id)
        }
        if (estadoFilter === "RECHAZADOS") {
          return rechazados.some((item) => item.id === rma.id)
        }
        return true
      })
      .filter((rma) => {
        const haystack = `${rma.id} ${rma.pedidoItemId} ${rma.motivo} ${rma.estadoRma}`.toLowerCase()
        return haystack.includes(search.toLowerCase())
      })
  }, [abiertos, estadoFilter, rechazados, resueltos, rmas, search])

  useEffect(() => {
    const nextParams = new URLSearchParams(searchParams)
    if (search.trim()) {
      nextParams.set("search", search.trim())
    } else {
      nextParams.delete("search")
    }
    if (estadoFilter !== "TODOS") {
      nextParams.set("estado", estadoFilter)
    } else {
      nextParams.delete("estado")
    }
    if (bandejaAbierta) {
      nextParams.set("open", "1")
    } else {
      nextParams.delete("open")
    }
    setSearchParams(nextParams, { replace: true })
  }, [bandejaAbierta, estadoFilter, search, searchParams, setSearchParams])

  return (
    <section className="space-y-6">
      <header className="rounded-[2rem] border border-slate-200 bg-gradient-to-br from-white via-slate-50 to-sky-50 p-4 shadow-sm sm:p-6 md:p-8">
        <div className="flex flex-col gap-5">
          <div className="flex flex-wrap items-start justify-between gap-4">
            <div className="max-w-4xl">
              <p className="text-xs font-semibold uppercase tracking-[0.35em] text-slate-400">Admin MD</p>
              <h2 className="mt-2 text-2xl font-semibold tracking-tight text-slate-950 sm:mt-3 sm:text-3xl">RMA</h2>
              <p className="mt-2 text-sm leading-6 text-slate-600 sm:mt-3">
                Bandeja admin de postventa conectada al flujo real. Desde aca podes filtrar, priorizar y abrir cada
                caso sin perder el contexto del panel.
              </p>
            </div>
            <div className="grid w-full gap-3 sm:flex sm:w-auto sm:flex-wrap">
              <button
                type="button"
                className="rounded-xl bg-slate-950 px-4 py-3 text-center text-sm font-medium text-white transition hover:bg-slate-800 sm:min-h-[44px]"
                onClick={() => {
                  setEstadoFilter("ABIERTOS")
                  setBandejaAbierta(true)
                }}
              >
                Ver abiertos
              </button>
              <Link
                to="/admin"
                className="rounded-xl border border-slate-200 bg-white px-4 py-3 text-center text-sm font-medium text-slate-700 transition hover:border-blue-200 hover:bg-blue-50 hover:text-blue-700 sm:min-h-[44px]"
              >
                Volver al admin
              </Link>
            </div>
          </div>

          <div className="grid gap-3 sm:grid-cols-4">
            <SummaryCard label="Totales" value={String(rmas.length)} detail="Casos visibles desde backend." />
            <SummaryCard label="Abiertos" value={String(abiertos.length)} detail="Todavia requieren seguimiento." />
            <SummaryCard label="Resueltos" value={String(resueltos.length)} detail="Cerrados con solucion o reembolso." />
            <SummaryCard label="Rechazados" value={String(rechazados.length)} detail="Casos descartados o no aprobados." />
          </div>
        </div>
      </header>

      <section className="rounded-[2rem] border border-slate-200 bg-white p-4 shadow-sm sm:p-6 md:p-8">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.35em] text-slate-400">Bandeja</p>
            <h3 className="mt-2 text-xl font-semibold tracking-tight text-slate-950 sm:text-2xl">Filtra y entra al detalle</h3>
            <p className="mt-2 text-sm text-slate-600">
              Busca por id, pedido item, motivo o estado. La bandeja puede quedar abierta por query para volver a un contexto puntual.
            </p>
          </div>
          <button
            type="button"
            className="rounded-xl border border-slate-300 bg-white px-4 py-3 text-sm font-medium text-slate-700 transition hover:bg-slate-50"
            onClick={() => {
              setBandejaAbierta((current) => !current)
            }}
          >
            {bandejaAbierta ? "Ocultar bandeja" : "Mostrar bandeja"}
          </button>
        </div>

        <div className="mt-6 grid gap-3 lg:grid-cols-[minmax(0,1fr)_auto]">
          <input
            className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm outline-none transition focus:border-blue-300 focus:bg-white"
            placeholder="Buscar por RMA, pedido item, motivo o estado"
            value={search}
            onChange={(event) => {
              setSearch(event.target.value)
            }}
          />
          <div className="grid grid-cols-2 gap-2 sm:grid-cols-4">
            {(["TODOS", "ABIERTOS", "RESUELTOS", "RECHAZADOS"] as const).map((option) => (
              <button
                key={option}
                type="button"
                onClick={() => {
                  setEstadoFilter(option)
                  setBandejaAbierta(true)
                }}
                className={`rounded-xl px-3 py-3 text-xs font-semibold uppercase tracking-[0.18em] transition ${
                  estadoFilter === option ? "bg-slate-950 text-white" : "bg-slate-100 text-slate-600 hover:bg-slate-200"
                }`}
              >
                {option}
              </button>
            ))}
          </div>
        </div>

        {!bandejaAbierta ? (
          <div className="mt-6 rounded-[1.5rem] border border-dashed border-slate-300 bg-slate-50 px-5 py-6 text-sm text-slate-500">
            La bandeja esta cerrada. Selecciona un filtro o presiona `Mostrar bandeja` para revisar los casos.
          </div>
        ) : null}

        {bandejaAbierta ? (
          <div className="mt-6 space-y-3">
            {rmasQuery.isLoading ? (
              Array.from({ length: 4 }).map((_, index) => (
                <div key={index} className="h-24 animate-pulse rounded-2xl border border-slate-200 bg-slate-200" />
              ))
            ) : null}

            {rmasQuery.isError ? (
              <div className="rounded-2xl border border-red-200 bg-red-50 p-6">
                <p className="text-sm text-red-800">No se pudo cargar la bandeja RMA admin.</p>
                <button
                  type="button"
                  className="mt-4 rounded-xl bg-red-700 px-4 py-2 text-sm font-medium text-white hover:bg-red-800"
                  onClick={() => {
                    void rmasQuery.refetch()
                  }}
                >
                  Reintentar
                </button>
              </div>
            ) : null}

            {!rmasQuery.isLoading && !rmasQuery.isError && filtered.length === 0 ? (
              <div className="rounded-2xl border border-dashed border-slate-300 bg-slate-50 px-4 py-6 text-sm text-slate-500">
                No hay casos que coincidan con el filtro actual.
              </div>
            ) : null}

            {!rmasQuery.isLoading && !rmasQuery.isError
              ? filtered.map((rma) => (
                  <Link
                    key={rma.id}
                    to={`/admin/rma/${rma.id}`}
                    className="block rounded-[1.5rem] border border-slate-200 bg-gradient-to-br from-white to-slate-50 px-4 py-4 shadow-sm transition hover:border-blue-200 hover:from-blue-50 hover:to-white hover:shadow-md"
                  >
                    <div className="flex flex-wrap items-start justify-between gap-3">
                      <div className="min-w-0">
                        <p className="text-sm font-semibold text-slate-900">{rma.motivo}</p>
                        <p className="mt-1 break-all text-xs text-slate-500">{rma.id}</p>
                      </div>
                      <span className={`rounded-full border px-3 py-1 text-xs font-semibold uppercase tracking-[0.2em] ${getRmaTone(rma.estadoRma)}`}>
                        {formatEstadoRma(rma.estadoRma)}
                      </span>
                    </div>

                    <div className="mt-4 grid gap-3 sm:grid-cols-3">
                      <InfoPill label="Pedido item" value={rma.pedidoItemId} />
                      <InfoPill label="Actualizado" value={formatDateTime(rma.updatedAt)} />
                      <InfoPill label="Resolucion" value={rma.resolucionFinal || "Pendiente"} />
                    </div>
                  </Link>
                ))
              : null}
          </div>
        ) : null}
      </section>
    </section>
  )
}

function getFilterValue(value: string | null): "TODOS" | "ABIERTOS" | "RESUELTOS" | "RECHAZADOS" {
  if (value === "ABIERTOS" || value === "RESUELTOS" || value === "RECHAZADOS") {
    return value
  }
  return "TODOS"
}

function SummaryCard({ label, value, detail }: { label: string; value: string; detail: string }) {
  return (
    <article className="rounded-[2rem] border border-slate-200 bg-white p-4 shadow-sm sm:p-5">
      <p className="text-[11px] font-semibold uppercase tracking-[0.28em] text-slate-400">{label}</p>
      <p className="mt-3 text-3xl font-semibold tracking-tight text-slate-950 sm:text-4xl">{value}</p>
      <p className="mt-2 text-sm text-slate-600">{detail}</p>
    </article>
  )
}

function InfoPill({ label, value }: { label: string; value: string }) {
  return (
    <article className="rounded-[1.25rem] border border-slate-200 bg-white px-4 py-4">
      <p className="text-[11px] font-semibold uppercase tracking-[0.18em] text-slate-500">{label}</p>
      <p className="mt-2 break-all text-sm text-slate-800">{value}</p>
    </article>
  )
}
