import { useEffect, useMemo, useRef, useState, type FormEvent, type ReactNode } from "react"
import { useSearchParams } from "react-router-dom"
import ProveedorMetricCard from "../../components/proveedor/ProveedorMetricCard"
import ProveedorModuleHeader from "../../components/proveedor/ProveedorModuleHeader"
import FeedbackAlert from "../../components/ui/FeedbackAlert"
import { useProveedorContext } from "../../hooks/useProveedorContext"
import {
  useActualizarProveedorProducto,
  useProveedorCategoriasProducto,
  useCrearProveedorProducto,
  useCrearProveedorProductoMedia,
  useDesactivarProveedorProducto,
  useEliminarProveedorProductoMedia,
  useProveedorProductoMedia,
  useProveedorProductos,
  useSugerirProveedorCategoriaProducto,
} from "../../hooks/useProveedorProductos"
import StorefrontProductImage from "../../storefront/components/StorefrontProductImage"
import type {
  ProveedorProductoMediaRequest,
  ProveedorProductoResponse,
  ProveedorProductoUpsertRequest,
} from "../../types/proveedor"

const inputClassName =
  "mt-2 w-full rounded-2xl border border-slate-300 bg-white px-4 py-3 text-sm text-slate-900 outline-none transition placeholder:text-slate-400 focus:border-slate-400 focus:ring-2 focus:ring-slate-200"

const emptyProducto: ProveedorProductoUpsertRequest = {
  sku: "",
  nombre: "",
  descripcion: "",
  precioCosto: 0,
  stock: 0,
  categoriaPaquete: "MEDIANO",
  imagenes: [],
  activo: true,
}

const emptyMedia: ProveedorProductoMediaRequest = {
  tipoMedia: "IMAGEN",
  url: "",
  orden: 0,
  esPrincipal: false,
}

type FeedbackState = { variant: "success" | "error"; message: string }
type FiltroProductos = "todos" | "activos" | "inactivos" | "sin-stock" | "sin-media" | "pendientes"

export default function ProveedorProductosPage() {
  const { proveedorId } = useProveedorContext()
  const [searchParams, setSearchParams] = useSearchParams()
  const [feedback, setFeedback] = useState<FeedbackState | null>(null)
  const [query, setQuery] = useState("")
  const [filtro, setFiltro] = useState<FiltroProductos>(resolveInitialFiltro(searchParams.get("filtro")))
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const [productoForm, setProductoForm] = useState<ProveedorProductoUpsertRequest>(emptyProducto)
  const [imagenesAlta, setImagenesAlta] = useState<string[]>(["", "", ""])
  const [categoriaSugerida, setCategoriaSugerida] = useState("")
  const [categoriaTemporal, setCategoriaTemporal] = useState<{ id: string; nombre: string } | null>(null)
  const [mediaForm, setMediaForm] = useState<ProveedorProductoMediaRequest>(emptyMedia)
  const editorRef = useRef<HTMLDivElement | null>(null)

  const productosQuery = useProveedorProductos(proveedorId)
  const categoriasProductoQuery = useProveedorCategoriasProducto(proveedorId)
  const sugerirCategoriaProducto = useSugerirProveedorCategoriaProducto(proveedorId)
  const crearProducto = useCrearProveedorProducto(proveedorId)
  const actualizarProducto = useActualizarProveedorProducto(proveedorId, selectedId)
  const desactivarProducto = useDesactivarProveedorProducto(proveedorId)
  const mediaQuery = useProveedorProductoMedia(proveedorId, selectedId)
  const crearMedia = useCrearProveedorProductoMedia(proveedorId, selectedId)
  const eliminarMedia = useEliminarProveedorProductoMedia(proveedorId, selectedId)

  const productos = productosQuery.data ?? []
  const productoSeleccionado = selectedId ? productos.find((item) => item.id === selectedId) ?? null : null
  const productosFiltrados = useMemo(() => filtrarProductos(productos, query, filtro), [productos, query, filtro])

  useEffect(() => {
    setProductoForm(productoSeleccionado ? toProductoForm(productoSeleccionado) : emptyProducto)
    if (productoSeleccionado) {
      setImagenesAlta(["", "", ""])
      setCategoriaTemporal(null)
    }
  }, [productoSeleccionado])

  const activos = productos.filter((item) => item.activo).length
  const inactivos = productos.filter((item) => !item.activo).length
  const pendientes = productos.filter((item) => item.estadoNormalizacion === "PENDIENTE").length
  const sinStock = productos.filter((item) => item.stockDisponible <= 0).length
  const sinMedia = productos.filter((item) => item.cantidadMedia <= 0).length

  function onChangeFiltro(nextFiltro: FiltroProductos) {
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

  function focusEditor() {
    window.requestAnimationFrame(() => {
      editorRef.current?.scrollIntoView({ behavior: "smooth", block: "start" })
    })
  }

  async function onSubmitProducto(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setFeedback(null)
    try {
      const payload = normalizeProductoPayload(productoForm)
      if (productoSeleccionado) {
        const producto = await actualizarProducto.mutateAsync(payload)
        setFeedback({ variant: "success", message: `Producto ${producto.nombre} actualizado.` })
      } else {
        const categoriaDisponible = (categoriasProductoQuery.data ?? []).length > 0
        if (categoriaDisponible && !payload.categoriaProductoId) {
          setFeedback({ variant: "error", message: "Selecciona una categoria comercial o sugiere una nueva antes de crear el producto." })
          return
        }
        const producto = await crearProducto.mutateAsync({
          ...payload,
          imagenes: normalizeImagenesAlta(imagenesAlta),
        })
        setSelectedId(producto.id)
        setImagenesAlta(["", "", ""])
        setCategoriaSugerida("")
        setCategoriaTemporal(null)
        setFeedback({ variant: "success", message: `Producto ${producto.nombre} creado correctamente.` })
      }
    } catch {
      setFeedback({ variant: "error", message: "No se pudo guardar el producto. Reintenta." })
    }
  }

  async function onSugerirCategoriaProducto() {
    const nombre = categoriaSugerida.trim()
    if (!nombre) {
      setFeedback({ variant: "error", message: "Escribe el nombre de la categoria para sugerir." })
      return
    }

    setFeedback(null)
    try {
      const response = await sugerirCategoriaProducto.mutateAsync({ nombre })
      setCategoriaTemporal({ id: response.categoriaId, nombre: response.nombre })
      setProductoForm((current) => ({ ...current, categoriaProductoId: response.categoriaId }))
      setCategoriaSugerida("")
      setFeedback({ variant: "success", message: response.mensaje })
    } catch {
      setFeedback({ variant: "error", message: "No se pudo sugerir la categoria. Reintenta." })
    }
  }

  async function onDesactivarProducto() {
    if (!productoSeleccionado) return
    const confirmar = window.confirm(`Desactivar ${productoSeleccionado.nombre}? El producto quedara oculto como oferta activa, pero seguira disponible para historial y reactivacion.`)
    if (!confirmar) return

    setFeedback(null)
    try {
      const producto = await desactivarProducto.mutateAsync(productoSeleccionado.id)
      setProductoForm(toProductoForm(producto))
      setFeedback({ variant: "success", message: `Producto ${producto.nombre} desactivado.` })
    } catch {
      setFeedback({ variant: "error", message: "No se pudo desactivar el producto. Reintenta." })
    }
  }

  async function onReactivarProducto() {
    if (!productoSeleccionado) return
    setFeedback(null)
    try {
      const producto = await actualizarProducto.mutateAsync({
        ...normalizeProductoPayload(productoForm),
        activo: true,
      })
      setProductoForm(toProductoForm(producto))
      setFeedback({ variant: "success", message: `Producto ${producto.nombre} reactivado.` })
    } catch {
      setFeedback({ variant: "error", message: "No se pudo reactivar el producto. Reintenta." })
    }
  }

  async function onSubmitMedia(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!selectedId) {
      setFeedback({ variant: "error", message: "Primero crea o selecciona un producto para gestionar su media." })
      return
    }
    setFeedback(null)
    try {
      await crearMedia.mutateAsync(normalizeMediaPayload(mediaForm))
      setMediaForm(emptyMedia)
      setFeedback({ variant: "success", message: "Media agregada correctamente al producto." })
    } catch {
      setFeedback({ variant: "error", message: "No se pudo agregar la media. Reintenta." })
    }
  }

  async function onDeleteMedia(mediaId: string) {
    setFeedback(null)
    try {
      await eliminarMedia.mutateAsync(mediaId)
      setFeedback({ variant: "success", message: "Media eliminada correctamente." })
    } catch {
      setFeedback({ variant: "error", message: "No se pudo eliminar la media seleccionada." })
    }
  }

  return (
    <section className="space-y-6">
      <ProveedorModuleHeader
        eyebrow="Proveedor"
        title="Productos y ofertas"
        description="Administra productos, precios, stock e imagenes desde una vista simple para carga y mantenimiento diario."
        proveedorId={proveedorId}
        aside={
          <div className="flex flex-wrap gap-3">
            <button
              type="button"
              className="rounded-full border border-slate-300 px-4 py-2 text-sm font-semibold text-slate-700 transition hover:border-slate-400 hover:text-slate-900"
              onClick={() => {
                setSelectedId(null)
                setProductoForm(emptyProducto)
                setImagenesAlta(["", "", ""])
                setCategoriaSugerida("")
                setCategoriaTemporal(null)
                setMediaForm(emptyMedia)
                setFeedback(null)
                focusEditor()
              }}
            >
              Nuevo producto
            </button>
            <button
              type="button"
              className="rounded-full bg-slate-950 px-4 py-2 text-sm font-semibold text-white transition hover:bg-slate-800"
              onClick={() => void productosQuery.refetch()}
            >
              Actualizar listado
            </button>
          </div>
        }
      />

      {!proveedorId ? (
        <FeedbackAlert variant="warning" title="Proveedor no resuelto">
          Falta `proveedorId` para consultar y operar los productos del proveedor autenticado.
        </FeedbackAlert>
      ) : null}

      {feedback ? <FeedbackAlert variant={feedback.variant}>{feedback.message}</FeedbackAlert> : null}

      {proveedorId && productosQuery.isLoading ? (
        <div className="grid gap-4 md:grid-cols-3">
          {Array.from({ length: 3 }).map((_, index) => (
            <div key={index} className="h-36 animate-pulse rounded-3xl border border-slate-200 bg-slate-200" />
          ))}
        </div>
      ) : null}

      {proveedorId && productosQuery.isError ? (
        <FeedbackAlert variant="error" title="No se pudo cargar el catalogo del proveedor">
          <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
            <p>Reintenta para recuperar el listado real de productos y ofertas.</p>
            <button
              type="button"
              className="rounded-full bg-red-700 px-4 py-2 text-sm font-semibold text-white hover:bg-red-800"
              onClick={() => void productosQuery.refetch()}
            >
              Reintentar
            </button>
          </div>
        </FeedbackAlert>
      ) : null}

      {proveedorId && !productosQuery.isLoading && !productosQuery.isError ? (
        <>
          <div className="grid gap-4 md:grid-cols-3">
            <ProveedorMetricCard label="Productos activos" value={String(activos)} helper="Ofertas disponibles para operar." tone="blue" to="/proveedor/productos?filtro=activos" />
            <ProveedorMetricCard label="Sin stock" value={String(sinStock)} helper="Productos que requieren reposicion o ajuste." tone="amber" to="/proveedor/productos?filtro=sin-stock" />
            <ProveedorMetricCard label="Sin media" value={String(sinMedia)} helper="Productos para completar visualmente." tone="emerald" to="/proveedor/productos?filtro=sin-media" />
          </div>

          <section className="space-y-6">
            <section className="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm">
              <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
                <div>
                  <p className="text-xs font-semibold uppercase tracking-[0.22em] text-slate-500">Listado real</p>
                  <h3 className="mt-2 text-2xl font-semibold text-slate-950">Productos del proveedor</h3>
                </div>
                <label className="block w-full max-w-xl">
                  <span className="text-xs font-semibold uppercase tracking-[0.22em] text-slate-500">Busqueda</span>
                  <input
                    type="search"
                    value={query}
                    onChange={(event) => setQuery(event.target.value)}
                    placeholder="Buscar por nombre, SKU o producto canonico"
                    className={inputClassName}
                  />
                </label>
              </div>

              <div className="mt-4 flex flex-wrap gap-2">
                <Tag tone="slate">{productosFiltrados.length} visibles</Tag>
                <Tag tone="blue">{activos} activos</Tag>
                <Tag tone="red">{inactivos} inactivos</Tag>
                <Tag tone="amber">{pendientes} pendientes</Tag>
                <Tag tone="amber">{sinMedia} sin media</Tag>
                <Tag tone="amber">{sinStock} sin stock</Tag>
              </div>

              <div className="mt-4 flex flex-wrap gap-2">
                {filterOptions.map((option) => (
                  <button
                    key={option.value}
                    type="button"
                    className={`rounded-full border px-3 py-1.5 text-xs font-semibold transition ${
                      filtro === option.value
                        ? "border-slate-900 bg-slate-900 text-white"
                        : "border-slate-300 bg-white text-slate-700 hover:border-slate-400 hover:text-slate-950"
                    }`}
                    onClick={() => onChangeFiltro(option.value)}
                  >
                    {option.label}
                  </button>
                ))}
              </div>

              {productos.length === 0 ? (
                <EmptyState>Todavia no hay productos cargados para este proveedor. Usa el formulario lateral para crear el primero.</EmptyState>
              ) : productosFiltrados.length === 0 ? (
                <EmptyState>No hay resultados para la busqueda actual. Ajusta el texto para volver a explorar el listado.</EmptyState>
              ) : (
                <div className="mt-5 space-y-3">
                  {productosFiltrados.map((producto) => {
                    const isSelected = producto.id === selectedId
                    return (
                      <button
                        key={producto.id}
                        type="button"
                        onClick={() => {
                          setSelectedId(producto.id)
                          setFeedback(null)
                          focusEditor()
                        }}
                        className={`w-full rounded-3xl border p-4 text-left shadow-sm transition ${
                          isSelected ? "border-blue-400 bg-blue-50/70 ring-2 ring-blue-100" : "border-slate-200 bg-white hover:border-slate-300 hover:bg-slate-50"
                        }`}
                      >
                        <div className="grid gap-4 md:grid-cols-[104px_minmax(0,1fr)]">
                          <div className="overflow-hidden rounded-2xl border border-slate-200/70 bg-slate-100">
                            <StorefrontProductImage
                              src={producto.imagenPrincipalUrl}
                              alt={producto.nombre}
                              className="h-24 w-full object-cover"
                              fallbackClassName="flex h-24 w-full items-center justify-center bg-[linear-gradient(135deg,#e2e8f0_0%,#f8fafc_100%)] text-xs font-semibold text-slate-500"
                            />
                          </div>
                          <div className="space-y-3">
                            <div className="flex flex-wrap items-start justify-between gap-3">
                              <div>
                                <p className="text-lg font-semibold text-slate-950">{producto.nombre}</p>
                                <p className="text-sm text-slate-500">SKU {producto.sku}</p>
                              </div>
                              <div className="flex flex-wrap gap-2">
                                <Tag tone="slate">{producto.estadoNormalizacion}</Tag>
                                <Tag tone={producto.activo ? "emerald" : "red"}>{producto.activo ? "Activo" : "Inactivo"}</Tag>
                              </div>
                            </div>
                            <div className="grid gap-3 text-sm text-slate-600 md:grid-cols-2 xl:grid-cols-4">
                              <ItemMeta label="Canonico" value={producto.productoCanonicoNombre || "Pendiente"} />
                              <ItemMeta label="Categoria" value={producto.categoriaProductoNombre || "Sin categoria"} />
                              <ItemMeta label="Costo" value={formatCurrency(producto.precioCosto, producto.moneda)} />
                              <ItemMeta label="Stock" value={`${producto.stockDisponible} unidades`} />
                              <ItemMeta
                                label="Media"
                                value={`${producto.cantidadMedia} archivo${producto.cantidadMedia === 1 ? "" : "s"}${producto.tieneVideo ? " + video" : ""}`}
                              />
                            </div>
                          </div>
                        </div>
                      </button>
                    )
                  })}
                </div>
              )}
            </section>

            <div ref={editorRef} className="scroll-mt-24 space-y-6">
              <section className="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm">
                <div className="flex flex-col gap-3 lg:flex-row lg:items-start lg:justify-between">
                  <div>
                    <p className="text-xs font-semibold uppercase tracking-[0.22em] text-slate-500">{productoSeleccionado ? "Edicion" : "Alta manual"}</p>
                    <h3 className="mt-2 text-2xl font-semibold text-slate-950">{productoSeleccionado ? "Editar producto" : "Crear producto"}</h3>
                    <p className="mt-2 max-w-3xl text-sm leading-6 text-slate-600">
                      Actualiza los datos comerciales del proveedor. La asociacion canonica queda a cargo de Mercado Digital.
                    </p>
                  </div>
                  {productoSeleccionado ? (
                    <button
                      type="button"
                      className="rounded-full border border-slate-300 px-4 py-2 text-sm font-semibold text-slate-700 transition hover:border-slate-400 hover:text-slate-950"
                      onClick={() => {
                        setSelectedId(null)
                        setProductoForm(emptyProducto)
                        setImagenesAlta(["", "", ""])
                        setCategoriaSugerida("")
                        setCategoriaTemporal(null)
                        setMediaForm(emptyMedia)
                        setFeedback(null)
                      }}
                    >
                      Nuevo producto
                    </button>
                  ) : null}
                </div>
                <form className="mt-5 space-y-4" onSubmit={onSubmitProducto}>
                  <div className="grid gap-4 md:grid-cols-2">
                    <Field label="SKU">
                      <input type="text" value={productoForm.sku} onChange={(event) => setProductoForm((current) => ({ ...current, sku: event.target.value }))} className={inputClassName} required />
                    </Field>
                    <Field label="Categoria paquete">
                      <select value={productoForm.categoriaPaquete} onChange={(event) => setProductoForm((current) => ({ ...current, categoriaPaquete: event.target.value }))} className={inputClassName}>
                        <option value="CHICO">CHICO</option>
                        <option value="MEDIANO">MEDIANO</option>
                        <option value="GRANDE">GRANDE</option>
                      </select>
                    </Field>
                  </div>
                  <Field label="Categoria comercial">
                    <select
                      value={productoForm.categoriaProductoId ?? ""}
                      onChange={(event) => setProductoForm((current) => ({ ...current, categoriaProductoId: event.target.value || null }))}
                      className={inputClassName}
                    >
                      <option value="">Sin categoria asignada</option>
                      {(categoriasProductoQuery.data ?? []).map((categoria) => (
                        <option key={categoria.id} value={categoria.id}>
                          {categoria.nombre}
                        </option>
                      ))}
                      {categoriaTemporal && !(categoriasProductoQuery.data ?? []).some((item) => item.id === categoriaTemporal.id) ? (
                        <option value={categoriaTemporal.id}>
                          {categoriaTemporal.nombre} (pendiente de activacion)
                        </option>
                      ) : null}
                    </select>
                  </Field>
                  {!productoSeleccionado ? (
                    <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
                      <p className="text-xs font-semibold uppercase tracking-[0.22em] text-slate-500">Sugerir categoria</p>
                      <p className="mt-2 text-sm leading-6 text-slate-600">
                        Si no encuentras la categoria comercial, puedes sugerirla desde aqui sin salir del alta.
                      </p>
                      <div className="mt-3 flex flex-col gap-3 sm:flex-row">
                        <input
                          type="text"
                          value={categoriaSugerida}
                          onChange={(event) => setCategoriaSugerida(event.target.value)}
                          className="w-full rounded-2xl border border-slate-300 bg-white px-4 py-3 text-sm text-slate-900 outline-none transition placeholder:text-slate-400 focus:border-slate-400 focus:ring-2 focus:ring-slate-200"
                          placeholder="Ejemplo: Electrohogar"
                        />
                        <button
                          type="button"
                          className="rounded-full border border-slate-300 px-5 py-3 text-sm font-semibold text-slate-700 transition hover:border-slate-400 hover:text-slate-900 disabled:cursor-not-allowed disabled:opacity-60"
                          onClick={() => void onSugerirCategoriaProducto()}
                          disabled={sugerirCategoriaProducto.isPending}
                        >
                          {sugerirCategoriaProducto.isPending ? "Sugiriendo..." : "Sugerir categoria"}
                        </button>
                      </div>
                    </div>
                  ) : null}
                  <Field label="Nombre">
                    <input type="text" value={productoForm.nombre} onChange={(event) => setProductoForm((current) => ({ ...current, nombre: event.target.value }))} className={inputClassName} required />
                  </Field>
                  <Field label="Descripcion">
                    <textarea value={productoForm.descripcion} onChange={(event) => setProductoForm((current) => ({ ...current, descripcion: event.target.value }))} className={`${inputClassName} min-h-28 resize-y`} />
                  </Field>
                  <div className="grid gap-4 md:grid-cols-2">
                    <Field label="Precio costo">
                      <input type="number" min="0" step="0.01" value={productoForm.precioCosto} onChange={(event) => setProductoForm((current) => ({ ...current, precioCosto: Number(event.target.value || 0) }))} className={inputClassName} required />
                    </Field>
                    <Field label="Stock disponible">
                      <input type="number" min="0" step="1" value={productoForm.stock} onChange={(event) => setProductoForm((current) => ({ ...current, stock: Number(event.target.value || 0) }))} className={inputClassName} required />
                    </Field>
                  </div>
                  {!productoSeleccionado ? (
                    <div className="space-y-3 rounded-2xl border border-slate-200 bg-slate-50 p-4">
                      <div>
                        <p className="text-xs font-semibold uppercase tracking-[0.22em] text-slate-500">Imagenes opcionales</p>
                        <p className="mt-2 text-sm leading-6 text-slate-600">
                          Carga hasta 3 URLs de imagen para que el producto salga publicado con media inicial en el mismo alta.
                        </p>
                      </div>
                      <div className="grid gap-3">
                        {imagenesAlta.map((imagen, index) => (
                          <Field key={index} label={`URL imagen ${index + 1}`}>
                            <input
                              type="url"
                              value={imagen}
                              onChange={(event) =>
                                setImagenesAlta((current) =>
                                  current.map((item, itemIndex) => (itemIndex === index ? event.target.value : item)),
                                )
                              }
                              className={inputClassName}
                              placeholder="https://..."
                            />
                          </Field>
                        ))}
                      </div>
                    </div>
                  ) : null}
                  <label className="flex items-center gap-3 rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm font-medium text-slate-700">
                    <input type="checkbox" checked={productoForm.activo} onChange={(event) => setProductoForm((current) => ({ ...current, activo: event.target.checked }))} className="h-4 w-4 rounded border-slate-300 text-slate-900 focus:ring-slate-400" />
                    Producto activo
                  </label>
                  {productoSeleccionado ? (
                    <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4 text-sm leading-6 text-slate-600">
                      <p><span className="font-semibold text-slate-900">Estado de normalizacion:</span> {productoSeleccionado.estadoNormalizacion}</p>
                      <p><span className="font-semibold text-slate-900">Producto canonico:</span> {productoSeleccionado.productoCanonicoNombre || "Pendiente de asociacion"}</p>
                    </div>
                  ) : null}
                  <div className="flex flex-wrap gap-3">
                    <button type="submit" className="rounded-full bg-slate-950 px-5 py-3 text-sm font-semibold text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:bg-slate-400" disabled={crearProducto.isPending || actualizarProducto.isPending}>
                      {crearProducto.isPending || actualizarProducto.isPending ? "Guardando..." : productoSeleccionado ? "Guardar cambios" : "Crear producto"}
                    </button>
                    {productoSeleccionado ? (
                      <>
                        <button type="button" className="rounded-full border border-slate-300 px-5 py-3 text-sm font-semibold text-slate-700 transition hover:border-slate-400 hover:text-slate-900" onClick={() => {
                          setSelectedId(null)
                          setProductoForm(emptyProducto)
                          setImagenesAlta(["", "", ""])
                          setCategoriaSugerida("")
                          setCategoriaTemporal(null)
                          setMediaForm(emptyMedia)
                          setFeedback(null)
                        }}>
                          Crear otro
                        </button>
                        {productoSeleccionado.activo ? (
                          <button
                            type="button"
                            className="rounded-full border border-red-300 px-5 py-3 text-sm font-semibold text-red-700 transition hover:border-red-400 hover:bg-red-50 disabled:cursor-not-allowed disabled:opacity-60"
                            onClick={() => void onDesactivarProducto()}
                            disabled={desactivarProducto.isPending}
                          >
                            {desactivarProducto.isPending ? "Desactivando..." : "Desactivar"}
                          </button>
                        ) : (
                          <button
                            type="button"
                            className="rounded-full border border-emerald-300 px-5 py-3 text-sm font-semibold text-emerald-700 transition hover:border-emerald-400 hover:bg-emerald-50 disabled:cursor-not-allowed disabled:opacity-60"
                            onClick={() => void onReactivarProducto()}
                            disabled={actualizarProducto.isPending}
                          >
                            {actualizarProducto.isPending ? "Reactivando..." : "Reactivar"}
                          </button>
                        )}
                      </>
                    ) : null}
                  </div>
                </form>
              </section>

              <section className="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm">
                <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                  <div>
                    <p className="text-xs font-semibold uppercase tracking-[0.22em] text-slate-500">Media</p>
                    <h3 className="mt-2 text-2xl font-semibold text-slate-950">Imagenes y video</h3>
                    <p className="mt-2 text-sm leading-6 text-slate-600">
                      La carga de media es opcional y puede existir aunque el producto siga pendiente de normalizacion.
                    </p>
                  </div>
                  {productoSeleccionado ? <Tag tone="slate">{productoSeleccionado.nombre}</Tag> : null}
                </div>
                {!productoSeleccionado ? (
                  <EmptyState>Crea o selecciona un producto para ver, agregar o eliminar su media.</EmptyState>
                ) : (
                  <div className="mt-5 space-y-5">
                    <div className="overflow-hidden rounded-3xl border border-slate-200 bg-slate-50">
                      <StorefrontProductImage src={productoSeleccionado.imagenPrincipalUrl} alt={productoSeleccionado.nombre} className="h-52 w-full object-cover" />
                    </div>
                    <div className="flex flex-wrap gap-2">
                      <Tag tone="slate">{productoSeleccionado.cantidadMedia} archivo{productoSeleccionado.cantidadMedia === 1 ? "" : "s"}</Tag>
                      <Tag tone="blue">Principal {productoSeleccionado.imagenPrincipalUrl ? "disponible" : "pendiente"}</Tag>
                      <Tag tone="emerald">{productoSeleccionado.tieneVideo ? "Con video" : "Sin video"}</Tag>
                    </div>
                    {mediaQuery.isLoading ? (
                      <div className="space-y-2">
                        {Array.from({ length: 2 }).map((_, index) => (
                          <div key={index} className="h-16 animate-pulse rounded-2xl border border-slate-200 bg-slate-200" />
                        ))}
                      </div>
                    ) : mediaQuery.isError ? (
                      <FeedbackAlert variant="error" title="No se pudo cargar la media">
                        Reintenta para recuperar las imagenes y videos asociados a este producto.
                      </FeedbackAlert>
                    ) : (
                      <div className="space-y-3">
                        {(mediaQuery.data ?? []).length > 0 ? (
                          (mediaQuery.data ?? []).map((media) => (
                            <div key={media.id} className="flex flex-col gap-4 rounded-2xl border border-slate-200 bg-slate-50 p-4 sm:flex-row sm:items-start sm:justify-between">
                              <div className="min-w-0 space-y-2">
                                <div className="flex flex-wrap gap-2">
                                  <Tag tone="slate">{media.tipoMedia}</Tag>
                                  {media.esPrincipal ? <Tag tone="blue">Principal</Tag> : null}
                                  <Tag tone="slate">Orden {media.orden}</Tag>
                                </div>
                                <p className="break-all text-sm leading-6 text-slate-600">{media.url}</p>
                              </div>
                              <button type="button" className="rounded-full border border-red-300 px-4 py-2 text-sm font-semibold text-red-700 transition hover:border-red-400 hover:bg-red-50 disabled:cursor-not-allowed disabled:opacity-60" onClick={() => void onDeleteMedia(media.id)} disabled={eliminarMedia.isPending}>
                                {eliminarMedia.isPending ? "Eliminando..." : "Eliminar"}
                              </button>
                            </div>
                          ))
                        ) : (
                          <EmptyState>Este producto todavia no tiene media cargada.</EmptyState>
                        )}
                      </div>
                    )}
                    <form className="space-y-4 rounded-3xl border border-slate-200 bg-white p-4" onSubmit={onSubmitMedia}>
                      <div className="grid gap-4 md:grid-cols-2">
                        <Field label="Tipo de media">
                          <select value={mediaForm.tipoMedia} onChange={(event) => setMediaForm((current) => ({ ...current, tipoMedia: event.target.value as ProveedorProductoMediaRequest["tipoMedia"] }))} className={inputClassName}>
                            <option value="IMAGEN">IMAGEN</option>
                            <option value="VIDEO">VIDEO</option>
                          </select>
                        </Field>
                        <Field label="Orden">
                          <input type="number" min="0" step="1" value={mediaForm.orden ?? 0} onChange={(event) => setMediaForm((current) => ({ ...current, orden: Number(event.target.value || 0) }))} className={inputClassName} />
                        </Field>
                      </div>
                      <Field label="URL">
                        <input type="url" value={mediaForm.url} onChange={(event) => setMediaForm((current) => ({ ...current, url: event.target.value }))} className={inputClassName} placeholder="https://..." required />
                      </Field>
                      <label className="flex items-center gap-3 rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm font-medium text-slate-700">
                        <input type="checkbox" checked={Boolean(mediaForm.esPrincipal)} onChange={(event) => setMediaForm((current) => ({ ...current, esPrincipal: event.target.checked }))} className="h-4 w-4 rounded border-slate-300 text-slate-900 focus:ring-slate-400" />
                        Marcar como principal
                      </label>
                      <button type="submit" className="rounded-full bg-slate-950 px-5 py-3 text-sm font-semibold text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:bg-slate-400" disabled={crearMedia.isPending}>
                        {crearMedia.isPending ? "Agregando..." : "Agregar media"}
                      </button>
                    </form>
                  </div>
                )}
              </section>
            </div>
          </section>
        </>
      ) : null}
    </section>
  )
}

const filterOptions: { value: FiltroProductos; label: string }[] = [
  { value: "todos", label: "Todos" },
  { value: "activos", label: "Activos" },
  { value: "inactivos", label: "Inactivos" },
  { value: "sin-stock", label: "Sin stock" },
  { value: "sin-media", label: "Sin media" },
  { value: "pendientes", label: "Pendientes MD" },
]

function filtrarProductos(productos: ProveedorProductoResponse[], query: string, filtro: FiltroProductos) {
  const normalizedQuery = normalizeText(query)
  return productos.filter((producto) => {
    const matchesQuery =
      !normalizedQuery ||
      [producto.nombre, producto.sku, producto.productoCanonicoNombre || ""].some((value) =>
        normalizeText(value).includes(normalizedQuery),
      )

    const matchesFiltro =
      filtro === "todos" ? true :
      filtro === "activos" ? producto.activo :
      filtro === "inactivos" ? !producto.activo :
      filtro === "sin-stock" ? producto.stockDisponible <= 0 :
      filtro === "sin-media" ? producto.cantidadMedia <= 0 :
      producto.estadoNormalizacion === "PENDIENTE"

    return matchesQuery && matchesFiltro
  })
}

function resolveInitialFiltro(value: string | null): FiltroProductos {
  return filterOptions.some((option) => option.value === value) ? (value as FiltroProductos) : "todos"
}

function normalizeText(value: string) {
  return value.normalize("NFD").replace(/[\u0300-\u036f]/g, "").toLowerCase().trim()
}

function toProductoForm(producto: ProveedorProductoResponse): ProveedorProductoUpsertRequest {
  return {
    sku: producto.sku,
    nombre: producto.nombre,
    descripcion: producto.descripcion || "",
    precioCosto: producto.precioCosto,
    stock: producto.stockDisponible,
    categoriaPaquete: producto.categoriaPaquete,
    categoriaProductoId: producto.categoriaProductoId,
    activo: producto.activo,
  }
}

function normalizeProductoPayload(form: ProveedorProductoUpsertRequest): ProveedorProductoUpsertRequest {
  return {
    sku: form.sku.trim(),
    nombre: form.nombre.trim(),
    descripcion: form.descripcion.trim(),
    precioCosto: Number(form.precioCosto),
    stock: Number(form.stock),
    categoriaPaquete: form.categoriaPaquete,
    categoriaProductoId: form.categoriaProductoId ?? null,
    imagenes: form.imagenes ?? [],
    activo: form.activo,
  }
}

function normalizeImagenesAlta(imagenes: string[]) {
  return imagenes.map((item) => item.trim()).filter(Boolean).slice(0, 3)
}

function normalizeMediaPayload(form: ProveedorProductoMediaRequest): ProveedorProductoMediaRequest {
  return {
    tipoMedia: form.tipoMedia,
    url: form.url.trim(),
    orden: form.orden ?? 0,
    esPrincipal: Boolean(form.esPrincipal),
  }
}

function formatCurrency(value: number, currency: string) {
  try {
    return new Intl.NumberFormat("es-AR", { style: "currency", currency, maximumFractionDigits: 2 }).format(value)
  } catch {
    return `${currency} ${value.toFixed(2)}`
  }
}

function Field({ label, children }: { label: string; children: ReactNode }) {
  return (
    <label className="block">
      <span className="text-xs font-semibold uppercase tracking-[0.22em] text-slate-500">{label}</span>
      {children}
    </label>
  )
}

function EmptyState({ children }: { children: ReactNode }) {
  return <div className="mt-5 rounded-2xl border border-dashed border-slate-300 bg-slate-50 p-6 text-sm leading-6 text-slate-600">{children}</div>
}

function ItemMeta({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <p className="text-[11px] font-semibold uppercase tracking-[0.18em] opacity-70">{label}</p>
      <p className="mt-1 text-sm font-medium">{value}</p>
    </div>
  )
}

function Tag({
  children,
  tone,
}: {
  children: ReactNode
  tone: "slate" | "blue" | "amber" | "emerald" | "red" | "dark"
}) {
  const className =
    tone === "blue"
      ? "bg-blue-50 text-blue-700"
      : tone === "amber"
        ? "bg-amber-50 text-amber-700"
        : tone === "emerald"
          ? "bg-emerald-50 text-emerald-700"
          : tone === "red"
            ? "bg-red-50 text-red-700"
            : tone === "dark"
              ? "bg-white/10 text-white"
              : "bg-slate-100 text-slate-700"

  return <span className={`rounded-full px-3 py-1 text-xs font-semibold ${className}`}>{children}</span>
}
