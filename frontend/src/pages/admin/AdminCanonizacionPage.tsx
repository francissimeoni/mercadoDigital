import { useEffect, useMemo, useState } from "react"
import FeedbackAlert from "../../components/ui/FeedbackAlert"
import {
  useAdminCanonizacionAsignados,
  useAdminCanonizacionCandidatos,
  useAdminCanonizacionPendientes,
  useDesassociateAdminCanonizacionProducto,
  useAssociateAdminCanonizacionProducto,
  useCreateAdminCanonizacionCanonico,
} from "../../hooks/useAdmin"
import type { AdminCanonizacionCrearCanonicoRequest, AdminCanonizacionPendienteResponse } from "../../types/admin"

type EditorMessage = {
  variant: "success" | "error" | "info"
  title: string
  body: string
}

const emptyForm: AdminCanonizacionCrearCanonicoRequest = {
  marca: "",
  modelo: "",
  nombrePublico: "",
  atributosJson: "",
}

function formatMoney(amount: number, currency: string) {
  try {
    return new Intl.NumberFormat("es-AR", { style: "currency", currency }).format(amount)
  } catch {
    return `${amount} ${currency}`
  }
}

function formatDate(value: string) {
  const parsed = new Date(value)
  return Number.isNaN(parsed.getTime()) ? value : parsed.toLocaleString("es-AR")
}

function buildDefaultCanonico(pendiente: AdminCanonizacionPendienteResponse | null): AdminCanonizacionCrearCanonicoRequest {
  if (!pendiente) {
    return emptyForm
  }
  return {
    marca: pendiente.proveedorNombre,
    modelo: pendiente.sku,
    nombrePublico: pendiente.nombre,
    atributosJson: "",
  }
}

export default function AdminCanonizacionPage() {
  const pendientesQuery = useAdminCanonizacionPendientes()
  const [selectedProductoId, setSelectedProductoId] = useState<string | null>(null)
  const [candidateSearch, setCandidateSearch] = useState("")
  const [asignadoSearch, setAsignadoSearch] = useState("")
  const [pendingSearch, setPendingSearch] = useState("")
  const [proveedorFilter, setProveedorFilter] = useState("ALL")
  const [categoriaFilter, setCategoriaFilter] = useState("ALL")
  const [stockFilter, setStockFilter] = useState<"ALL" | "CON_STOCK" | "SIN_STOCK">("ALL")
  const [message, setMessage] = useState<EditorMessage | null>(null)
  const [form, setForm] = useState<AdminCanonizacionCrearCanonicoRequest>(emptyForm)
  const [recentlyRevertedIds, setRecentlyRevertedIds] = useState<string[]>([])

  const candidatosQuery = useAdminCanonizacionCandidatos(candidateSearch)
  const asignadosQuery = useAdminCanonizacionAsignados(asignadoSearch)
  const asociarMutation = useAssociateAdminCanonizacionProducto()
  const crearMutation = useCreateAdminCanonizacionCanonico()
  const desasociarMutation = useDesassociateAdminCanonizacionProducto()

  const pendientes = pendientesQuery.data ?? []
  const candidatos = candidatosQuery.data ?? []
  const asignados = asignadosQuery.data ?? []
  const proveedoresDisponibles = useMemo(
    () => Array.from(new Set(pendientes.map((item) => item.proveedorNombre))).sort((a, b) => a.localeCompare(b)),
    [pendientes],
  )
  const categoriasDisponibles = useMemo(
    () => Array.from(new Set(pendientes.map((item) => item.categoriaPaquete))).sort((a, b) => a.localeCompare(b)),
    [pendientes],
  )
  const pendientesFiltrados = useMemo(() => {
    const needle = pendingSearch.trim().toLowerCase()
    return pendientes.filter((item) => {
      const matchesSearch = !needle
        || item.nombre.toLowerCase().includes(needle)
        || item.sku.toLowerCase().includes(needle)
        || item.proveedorNombre.toLowerCase().includes(needle)
      const matchesProveedor = proveedorFilter === "ALL" || item.proveedorNombre === proveedorFilter
      const matchesCategoria = categoriaFilter === "ALL" || item.categoriaPaquete === categoriaFilter
      const matchesStock =
        stockFilter === "ALL"
        || (stockFilter === "CON_STOCK" && item.stockDisponible > 0)
        || (stockFilter === "SIN_STOCK" && item.stockDisponible <= 0)
      return matchesSearch && matchesProveedor && matchesCategoria && matchesStock
    })
  }, [categoriaFilter, pendingSearch, pendientes, proveedorFilter, stockFilter])
  const selectedPendiente = useMemo(
    () => pendientesFiltrados.find((item) => item.productoId === selectedProductoId) ?? null,
    [pendientesFiltrados, selectedProductoId],
  )

  useEffect(() => {
    if (pendientesFiltrados.length === 0) {
      setSelectedProductoId(null)
      return
    }
    const currentExists = pendientesFiltrados.some((item) => item.productoId === selectedProductoId)
    if (!currentExists) {
      setSelectedProductoId(pendientesFiltrados[0].productoId)
    }
  }, [pendientesFiltrados, selectedProductoId])

  useEffect(() => {
    setForm(buildDefaultCanonico(selectedPendiente))
  }, [selectedPendiente?.productoId])

  const handleAsociar = async (productoCanonicoId: string) => {
    if (!selectedPendiente) return
    setMessage(null)
    try {
      const result = await asociarMutation.mutateAsync({
        productoId: selectedPendiente.productoId,
        productoCanonicoId,
      })
      setMessage({
        variant: "success",
        title: "Canonizacion aprobada",
        body: `Se asocio ${result.sku} con ${result.productoCanonicoNombre}.`,
      })
      await pendientesQuery.refetch()
    } catch (error) {
      const body = error instanceof Error && error.message ? error.message : "No se pudo asociar el producto."
      setMessage({
        variant: "error",
        title: "Error al asociar",
        body,
      })
    }
  }

  const handleCrear = async () => {
    if (!selectedPendiente) return
    setMessage(null)
    try {
      const result = await crearMutation.mutateAsync({
        productoId: selectedPendiente.productoId,
        payload: form,
      })
      setMessage({
        variant: "success",
        title: "Canonico creado",
        body: `Se creo ${result.productoCanonicoNombre} y se asocio al producto ${result.sku}.`,
      })
      await pendientesQuery.refetch()
    } catch (error) {
      const body = error instanceof Error && error.message ? error.message : "No se pudo crear el producto canonico."
      setMessage({
        variant: "error",
        title: "Error de canonizacion",
        body,
      })
    }
  }

  const handleDesasociar = async (productoId: string) => {
    setMessage(null)
    try {
      const result = await desasociarMutation.mutateAsync(productoId)
      setMessage({
        variant: "success",
        title: "Canonizacion revertida",
        body: `Se desasocio ${result.sku} de ${result.productoCanonicoNombre}.`,
      })
      setRecentlyRevertedIds((current) => [result.productoId, ...current.filter((id) => id !== result.productoId)].slice(0, 20))
      await pendientesQuery.refetch()
      await asignadosQuery.refetch()
    } catch (error) {
      const body = error instanceof Error && error.message ? error.message : "No se pudo desasociar el producto."
      setMessage({
        variant: "error",
        title: "Error al revertir",
        body,
      })
    }
  }

  const isWorking = asociarMutation.isPending || crearMutation.isPending || desasociarMutation.isPending

  return (
    <section className="space-y-6">
      <header className="rounded-[2rem] border border-slate-200 bg-gradient-to-br from-white via-slate-50 to-blue-50 p-6 shadow-sm md:p-8">
        <p className="text-xs font-semibold uppercase tracking-[0.35em] text-slate-400">Admin MD</p>
        <h2 className="mt-3 text-3xl font-semibold tracking-tight text-slate-950">Canonizacion</h2>
        <p className="mt-3 max-w-4xl text-sm leading-6 text-slate-600">
          Bandeja real para aprobar productos proveedor pendientes, asociar con un canonico existente o crear un canonico
          nuevo cuando no haya coincidencia.
        </p>
      </header>

      {message ? (
        <FeedbackAlert variant={message.variant} title={message.title}>
          {message.body}
        </FeedbackAlert>
      ) : null}

      <div className="grid gap-4 md:grid-cols-3">
        <KpiCard
          label="Pendientes"
          value={`${pendientesFiltrados.length}/${pendientes.length}`}
          detail="Mostrados / totales sin canonico."
        />
        <KpiCard
          label="Candidatos visibles"
          value={String(candidatos.length)}
          detail={candidateSearch.trim() ? "Filtrados por busqueda." : "Top de canonicos activos."}
        />
        <KpiCard label="Seleccionado" value={selectedPendiente ? selectedPendiente.sku : "-"} detail="SKU sobre el que se decide." />
      </div>

      <div className="grid gap-6 xl:grid-cols-[1.1fr_1fr_1fr]">
        <section className="rounded-[2rem] border border-slate-200 bg-white p-6 shadow-sm">
          <h3 className="text-xl font-semibold text-slate-950">Pendientes proveedor</h3>
          <p className="mt-1 text-sm text-slate-600">Selecciona un producto para aprobar canonizacion.</p>
          <div className="mt-4 grid gap-2 sm:grid-cols-2">
            <input
              value={pendingSearch}
              onChange={(event) => setPendingSearch(event.target.value)}
              className="rounded-xl border border-slate-300 px-3 py-2 text-sm outline-none transition focus:border-slate-500 sm:col-span-2"
              placeholder="Buscar por nombre, SKU o proveedor"
            />
            <select
              value={proveedorFilter}
              onChange={(event) => setProveedorFilter(event.target.value)}
              className="rounded-xl border border-slate-300 px-3 py-2 text-sm outline-none transition focus:border-slate-500"
            >
              <option value="ALL">Todos los proveedores</option>
              {proveedoresDisponibles.map((proveedor) => (
                <option key={proveedor} value={proveedor}>
                  {proveedor}
                </option>
              ))}
            </select>
            <select
              value={categoriaFilter}
              onChange={(event) => setCategoriaFilter(event.target.value)}
              className="rounded-xl border border-slate-300 px-3 py-2 text-sm outline-none transition focus:border-slate-500"
            >
              <option value="ALL">Todas las categorías</option>
              {categoriasDisponibles.map((categoria) => (
                <option key={categoria} value={categoria}>
                  {categoria}
                </option>
              ))}
            </select>
            <select
              value={stockFilter}
              onChange={(event) => setStockFilter(event.target.value as "ALL" | "CON_STOCK" | "SIN_STOCK")}
              className="rounded-xl border border-slate-300 px-3 py-2 text-sm outline-none transition focus:border-slate-500"
            >
              <option value="ALL">Stock: todos</option>
              <option value="CON_STOCK">Con stock</option>
              <option value="SIN_STOCK">Sin stock</option>
            </select>
            <button
              type="button"
              className="rounded-xl border border-slate-300 px-3 py-2 text-sm font-medium text-slate-700 transition hover:border-slate-400 hover:bg-slate-50"
              onClick={() => {
                setPendingSearch("")
                setProveedorFilter("ALL")
                setCategoriaFilter("ALL")
                setStockFilter("ALL")
              }}
            >
              Limpiar filtros
            </button>
          </div>
          <div className="mt-4 max-h-[520px] space-y-3 overflow-auto pr-1">
            {pendientesQuery.isLoading ? (
              Array.from({ length: 6 }).map((_, index) => (
                <div key={index} className="h-20 animate-pulse rounded-2xl border border-slate-200 bg-slate-100" />
              ))
            ) : null}

            {!pendientesQuery.isLoading && pendientesFiltrados.length === 0 ? (
              <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4 text-sm text-slate-600">
                No hay pendientes para los filtros seleccionados.
              </div>
            ) : null}

            {pendientesFiltrados.map((item) => {
              const selected = item.productoId === selectedProductoId
              const recentlyReverted = recentlyRevertedIds.includes(item.productoId)
              return (
                <button
                  key={item.productoId}
                  type="button"
                  onClick={() => setSelectedProductoId(item.productoId)}
                  className={`w-full rounded-2xl border px-4 py-4 text-left transition ${
                    selected
                      ? "border-blue-300 bg-blue-50"
                      : "border-slate-200 bg-white hover:border-slate-300 hover:bg-slate-50"
                  }`}
                >
                  <div className="flex flex-wrap items-start justify-between gap-2">
                    <p className="text-sm font-semibold text-slate-900">{item.nombre}</p>
                    {recentlyReverted ? (
                      <span className="rounded-full border border-amber-200 bg-amber-50 px-2 py-0.5 text-[11px] font-semibold uppercase tracking-[0.16em] text-amber-700">
                        Revertido recientemente
                      </span>
                    ) : null}
                  </div>
                  <p className="mt-1 text-xs uppercase tracking-[0.16em] text-slate-500">{item.sku}</p>
                  <p className="mt-2 text-xs text-slate-600">
                    {item.proveedorNombre} - {item.categoriaPaquete}
                  </p>
                </button>
              )
            })}
          </div>
        </section>

        <section className="rounded-[2rem] border border-slate-200 bg-white p-6 shadow-sm">
          <h3 className="text-xl font-semibold text-slate-950">Aprobar con canonico existente</h3>
          <article className="mt-4 rounded-2xl border border-slate-200 bg-slate-50 p-4">
            <div className="flex flex-wrap items-center justify-between gap-2">
              <p className="text-sm font-semibold text-slate-900">Canonizados recientes</p>
              <span className="rounded-full bg-white px-2 py-1 text-xs font-semibold text-slate-600">
                {asignados.length} visibles
              </span>
            </div>
            <input
              value={asignadoSearch}
              onChange={(event) => setAsignadoSearch(event.target.value)}
              className="mt-3 w-full rounded-xl border border-slate-300 px-3 py-2 text-sm outline-none transition focus:border-slate-500"
              placeholder="Buscar por SKU, proveedor o canonico"
            />
            <div className="mt-3 max-h-[220px] space-y-2 overflow-auto pr-1">
              {asignadosQuery.isLoading ? (
                Array.from({ length: 3 }).map((_, index) => (
                  <div key={index} className="h-16 animate-pulse rounded-xl border border-slate-200 bg-slate-100" />
                ))
              ) : null}

              {!asignadosQuery.isLoading && asignados.length === 0 ? (
                <div className="rounded-xl border border-slate-200 bg-white p-3 text-xs text-slate-600">
                  No hay productos canonizados para este filtro.
                </div>
              ) : null}

              {asignados.map((item) => (
                <article key={item.productoId} className="rounded-xl border border-slate-200 bg-white p-3">
                  <p className="text-sm font-semibold text-slate-900">{item.nombreProductoProveedor}</p>
                  <p className="mt-1 text-xs text-slate-600">
                    {item.sku} - {item.proveedorNombre}
                  </p>
                  <p className="mt-1 text-xs text-slate-500">Canonico: {item.productoCanonicoNombre}</p>
                  <button
                    type="button"
                    className="mt-2 rounded-full border border-red-300 px-3 py-1.5 text-xs font-semibold text-red-700 transition hover:border-red-400 hover:bg-red-50 disabled:opacity-60"
                    disabled={isWorking}
                    onClick={() => {
                      const ok = window.confirm(
                        `Desasociar ${item.sku} de ${item.productoCanonicoNombre}? Volvera a pendientes de canonizacion.`,
                      )
                      if (ok) {
                        void handleDesasociar(item.productoId)
                      }
                    }}
                  >
                    Desasociar
                  </button>
                </article>
              ))}
            </div>
          </article>

          {selectedPendiente ? (
            <>
              <article className="mt-4 rounded-2xl border border-slate-200 bg-slate-50 p-4">
                <p className="text-xs uppercase tracking-[0.16em] text-slate-500">{selectedPendiente.sku}</p>
                <p className="mt-1 text-lg font-semibold text-slate-900">{selectedPendiente.nombre}</p>
                <p className="mt-2 text-sm text-slate-700">{formatMoney(selectedPendiente.precioCosto, selectedPendiente.moneda)}</p>
                <p className="mt-1 text-sm text-slate-600">Stock: {selectedPendiente.stockDisponible}</p>
                <p className="mt-1 text-xs text-slate-500">Actualizado: {formatDate(selectedPendiente.updatedAt)}</p>
              </article>

              <label className="mt-4 block space-y-2">
                <span className="text-sm font-medium text-slate-700">Buscar canonico</span>
                <input
                  value={candidateSearch}
                  onChange={(event) => setCandidateSearch(event.target.value)}
                  className="w-full rounded-xl border border-slate-300 px-3 py-2 text-sm outline-none transition focus:border-slate-500"
                  placeholder="Marca, modelo o nombre publico"
                />
              </label>

              <div className="mt-4 max-h-[300px] space-y-2 overflow-auto pr-1">
                {candidatosQuery.isLoading ? (
                  Array.from({ length: 5 }).map((_, index) => (
                    <div key={index} className="h-16 animate-pulse rounded-xl border border-slate-200 bg-slate-100" />
                  ))
                ) : null}

                {!candidatosQuery.isLoading && candidatos.length === 0 ? (
                  <div className="rounded-xl border border-slate-200 bg-slate-50 p-4 text-sm text-slate-600">
                    No hay canonicos para ese filtro.
                  </div>
                ) : null}

                {candidatos.map((item) => (
                  <article key={item.productoCanonicoId} className="rounded-xl border border-slate-200 bg-white p-3">
                    <p className="text-sm font-semibold text-slate-900">{item.nombrePublico}</p>
                    <p className="mt-1 text-xs text-slate-600">
                      {item.marca} - {item.modelo}
                    </p>
                    <button
                      type="button"
                      className="mt-3 rounded-full bg-slate-950 px-3 py-1.5 text-xs font-semibold text-white transition hover:bg-slate-800 disabled:opacity-60"
                      disabled={isWorking}
                      onClick={() => void handleAsociar(item.productoCanonicoId)}
                    >
                      Asociar
                    </button>
                  </article>
                ))}
              </div>
            </>
          ) : (
            <div className="mt-4 rounded-2xl border border-slate-200 bg-slate-50 p-4 text-sm text-slate-600">
              Selecciona un pendiente para asociarlo.
            </div>
          )}
        </section>

        <section className="rounded-[2rem] border border-slate-200 bg-white p-6 shadow-sm">
          <h3 className="text-xl font-semibold text-slate-950">Crear canonico nuevo</h3>
          <p className="mt-1 text-sm text-slate-600">Si no existe coincidencia, crea y asocia en el mismo paso.</p>

          <div className="mt-4 space-y-3">
            <label className="space-y-1">
              <span className="text-sm font-medium text-slate-700">Marca</span>
              <input
                value={form.marca}
                onChange={(event) => setForm((current) => ({ ...current, marca: event.target.value }))}
                className="w-full rounded-xl border border-slate-300 px-3 py-2 text-sm outline-none transition focus:border-slate-500"
                placeholder="Marca"
                disabled={!selectedPendiente || isWorking}
              />
            </label>
            <label className="space-y-1">
              <span className="text-sm font-medium text-slate-700">Modelo</span>
              <input
                value={form.modelo}
                onChange={(event) => setForm((current) => ({ ...current, modelo: event.target.value }))}
                className="w-full rounded-xl border border-slate-300 px-3 py-2 text-sm outline-none transition focus:border-slate-500"
                placeholder="Modelo"
                disabled={!selectedPendiente || isWorking}
              />
            </label>
            <label className="space-y-1">
              <span className="text-sm font-medium text-slate-700">Nombre publico</span>
              <input
                value={form.nombrePublico}
                onChange={(event) => setForm((current) => ({ ...current, nombrePublico: event.target.value }))}
                className="w-full rounded-xl border border-slate-300 px-3 py-2 text-sm outline-none transition focus:border-slate-500"
                placeholder="Nombre visible en catalogo"
                disabled={!selectedPendiente || isWorking}
              />
            </label>
            <label className="space-y-1">
              <span className="text-sm font-medium text-slate-700">Atributos JSON (opcional)</span>
              <textarea
                value={form.atributosJson}
                onChange={(event) => setForm((current) => ({ ...current, atributosJson: event.target.value }))}
                className="min-h-24 w-full rounded-xl border border-slate-300 px-3 py-2 text-sm outline-none transition focus:border-slate-500"
                placeholder='{"color":"negro","memoria":"256GB"}'
                disabled={!selectedPendiente || isWorking}
              />
            </label>
          </div>

          <button
            type="button"
            className="mt-4 w-full rounded-xl bg-blue-600 px-4 py-3 text-sm font-semibold text-white transition hover:bg-blue-700 disabled:opacity-60"
            disabled={!selectedPendiente || isWorking}
            onClick={() => void handleCrear()}
          >
            Crear y asociar
          </button>
        </section>
      </div>
    </section>
  )
}

function KpiCard({ label, value, detail }: { label: string; value: string; detail: string }) {
  return (
    <article className="rounded-[1.5rem] border border-slate-200 bg-white p-4 shadow-sm">
      <p className="text-[11px] font-semibold uppercase tracking-[0.22em] text-slate-400">{label}</p>
      <p className="mt-3 text-3xl font-semibold tracking-tight text-slate-950">{value}</p>
      <p className="mt-1 text-sm text-slate-600">{detail}</p>
    </article>
  )
}
