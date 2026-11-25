import { getStorefrontCatalogo } from "../api/storefrontCatalogoApi"
import { formatPrecioArs } from "../utils/catalogoPublico"
/* eslint-disable @typescript-eslint/no-explicit-any */
import { useMutation, useQuery, useQueryClient, type UseQueryResult } from "@tanstack/react-query"
import { useEffect, useRef, useState, type FormEvent, type ReactNode } from "react"
import { Navigate, Link, useSearchParams } from "react-router-dom"
import { useTenant } from "../../tenant/useTenant"
import StorefrontBrandCapsule from "../components/StorefrontBrandCapsule"
import StorefrontAdminProductImport from "../components/StorefrontAdminProductImport"
import { useStorefrontAuth } from "../auth/useStorefrontAuth"
import { applyStorefrontDocumentBranding } from "../utils/storefrontBranding"
import {
  cancelarPedidoImpago,
  actualizarAsesor,
  actualizarImagen,
  actualizarProducto,
  agregarImagen,
  crearAsesor,
  crearProducto,
  getStorefrontMetrics,
  getStorefrontContent,
  updateStorefrontContent,
  uploadStorefrontImage,
  probarCorreoStorefront,
  listarPedidos,
  confirmarTransferencia,
  abrirRotuloPedido,
  despacharPedido,
  entregarPedido,
  eliminarImagen,
  abrirComprobante,
  listarAsesores,
  listarCategorias,
  listarDisponibilidad,
  listarPreguntas,
  listarProductos,
  responderDisponibilidad,
  responderPregunta,
  type ProductoPropio,
  type ProductoAnterior,
  esProductoAnterior,
  type ProductoMedia,
  type ProductoPropioInput,
  type CategoriaProducto,
  type PreguntaProducto,
  type SolicitudDisponibilidad,
  type Asesor,
  type StorefrontContent,
  type OwnerOrder,
  type StorefrontMetrics,
} from "../api/storefrontOwnerApi"

import StorefrontBulkPrices from "../components/StorefrontBulkPrices"
import CatalogoAdminSection from "../components/CatalogoAdminSection"
import CatalogoAdvisorContact from "../components/CatalogoAdvisorContact"
import CatalogoTextReview from "../components/CatalogoTextReview"
import { catalogoError, confirmCatalogoLeave, useCatalogoDraft } from "../hooks/useCatalogoDraft"

type Tab = "pedidos" | "metricas" | "productos" | "precios" | "consultas" | "disponibilidad" | "red" | "cobros" | "cuenta" | "catalogo"
const tabs: { id: Tab; label: string }[] = [
  { id: "catalogo", label: "Preparar catálogo" },
  { id: "pedidos", label: "Pedidos" }, { id: "productos", label: "Productos" }, { id: "precios", label: "Precios" },
  { id: "consultas", label: "Consultas" }, { id: "disponibilidad", label: "Disponibilidad" },
  { id: "red", label: "Redes sociales" }, { id: "cobros", label: "Cobros" }, { id: "cuenta", label: "Mi pagina" },
  { id: "metricas", label: "Métricas · Premium" },
]
const emptyProduct: ProductoPropioInput = {
  sku: "", nombre: "", descripcion: "", precioVenta: 0, stock: 0,
  categoriaPaquete: "MEDIANO", categoriaProductoId: null, activo: true, promocion: false, novedad: false,
}
const field = "w-full rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm outline-none focus:border-blue-400 focus:ring-2 focus:ring-blue-100"
const money = new Intl.NumberFormat("es-AR", { style: "currency", currency: "ARS" })

function shareMediaVersion(product: ProductoPropio) {
  const source = `og2|${product.imagenPrincipalUrl ?? ""}|${product.media.map((media) => media.url).join("|")}`
  let hash = 2166136261
  for (let index = 0; index < source.length; index += 1) {
    hash ^= source.charCodeAt(index)
    hash = Math.imul(hash, 16777619)
  }
  return `og2-${(hash >>> 0).toString(36)}`
}

function StateBox({ children, tone = "neutral" }: { children: ReactNode; tone?: "neutral" | "error" | "success" }) {
  const styles = tone === "error" ? "border-red-200 bg-red-50 text-red-700" : tone === "success" ? "border-emerald-200 bg-emerald-50 text-emerald-700" : "border-slate-200 bg-white text-slate-500"
  return <div className={`rounded-2xl border border-dashed p-6 text-center text-sm ${styles}`}>{children}</div>
}

export default function LegacyStorefrontOwnerPage() {
  const tenant = useTenant()
  const auth = useStorefrontAuth()
  const queryClient = useQueryClient()
  const [params, setParams] = useSearchParams()
  const requestedTab = tabs.some(item => item.id === params.get("tab")) ? params.get("tab") as Tab : "productos"
  const tab = tenant.storefront?.modoCatalogo && requestedTab === "precios" ? "catalogo" : requestedTab
  const setTab = (value: Tab) => { if (value !== tab && !confirmCatalogoLeave()) return; setParams(value === "productos" ? {} : { tab: value }) }
  const focusedOrderId = params.get("pedido")
  const [message, setMessage] = useState<string | null>(null)
  const id = auth.session?.revendedorId ?? ""
  const isAdmin = Boolean(auth.session?.roles.includes("ADMIN_MD"))
  const allowed = Boolean(tenant.status === "resolved" && id && (auth.session?.roles.includes("REVENDEDOR") || isAdmin) && id === tenant.revendedorId)

  useEffect(() => {
    if (tenant.branding) applyStorefrontDocumentBranding(tenant.branding)
  }, [tenant.branding])

  const modoCatalogo = Boolean(tenant.storefront?.modoCatalogo)
  const catalogoPublico = useQuery({ queryKey: ["storefront-catalogo", { host: tenant.host }], queryFn: () => getStorefrontCatalogo({ tipo: "REVENDEDOR", revendedorId: id, host: tenant.host }), enabled: allowed && modoCatalogo && tab === "red", staleTime: 0, refetchInterval: 60_000 })
  const products = useQuery({ queryKey: ["owner-products", id], queryFn: () => listarProductos(id), enabled: allowed })
  const productosParaCompartir = modoCatalogo ? (catalogoPublico.isError ? [] : catalogoPublico.data ?? []).flatMap(publico => { const propio = products.data?.find(p => p.id === publico.productoId); return propio && publico.precioPublicoArs ? [{ ...propio, precioPublicoArs: publico.precioPublicoArs }] : [] }) : (products.data ?? []).filter(esProductoAnterior)
  const orders = useQuery({ queryKey: ["owner-orders", id], queryFn: () => listarPedidos(id), enabled: allowed && tab === "pedidos", refetchInterval: 30_000 })
  const categories = useQuery({ queryKey: ["owner-categories", id], queryFn: () => listarCategorias(id), enabled: allowed })
  const questions = useQuery({ queryKey: ["owner-questions", id], queryFn: () => listarPreguntas(id), enabled: allowed && tab === "consultas" })
  const availability = useQuery({ queryKey: ["owner-availability", id], queryFn: () => listarDisponibilidad(id), enabled: allowed && tab === "disponibilidad" })
  const advisors = useQuery({ queryKey: ["owner-advisors", id], queryFn: () => listarAsesores(id), enabled: allowed && tab === "red" })
  const content = useQuery({ queryKey: ["owner-storefront-content", id], queryFn: () => getStorefrontContent(id), enabled: allowed && tab === "cuenta" })

  if (tenant.isLoading || tenant.status === "idle" || tenant.status === "loading") return <p role="status" className="p-6">Cargando panel de la tienda...</p>
  if (tenant.status !== "resolved" || !tenant.storefront) return <StateBox tone="error">No se pudo resolver la tienda para abrir el panel. <button type="button" className="font-semibold underline" onClick={() => void tenant.refresh()}>Reintentar</button></StateBox>
  if (!auth.session) return <Navigate to="/ingresar" replace />
  if (!allowed) return <Navigate to="/" replace />

  const refreshProducts = async () => Promise.all([
    queryClient.invalidateQueries({ queryKey: ["owner-products", id] }),
    queryClient.invalidateQueries({ queryKey: ["catalogo-productos", id] }),
    queryClient.invalidateQueries({ queryKey: ["storefront-catalogo"] }),
    queryClient.invalidateQueries({ queryKey: ["storefront-producto-detalle"] }),
  ])
  const notify = (text: string) => { setMessage(text); window.setTimeout(() => setMessage(null), 3500) }

  return (
    <div className="min-h-screen bg-slate-50 text-slate-950">
      <header className="border-b border-slate-200 bg-white shadow-sm">
        <div className="mx-auto flex max-w-[1500px] items-center justify-between gap-4 px-4 py-4 sm:px-7">
          <Link to="/"><StorefrontBrandCapsule branding={tenant.branding} isReady accentColor={tenant.branding?.colorPrimario || "#0f172a"} /></Link>
          <div className="hidden flex-1 sm:block">
            <p className="text-xs font-semibold uppercase tracking-[.18em] text-slate-400">Panel propietario</p>
            <p className="font-semibold">{tenant.branding?.nombreComercial}</p>
          </div>
          <button onClick={() => { if (!confirmCatalogoLeave()) return; auth.logout(); window.location.assign("/") }} className="rounded-xl border border-slate-200 px-4 py-2 text-sm text-slate-600 hover:border-slate-400">Cerrar sesión</button>
        </div>
        <nav className="mx-auto flex max-w-[1500px] items-center overflow-x-auto px-3 sm:px-7">
          {tabs.filter(item => !modoCatalogo || item.id !== "precios").map((item) => {
            const premium = item.id === "metricas"
            return <button key={item.id} onClick={() => setTab(item.id)} className={`flex shrink-0 items-center gap-1.5 text-sm font-semibold transition ${premium
              ? `my-1.5 ml-2 rounded-full border px-4 py-2 ${tab === item.id ? "border-orange-500 bg-gradient-to-r from-orange-500 to-amber-500 text-white shadow-md shadow-orange-200" : "border-orange-200 bg-orange-50 text-orange-700 hover:border-orange-300 hover:bg-orange-100"}`
              : `border-b-2 px-4 py-3 ${tab === item.id ? "border-slate-950 text-slate-950" : "border-transparent text-slate-500 hover:text-slate-900"}`
            }`}>{premium ? <PremiumIcon className="h-4 w-4" /> : null}{modoCatalogo ? ({ catalogo: "Configurar catálogo", pedidos: "Pedidos anteriores", cobros: "Pagos", disponibilidad: "Consultas anteriores", red: "Contactos y difusión" } as Partial<Record<Tab, string>>)[item.id] ?? item.label : item.label}</button>
          })}
        </nav>
      </header>

      <main className="mx-auto max-w-[1500px] space-y-4 px-4 py-6 sm:px-7">
        {isAdmin ? <div className="flex items-center justify-between gap-3 rounded-2xl border border-violet-200 bg-violet-50 px-4 py-3 text-sm text-violet-800"><span><strong>Modo administrador MD.</strong> Estas operando la tienda {tenant.branding?.nombreComercial}.</span><span className="rounded-full bg-violet-700 px-3 py-1 text-xs font-bold uppercase tracking-wider text-white">Acceso privado</span></div> : null}
        {modoCatalogo && <div className="rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm leading-6 text-emerald-950"><strong>Catálogo con atención por WhatsApp.</strong> Las consultas nuevas no generan pedidos ni cobros en la plataforma. Los pedidos y cobros anteriores siguen disponibles para su gestión.</div>}
        {message ? <div className="rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">{message}</div> : null}
        {tab === "pedidos" ? <><h1 className="sr-only">{modoCatalogo ? "Pedidos anteriores" : "Pedidos"}</h1><OrdersSection id={id} query={orders} notify={notify} focusedOrderId={focusedOrderId} /></> : null}
        {tab === "metricas" ? <MetricsSection id={id} /> : null}
        {tab === "catalogo" ? <CatalogoAdminSection id={id} notify={notify} refreshBranding={tenant.refresh} modoCatalogo={modoCatalogo} /> : null}
        {tab === "productos" && modoCatalogo ? <CatalogoAdminSection id={id} notify={notify} refreshBranding={tenant.refresh} modoCatalogo /> : null}
        {tab === "productos" && !modoCatalogo ? <ProductsSection id={id} data={(products.data ?? []).filter(esProductoAnterior)} categories={categories.data ?? []} loading={products.isLoading || categories.isLoading} failed={products.isError || categories.isError} refresh={refreshProducts} notify={notify} isAdmin={isAdmin} modoCatalogo={modoCatalogo} onConfigurePrice={() => setTab("catalogo")} /> : null}
        {tab === "precios" ? modoCatalogo ? <CatalogoAdminSection id={id} notify={notify} refreshBranding={tenant.refresh} modoCatalogo /> : <PricesSection id={id} products={(products.data ?? []).filter(esProductoAnterior)} refresh={refreshProducts} notify={notify} /> : null}
        {tab === "consultas" ? <QuestionsSection id={id} query={questions} notify={notify} /> : null}
        {tab === "disponibilidad" ? <AvailabilitySection id={id} query={availability} notify={notify} /> : null}
        {tab === "red" && modoCatalogo && catalogoPublico.isPending && <StateBox>Cargando precios públicos para compartir...</StateBox>}
        {tab === "red" && modoCatalogo && catalogoPublico.isError && <StateBox tone="error">No se pudieron actualizar los precios para compartir. <button type="button" className="underline" onClick={() => void catalogoPublico.refetch()}>Reintentar</button></StateBox>}
        {tab === "red" ? <AdvisorsSection id={id} query={advisors} storefrontHost={tenant.storefront?.dominio ?? tenant.host ?? ""} products={productosParaCompartir} categories={categories.data ?? []} storeName={tenant.branding?.nombreComercial ?? "Mi tienda"} slogan={tenant.branding?.heroSubtitulo || tenant.branding?.footerTexto || "Calidad, confianza y atención personalizada."} notify={notify} /> : null}
        {tab === "cobros" ? <PaymentsSection /> : null}
        {tab === "cuenta" ? content.isLoading ? <StateBox>Cargando configuracion...</StateBox> : content.isError || !content.data ? <StateBox tone="error">No se pudo cargar la configuracion de la pagina.</StateBox> : <StorefrontContentSection modoCatalogo={modoCatalogo} id={id} initial={content.data} name={auth.session.nombre} email={auth.session.email} refresh={async () => { await queryClient.invalidateQueries({ queryKey: ["owner-storefront-content", id] }); await tenant.refresh() }} notify={notify} /> : null}
      </main>
    </div>
  )
}

type ProductsSectionProps = {
  id: string
  data: ProductoAnterior[]
  categories: CategoriaProducto[]
  loading: boolean
  failed: boolean
  refresh: () => Promise<unknown>
  notify: (message: string) => void
  isAdmin: boolean
  modoCatalogo: boolean
  onConfigurePrice: () => void
}

function ProductsSection({ id, data, categories, loading, failed, refresh, notify, isAdmin, modoCatalogo, onConfigurePrice }: ProductsSectionProps) {
  const editorRef = useRef<HTMLFormElement>(null)
  const [editing, setEditing] = useState<ProductoAnterior | null>(null)
  const [form, setForm] = useState<ProductoPropioInput>(emptyProduct)
  const [legacyPriceEntered, setLegacyPriceEntered] = useState(false)
  const [imageUrls, setImageUrls] = useState([""])
  const [imageFiles, setImageFiles] = useState<File[]>([])
  const [productSearch, setProductSearch] = useState("")
  const [categoryFilter, setCategoryFilter] = useState("")
  const [statusFilter, setStatusFilter] = useState<"todos" | "activos" | "inactivos">("todos")
  const normalizedSearch = productSearch.trim().toLocaleLowerCase("es")
  const filteredProducts = data.filter((product) => {
    const matchesSearch = !normalizedSearch || product.nombre.toLocaleLowerCase("es").includes(normalizedSearch) || product.sku.toLocaleLowerCase("es").includes(normalizedSearch)
    const matchesCategory = !categoryFilter || (categoryFilter === "sin-categoria" ? !product.categoriaProductoId : product.categoriaProductoId === categoryFilter)
    const matchesStatus = statusFilter === "todos" || (statusFilter === "activos" ? product.activo : !product.activo)
    return matchesSearch && matchesCategory && matchesStatus
  })
  const filtersApplied = Boolean(productSearch || categoryFilter || statusFilter !== "todos")
  const editProduct = (product: ProductoAnterior) => {
    setEditing(product)
    setForm({ sku: product.sku, nombre: product.nombre, descripcion: product.descripcion ?? "", precioVenta: product.precioVenta, stock: product.stock, categoriaPaquete: product.categoriaPaquete, categoriaProductoId: product.categoriaProductoId, activo: product.activo, promocion: product.promocion, novedad: product.novedad })
    setImageUrls([""])
    setImageFiles([])
    window.requestAnimationFrame(() => editorRef.current?.scrollTo({ top: 0, behavior: "smooth" }))
  }
  const mutation = useMutation({
    mutationFn: async () => {
      if (modoCatalogo && !editing && !legacyPriceEntered) throw new Error("Completá el precio ARS de compatibilidad. El precio público se configura por separado en Catálogo.")
      const input = modoCatalogo ? { ...form, stock: null } : form
      const saved = editing ? await actualizarProducto(id, editing.id, input) : await crearProducto(id, input)
      const uploaded = await Promise.all(imageFiles.map((file) => uploadStorefrontImage(id, file, "producto")))
      const urls = [...imageUrls.map((url) => url.trim()).filter(Boolean), ...uploaded.map((asset) => asset.url)]
      const currentImageCount = editing?.media.filter((item) => item.tipoMedia === "IMAGEN").length ?? 0
      for (const [index, url] of urls.entries()) {
        await agregarImagen(id, saved.id, url, currentImageCount + index, currentImageCount === 0 && index === 0)
      }
    },
    onSuccess: async () => { await refresh(); setEditing(null); setLegacyPriceEntered(false); setForm(emptyProduct); setImageUrls([""]); setImageFiles([]); notify(modoCatalogo ? "Ficha guardada. Configurá su precio y envío en Catálogo para publicarla." : "Producto guardado correctamente.") },
  })
  const mediaMutation = useMutation({
    mutationFn: async ({ action, media }: { action: "cover" | "delete"; media: ProductoMedia }) => {
      if (!editing) return
      if (action === "cover") await actualizarImagen(id, editing.id, media, true)
      else await eliminarImagen(id, editing.id, media.id)
    },
    onSuccess: async (_, variables) => {
      setEditing((current) => current ? {
        ...current,
        media: variables.action === "delete"
          ? current.media.filter((item) => item.id !== variables.media.id).map((item, index) => ({ ...item, esPrincipal: variables.media.esPrincipal ? index === 0 : item.esPrincipal }))
          : current.media.map((item) => ({ ...item, esPrincipal: item.id === variables.media.id })),
        imagenPrincipalUrl: variables.action === "cover"
          ? variables.media.url
          : variables.media.esPrincipal
            ? current.media.find((item) => item.id !== variables.media.id && item.tipoMedia === "IMAGEN")?.url ?? null
            : current.imagenPrincipalUrl,
      } : current)
      await refresh()
      notify(variables.action === "cover" ? "Portada del producto actualizada." : "Imagen eliminada.")
    },
  })
  const statusMutation = useMutation({
    mutationFn: (product: ProductoAnterior) => actualizarProducto(id, product.id, {
      sku: product.sku,
      nombre: product.nombre,
      descripcion: product.descripcion,
      precioVenta: product.precioVenta,
      stock: product.stock,
      categoriaPaquete: product.categoriaPaquete,
      categoriaProductoId: product.categoriaProductoId,
      activo: !product.activo,
      promocion: product.promocion,
      novedad: product.novedad,
    }),
    onSuccess: async (saved) => {
      await refresh()
      notify(saved.activo ? modoCatalogo ? "Ficha activada. Se publica cuando su precio de catálogo está completo." : "Producto activado y visible en la tienda." : "Producto desactivado y oculto del catálogo.")
    },
  })
  const set = (name: keyof ProductoPropioInput, value: unknown) => setForm((current) => ({ ...current, [name]: value }))
  const setImageUrl = (index: number, value: string) => setImageUrls((current) => {
    const next = [...current]
    next[index] = value
    if (value.trim() && index === current.length - 1 && current.length < 5) next.push("")
    return next
  })
  const submit = (e: FormEvent) => { e.preventDefault(); mutation.mutate() }

  return <section className="space-y-4">
    <div><p className="text-xs font-bold uppercase tracking-[.2em] text-blue-700">Mi tienda</p><h1 className="mt-1 text-2xl font-semibold">Productos propios</h1><p className="text-sm text-slate-500">Tu catálogo autónomo, sin productos de proveedores.</p></div>
    {isAdmin && !modoCatalogo ? <StorefrontAdminProductImport revendedorId={id} products={data} categories={categories} refresh={refresh} notify={notify} /> : null}
    {modoCatalogo && <div className="rounded-2xl border border-blue-200 bg-blue-50 p-4 text-sm leading-6 text-blue-950"><p>1. Guardá la ficha del producto. 2. Configurá su moneda, precio y envío en Catálogo. La ficha activa se publica cuando su configuración está completa.</p><button type="button" onClick={onConfigurePrice} className="mt-2 rounded-xl border border-blue-300 bg-white px-4 py-2 font-semibold">Configurar precios del catálogo</button>{isAdmin && <p className="mt-2 text-xs">El importador anterior no admite estos precios. Usá la carga de fichas y su configuración individual.</p>}</div>}
    {failed ? <StateBox tone="error">No se pudo cargar el catálogo o las categorías.</StateBox> : null}
    {statusMutation.isError ? <StateBox tone="error">No se pudo cambiar el estado del producto.</StateBox> : null}
    <div className="grid gap-5 lg:grid-cols-[1fr_380px]">
      <div className="rounded-2xl border border-slate-200 bg-white p-4">
        <div className="mb-4 rounded-2xl border border-slate-200 bg-slate-50 p-3">
          <div className="grid gap-2 md:grid-cols-[minmax(220px,1fr)_minmax(170px,.65fr)_150px_auto]">
            <label className="relative"><span className="sr-only">Buscar por nombre o SKU</span><svg viewBox="0 0 20 20" fill="none" aria-hidden="true" className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400"><circle cx="8.5" cy="8.5" r="5.5" stroke="currentColor" strokeWidth="1.7" /><path d="m13 13 4 4" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" /></svg><input type="search" className={`${field} pl-9`} value={productSearch} onChange={(event) => setProductSearch(event.target.value)} placeholder="Buscar por nombre o SKU" /></label>
            <select aria-label="Filtrar por categoría" className={field} value={categoryFilter} onChange={(event) => setCategoryFilter(event.target.value)}><option value="">Todas las categorías</option><option value="sin-categoria">Sin categoría</option>{categories.map((category) => <option key={category.id} value={category.id}>{category.nombre}</option>)}</select>
            <select aria-label="Filtrar por estado" className={field} value={statusFilter} onChange={(event) => setStatusFilter(event.target.value as "todos" | "activos" | "inactivos")}><option value="todos">Todos</option><option value="activos">Activos</option><option value="inactivos">Inactivos</option></select>
            {filtersApplied ? <button type="button" onClick={() => { setProductSearch(""); setCategoryFilter(""); setStatusFilter("todos") }} className="rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm font-semibold text-slate-600 hover:border-slate-400">Limpiar</button> : null}
          </div>
          <p className="mt-2 px-1 text-xs text-slate-500">{filteredProducts.length} de {data.length} producto{data.length === 1 ? "" : "s"}</p>
        </div>
        {loading ? <StateBox>Cargando productos...</StateBox> : data.length === 0 ? <StateBox>Todavía no hay productos propios.</StateBox> : filteredProducts.length === 0 ? <StateBox>No encontramos productos con esos filtros.</StateBox> : <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">{filteredProducts.map((product: ProductoAnterior) => <article key={product.id} className="overflow-hidden rounded-2xl border border-slate-200">
          <div className="flex h-36 items-center justify-center bg-slate-50">{product.imagenPrincipalUrl ? <img src={product.imagenPrincipalUrl} className="h-full w-full object-contain" alt="" /> : <span className="text-xs text-slate-400">Sin imagen</span>}</div>
          <div className="space-y-1 p-3"><div className="flex justify-between gap-2"><h3 className="truncate font-semibold">{product.nombre}</h3><span className={`rounded-full px-2 py-0.5 text-[10px] font-bold ${product.activo ? "bg-emerald-50 text-emerald-700" : "bg-slate-100 text-slate-500"}`}>{product.activo ? "ACTIVO" : "INACTIVO"}</span></div><p className="text-xs text-slate-500">SKU {product.sku}{!modoCatalogo && ` · Stock ${product.stock}`}</p><p className="text-xs text-slate-500">{product.categoriaProductoNombre ?? "Sin categoría"}</p><p className="text-sm font-semibold">{modoCatalogo ? <button type="button" className="text-left text-blue-700 underline" onClick={onConfigurePrice}>Configurar precio público</button> : product.precioVenta === null ? "Precio no disponible" : money.format(product.precioVenta)}</p><div className="mt-2 grid grid-cols-2 gap-2"><button onClick={() => editProduct(product)} className="rounded-xl border border-slate-200 py-2 text-xs font-semibold hover:border-slate-400">Editar</button><button disabled={statusMutation.isPending} onClick={() => statusMutation.mutate(product)} className={`rounded-xl border py-2 text-xs font-semibold disabled:opacity-50 ${product.activo ? "border-red-200 text-red-600 hover:bg-red-50" : "border-emerald-200 text-emerald-700 hover:bg-emerald-50"}`}>{product.activo ? "Desactivar" : "Activar"}</button></div></div>
        </article>)}</div>}
      </div>
      <form ref={editorRef} onSubmit={submit} className="h-fit space-y-3 rounded-2xl border border-slate-200 bg-white p-5 lg:sticky lg:top-4 lg:max-h-[calc(100dvh-2rem)] lg:overflow-y-auto lg:overscroll-contain lg:[scrollbar-gutter:stable]">
        <div className="flex items-center justify-between lg:sticky lg:-top-5 lg:z-10 lg:-mx-5 lg:border-b lg:border-slate-100 lg:bg-white lg:px-5 lg:py-4"><h2 className="font-semibold">{editing ? "Editar producto" : "Nuevo producto"}</h2>{editing ? <button type="button" onClick={() => { setEditing(null); setLegacyPriceEntered(false); setForm(emptyProduct); setImageUrls([""]); setImageFiles([]); editorRef.current?.scrollTo({ top: 0, behavior: "smooth" }) }} className="text-xs text-slate-500">Cancelar</button> : null}</div>
        <div className="grid grid-cols-2 gap-2"><label className="text-xs text-slate-600">SKU *<input required className={`${field} mt-1`} value={form.sku} onChange={(e) => set("sku", e.target.value)} /></label><label className="text-xs text-slate-600">Nombre *<input required className={`${field} mt-1`} value={form.nombre} onChange={(e) => set("nombre", e.target.value)} /></label><label className="text-xs text-slate-600">{modoCatalogo ? "Precio anterior ARS (no publica)" : "Precio"}<input required min={0} step="0.01" type="number" className={`${field} mt-1`} value={modoCatalogo && !editing && !legacyPriceEntered ? "" : form.precioVenta} onChange={(e) => { setLegacyPriceEntered(e.target.value !== ""); set("precioVenta", Number(e.target.value)) }} /></label>{!modoCatalogo && <label className="text-xs text-slate-600">Stock<input required min={0} type="number" className={`${field} mt-1`} value={form.stock ?? ""} onChange={(e) => set("stock", Number(e.target.value))} /></label>}</div>
        {modoCatalogo && <p className="text-xs leading-5 text-slate-600">El precio anterior es un dato de compatibilidad y no se publica. No cargues dólares en ese campo: la moneda y el precio público se definen en Catálogo.</p>}
        <label className="block text-xs text-slate-600">Categoría<select className={`${field} mt-1`} value={form.categoriaProductoId ?? ""} onChange={(e) => set("categoriaProductoId", e.target.value || null)}><option value="">— Sin categoría —</option>{categories.map((category) => <option key={category.id} value={category.id}>{category.nombre}</option>)}</select></label>
        {!modoCatalogo && <label className="block text-xs text-slate-600">Tipo de paquete<select className={`${field} mt-1`} value={form.categoriaPaquete} onChange={(e) => set("categoriaPaquete", e.target.value)}><option value="CHICO">Chico</option><option value="MEDIANO">Mediano</option><option value="VOLUMINOSO">Voluminoso</option></select></label>}
        <label className="block text-xs text-slate-600">Descripción<textarea className={`${field} mt-1 resize-none`} rows={3} value={form.descripcion ?? ""} onChange={(e) => set("descripcion", e.target.value)} /></label>
        <div className="space-y-2 rounded-xl border border-slate-200 bg-slate-50 p-3">
          <p className="text-xs font-semibold text-slate-700">Imagenes del producto</p>
          <div className="space-y-2">
            <div className="flex items-center justify-between gap-3"><p className="text-xs text-slate-600">Agregar por URL</p><span className="text-[10px] font-semibold text-slate-400">Hasta 5</span></div>
            {imageUrls.map((url, index) => <label key={index} className="block"><span className="sr-only">URL de imagen {index + 1}</span><div className="relative"><span className="pointer-events-none absolute left-3 top-1/2 flex h-5 w-5 -translate-y-1/2 items-center justify-center rounded-md bg-slate-100 text-[10px] font-bold text-slate-500">{index + 1}</span><input type="url" className={`${field} pl-10`} value={url} onChange={(event) => setImageUrl(index, event.target.value)} placeholder="https://..." /></div></label>)}
            {imageUrls.length < 5 ? <p className="text-[11px] text-slate-500">Al completar una URL se habilita automáticamente la siguiente.</p> : <p className="text-[11px] font-semibold text-emerald-700">Llegaste al máximo de 5 URLs.</p>}
          </div>
          <label className="block cursor-pointer rounded-xl border border-dashed border-slate-300 bg-white px-3 py-3 text-center text-xs font-semibold text-slate-700 hover:border-blue-300"><input type="file" multiple accept="image/png,image/jpeg,image/webp,image/gif" className="hidden" onChange={(event) => { setImageFiles(Array.from(event.target.files ?? [])); event.target.value = "" }} />Seleccionar varias imagenes</label>
          {imageFiles.length > 0 ? <p className="text-xs text-emerald-700">{imageFiles.length} imagen{imageFiles.length === 1 ? "" : "es"} seleccionada{imageFiles.length === 1 ? "" : "s"}</p> : null}
          {editing?.media.some((item) => item.tipoMedia === "IMAGEN") ? <div className="grid grid-cols-2 gap-2">{editing.media.filter((item) => item.tipoMedia === "IMAGEN").map((media) => <article key={media.id} className={`overflow-hidden rounded-xl border bg-white ${media.esPrincipal ? "border-blue-400 ring-2 ring-blue-100" : "border-slate-200"}`}><div className="relative"><img src={media.url} alt="" className="h-24 w-full object-contain" />{media.esPrincipal ? <span className="absolute left-2 top-2 rounded-full bg-blue-600 px-2 py-0.5 text-[9px] font-bold uppercase text-white">Portada</span> : null}</div><div className="flex flex-col gap-1 border-t border-slate-100 p-2">{!media.esPrincipal ? <button type="button" disabled={mediaMutation.isPending} onClick={() => mediaMutation.mutate({ action: "cover", media })} className="text-left text-[11px] font-semibold text-blue-700">Usar como portada</button> : null}<button type="button" disabled={mediaMutation.isPending} onClick={() => mediaMutation.mutate({ action: "delete", media })} className="text-left text-[11px] font-semibold text-red-600">Eliminar</button></div></article>)}</div> : null}
          <p className="text-[11px] leading-4 text-slate-500">La primera imagen de un producto nuevo queda como portada. Luego podes cambiarla desde esta galeria.</p>
        </div>
        <div className="grid grid-cols-3 gap-2 text-xs text-slate-600">{[["activo", "Activo"], ["promocion", "Promoción"], ["novedad", "Novedad"]].map(([key, label]) => <label key={key} className="flex items-center gap-1.5"><input type="checkbox" checked={Boolean(form[key as keyof ProductoPropioInput])} onChange={(e) => set(key as keyof ProductoPropioInput, e.target.checked)} />{label}</label>)}</div>
        {mutation.isError ? <p className="text-xs text-red-600">No se pudo guardar. Revisá SKU y datos obligatorios.</p> : null}
        <div className="lg:sticky lg:-bottom-5 lg:z-10 lg:-mx-5 lg:border-t lg:border-slate-100 lg:bg-white/95 lg:px-5 lg:py-4 lg:backdrop-blur"><button disabled={mutation.isPending} className="w-full rounded-xl bg-slate-950 py-2.5 text-sm font-semibold text-white disabled:opacity-50">{mutation.isPending ? "Guardando..." : "Guardar producto"}</button></div>
      </form>
    </div>
  </section>
}

function PricesSection({ id, products, refresh, notify }: { id: string; products: ProductoAnterior[]; refresh: () => Promise<unknown>; notify: (message: string) => void }) {
  const [draft, setDraft] = useState<Record<string, number>>({})
  const mutation = useMutation({ mutationFn: (product: ProductoAnterior) => actualizarProducto(id, product.id, { sku: product.sku, nombre: product.nombre, descripcion: product.descripcion, precioVenta: draft[product.id] ?? product.precioVenta, stock: product.stock, categoriaPaquete: product.categoriaPaquete, categoriaProductoId: product.categoriaProductoId, activo: product.activo, promocion: product.promocion, novedad: product.novedad }), onSuccess: async () => { await refresh(); notify("Precio actualizado.") } })
  return <section className="space-y-4"><div><h1 className="text-2xl font-semibold">Precios</h1><p className="text-sm text-slate-500">Precios de venta de tus productos propios.</p></div><StorefrontBulkPrices id={id} products={products} refresh={refresh} notify={notify} /><div className="overflow-hidden rounded-2xl border border-slate-200 bg-white"><div className="divide-y divide-slate-100">{products.length === 0 ? <StateBox>No hay productos para configurar.</StateBox> : products.map((product: ProductoAnterior) => <div key={product.id} className="grid items-center gap-3 p-4 sm:grid-cols-[1fr_180px_100px]"><div><p className="font-semibold">{product.nombre}</p><p className="text-xs text-slate-500">{product.sku}</p></div><input aria-label={`Precio de ${product.nombre}`} type="number" min={0} className={field} value={draft[product.id] ?? product.precioVenta} onChange={(e) => setDraft((current) => ({ ...current, [product.id]: Number(e.target.value) }))} /><button disabled={mutation.isPending} onClick={() => mutation.mutate(product)} className="rounded-xl bg-slate-900 px-3 py-2 text-sm font-semibold text-white">Guardar</button></div>)}</div></div></section>
}

function QuestionsSection({ id, query, notify }: { id: string; query: UseQueryResult<PreguntaProducto[]>; notify: (message: string) => void }) {
  const client = useQueryClient(); const [answers, setAnswers] = useState<Record<string, string>>({})
  const mutation = useMutation({ mutationFn: ({ qid, answer }: { qid: string; answer: string }) => responderPregunta(id, qid, answer), onSuccess: async () => { await client.invalidateQueries({ queryKey: ["owner-questions", id] }); notify("Respuesta enviada al cliente.") } })
  if (query.isError) return <StateBox tone="error">No se pudieron cargar las consultas.</StateBox>
  return <section className="space-y-3"><h1 className="text-2xl font-semibold">Consultas de productos</h1>{query.isLoading ? <StateBox>Cargando...</StateBox> : query.data?.length ? query.data.map((q: any) => <article key={q.id} className="space-y-2 rounded-2xl border border-slate-200 bg-white p-4"><div className="flex justify-between"><div><p className="font-semibold">{q.productoNombre}</p><p className="text-xs text-slate-500">{q.nombreContacto ?? "Anónimo"}{q.emailContacto ? ` · ${q.emailContacto}` : ""}</p></div><span className="text-xs font-bold text-slate-500">{q.estado}</span></div><p className="rounded-xl bg-slate-50 p-3 text-sm">{q.pregunta}</p>{q.respuesta ? <p className="rounded-xl bg-emerald-50 p-3 text-sm text-emerald-800">Tu respuesta: {q.respuesta}</p> : <div className="flex gap-2"><input className={field} value={answers[q.id] ?? ""} onChange={(e) => setAnswers((a) => ({ ...a, [q.id]: e.target.value }))} placeholder="Escribí tu respuesta..." /><button disabled={!answers[q.id]?.trim() || mutation.isPending} onClick={() => mutation.mutate({ qid: q.id, answer: answers[q.id] })} className="rounded-xl bg-slate-900 px-4 text-sm font-semibold text-white disabled:opacity-50">Responder</button></div>}</article>) : <StateBox>No hay consultas.</StateBox>}</section>
}

function AvailabilitySection({ id, query, notify }: { id: string; query: UseQueryResult<SolicitudDisponibilidad[]>; notify: (message: string) => void }) {
  const client = useQueryClient(); const [answers, setAnswers] = useState<Record<string, string>>({})
  const mutation = useMutation({ mutationFn: ({ requestId, available }: { requestId: string; available: boolean }) => responderDisponibilidad(id, requestId, available, answers[requestId] ?? ""), onSuccess: async () => { await client.invalidateQueries({ queryKey: ["owner-availability", id] }); notify("Disponibilidad respondida al cliente.") } })
  if (query.isError) return <StateBox tone="error">No se pudieron cargar las solicitudes.</StateBox>
  return <section className="space-y-3"><h1 className="text-2xl font-semibold">Solicitudes de disponibilidad</h1>{query.isLoading ? <StateBox>Cargando...</StateBox> : query.data?.length ? query.data.map((request: any) => <article key={request.id} className="space-y-2 rounded-2xl border border-slate-200 bg-white p-4"><div className="flex justify-between"><div><p className="font-semibold">{request.productoNombre}</p><p className="text-xs text-slate-500">{request.nombreContacto ?? "Anónimo"} · {request.localidad}</p></div><span className="text-xs font-bold text-slate-500">{request.estado}</span></div>{request.observaciones ? <p className="text-sm text-slate-600">{request.observaciones}</p> : null}{request.respuestaProveedor ? <p className="rounded-xl bg-emerald-50 p-3 text-sm text-emerald-800">Tu respuesta: {request.respuestaProveedor}</p> : <><input className={field} value={answers[request.id] ?? ""} onChange={(e) => setAnswers((a) => ({ ...a, [request.id]: e.target.value }))} placeholder="Observación opcional" /><div className="grid grid-cols-2 gap-2"><button onClick={() => mutation.mutate({ requestId: request.id, available: true })} className="rounded-xl bg-emerald-600 py-2 text-sm font-semibold text-white">Disponible</button><button onClick={() => mutation.mutate({ requestId: request.id, available: false })} className="rounded-xl bg-red-500 py-2 text-sm font-semibold text-white">No disponible</button></div></>}</article>) : <StateBox>No hay solicitudes.</StateBox>}</section>
}

type AdvisorsSectionProps = {
  storefrontHost: string
  id: string
  query: UseQueryResult<Asesor[]>
  products: ProductoPropio[]
  categories: CategoriaProducto[]
  storeName: string
  slogan: string
  notify: (message: string) => void
}

function AdvisorsSection({ id, query, products, categories, storeName, slogan, notify, storefrontHost }: AdvisorsSectionProps) {
  const client = useQueryClient()
  const [form, setForm] = useState({ nombre: "", email: "", password: "", whatsappComercial: "" })
  useCatalogoDraft(Object.values(form).some(value => value.length > 0))
  const [selectedProducts, setSelectedProducts] = useState<string[]>([])
  const [selectedAdvisor, setSelectedAdvisor] = useState("")
  const [search, setSearch] = useState("")
  const [categoryFilter, setCategoryFilter] = useState("")
  const create = useMutation({ mutationFn: () => crearAsesor(id, { ...form, email: form.email.trim() || undefined, password: form.password || undefined, activo: true }), onSuccess: async () => { await client.invalidateQueries({ queryKey: ["owner-advisors", id] }); setForm({ nombre: "", email: "", password: "", whatsappComercial: "" }); notify("Asesor creado. El acceso al panel es opcional.") } })
  const toggle = useMutation({ mutationFn: (advisor: Asesor) => actualizarAsesor(id, advisor.id, { nombre: advisor.nombre, email: advisor.email ?? undefined, activo: !advisor.activo }), onSuccess: async () => client.invalidateQueries({ queryKey: ["owner-advisors", id] }) })
  const advisor = query.data?.find((item) => item.id === selectedAdvisor && item.activo)
  const activeProducts = products.filter((product) => product.activo)
  const normalizedSearch = search.trim().toLocaleLowerCase("es")
  const visibleProducts = activeProducts.filter((product) => {
    const matchesSearch = !normalizedSearch || product.nombre.toLocaleLowerCase("es").includes(normalizedSearch) || product.sku.toLocaleLowerCase("es").includes(normalizedSearch)
    const matchesCategory = !categoryFilter || (categoryFilter === "sin-categoria" ? !product.categoriaProductoId : product.categoriaProductoId === categoryFilter)
    return matchesSearch && matchesCategory
  })
  const chosenProducts = activeProducts.filter((product) => selectedProducts.includes(product.id))
  const catalogShareUrl = () => {
    const url = new URL("/catalogo", window.location.origin)
    if (chosenProducts.length) url.searchParams.set("lista", chosenProducts.map(p => p.id).join(","))
    if (storefrontHost) url.searchParams.set("storefrontHost", storefrontHost)
    if (selectedAdvisor) url.searchParams.set("asesorId", selectedAdvisor)
    if (advisor) url.searchParams.set("asesor", advisor.nombre)
    return url.toString()
  }
  const productShareUrl = (product: ProductoPropio) => {
    const url = new URL(`/api/public/share/producto/${product.id}`, window.location.origin)
    url.searchParams.set("storefrontHost", (storefrontHost || window.location.hostname))
    url.searchParams.set("v", shareMediaVersion(product) + (product.precioPublicoArs ? `-${product.precioPublicoArs.replace(".", "-")}` : ""))
    if (advisor) {
      url.searchParams.set("asesorId", advisor.id)
      url.searchParams.set("asesor", advisor.nombre)
    }
    return url.toString()
  }
  const productShareText = (product: ProductoPropio) => [
    product.descripcion?.trim() || product.nombre,
    "",
    `*💰 ${precioCompartido(product)}*`,
    "",
    `✨ *${slogan.trim()}*`,
    "",
    "+ Info:",
    productShareUrl(product),
  ].join("\n")
  const listShareText = () => [
    `✨ ${storeName}`,
    slogan,
    "",
    "Te comparto esta selección:",
    ...chosenProducts.map((product) => `• ${product.nombre} — ${precioCompartido(product)}`),
    "",
    `Total: ${totalCompartido(chosenProducts)}`,
    advisor ? `Te asesora: ${advisor.nombre}` : "",
    "",
    chosenProducts.some(p => p.precioPublicoArs) ? "Abrir la lista y consultar a la tienda:" : "Abrir la lista y comprar online:",
    catalogShareUrl(),
  ].filter(Boolean).join("\n")
  const copyProduct = async (product: ProductoPropio) => {
    await navigator.clipboard.writeText(productShareText(product))
    notify("Mensaje del producto copiado.")
  }
  const toggleProduct = (productId: string) => setSelectedProducts((current) => current.includes(productId) ? current.filter((value) => value !== productId) : [...current, productId])

  return <section className="space-y-5">
    <div><h1 className="text-2xl font-semibold">Redes sociales</h1><p className="text-sm text-slate-500">Prepará productos para compartir y administrá los contactos comerciales de esta tienda.</p></div>
    <div className="rounded-2xl border border-slate-200 bg-white p-5">
      <div><h2 className="font-semibold">Productos para compartir</h2><p className="text-xs text-slate-500">Abrí un artículo para ver la foto y el mensaje listo para enviar.</p></div>
      <div className="mt-4 rounded-2xl border border-slate-200 bg-slate-50 p-3">
        <div className="grid gap-2 md:grid-cols-[minmax(220px,1fr)_minmax(180px,.7fr)_minmax(180px,.7fr)_auto]">
          <label className="relative"><span className="sr-only">Buscar por nombre o SKU</span><svg viewBox="0 0 20 20" fill="none" aria-hidden="true" className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400"><circle cx="8.5" cy="8.5" r="5.5" stroke="currentColor" strokeWidth="1.7" /><path d="m13 13 4 4" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" /></svg><input type="search" className={`${field} pl-9`} value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Buscar por nombre o SKU" /></label>
          <select aria-label="Filtrar por categoría" className={field} value={categoryFilter} onChange={(event) => setCategoryFilter(event.target.value)}><option value="">Todas las categorías</option><option value="sin-categoria">Sin categoría</option>{categories.map((category) => <option key={category.id} value={category.id}>{category.nombre}</option>)}</select>
          <select aria-label="Asesor asignado" className={field} value={selectedAdvisor} onChange={(event) => setSelectedAdvisor(event.target.value)}><option value="">Sin asesor asignado</option>{query.data?.filter((item) => item.activo).map((item) => <option key={item.id} value={item.id}>{item.nombre}</option>)}</select>
          {search || categoryFilter ? <button type="button" onClick={() => { setSearch(""); setCategoryFilter("") }} className="rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm font-semibold text-slate-600 hover:border-slate-400">Limpiar</button> : null}
        </div>
        <p className="mt-2 px-1 text-xs text-slate-500">{visibleProducts.length} de {activeProducts.length} productos activos</p>
      </div>
      <div className="mt-4 space-y-2">{visibleProducts.length ? visibleProducts.map((product) => {
        const message = productShareText(product)
        return <details key={product.id} className="group overflow-hidden rounded-2xl border border-slate-200 bg-slate-50 open:border-blue-200 open:bg-white">
          <summary className="flex cursor-pointer list-none items-center justify-between gap-3 px-4 py-3 marker:content-none"><div className="min-w-0"><p className="truncate text-sm font-semibold text-slate-900">{product.nombre}</p><p className="text-xs text-slate-500">{product.categoriaProductoNombre ?? "Sin categoría"}{product.precioPublicoArs ? "" : ` · Stock ${product.stock}`}</p></div><div className="flex shrink-0 items-center gap-3"><span className="font-bold text-slate-950">{precioCompartido(product)}</span><span className="text-slate-400 transition group-open:rotate-180">▾</span></div></summary>
          <div className="grid gap-4 border-t border-slate-200 p-4 md:grid-cols-[180px_1fr]">
            <div className="flex h-44 items-center justify-center overflow-hidden rounded-2xl border border-slate-200 bg-white">{product.imagenPrincipalUrl ? <img src={product.imagenPrincipalUrl} alt={product.nombre} className="h-full w-full object-contain" /> : <span className="text-xs text-slate-400">Sin imagen</span>}</div>
            <div className="min-w-0"><p className="text-xs font-bold uppercase tracking-wider text-slate-400">Mensaje preparado</p><pre className="mt-2 whitespace-pre-wrap rounded-xl bg-slate-950 p-4 font-sans text-sm leading-6 text-white">{message}</pre><div className="mt-3 flex flex-wrap gap-2"><a href={`https://wa.me/?text=${encodeURIComponent(message)}`} target="_blank" rel="noreferrer" className="rounded-xl bg-emerald-600 px-4 py-2.5 text-sm font-semibold text-white hover:bg-emerald-700">Compartir por WhatsApp</a><button type="button" onClick={() => void copyProduct(product)} className="rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-sm font-semibold text-slate-700 hover:border-slate-400">Copiar mensaje</button><button type="button" onClick={() => toggleProduct(product.id)} className={`rounded-xl border px-4 py-2.5 text-sm font-semibold ${selectedProducts.includes(product.id) ? "border-blue-300 bg-blue-50 text-blue-700" : "border-slate-200 bg-white text-slate-700"}`}>{selectedProducts.includes(product.id) ? "Quitar de la lista" : "Agregar a una lista"}</button></div></div>
          </div>
        </details>
      }) : <StateBox>{activeProducts.length ? "No encontramos productos con ese nombre." : "No hay productos activos para compartir."}</StateBox>}</div>
      {chosenProducts.length ? <div className="mt-5 rounded-2xl border border-blue-200 bg-blue-50 p-4"><div className="flex flex-wrap items-center justify-between gap-3"><div><p className="font-semibold text-blue-950">Lista seleccionada: {chosenProducts.length} producto(s)</p><p className="text-sm text-blue-700">Total {totalCompartido(chosenProducts)}</p></div><a href={`https://wa.me/?text=${encodeURIComponent(listShareText())}`} target="_blank" rel="noreferrer" className="rounded-xl bg-emerald-600 px-4 py-2.5 text-sm font-semibold text-white">Compartir lista por WhatsApp</a></div></div> : null}
    </div>
    <div className="grid gap-5 lg:grid-cols-[1fr_360px]">
      <div className="space-y-3">{query.isError ? <StateBox tone="error">No se pudieron cargar los asesores.</StateBox> : query.data?.length ? query.data.map((advisor: Asesor) => <div key={advisor.id} className="flex items-center justify-between rounded-2xl border border-slate-200 bg-white p-4"><div><p className="font-semibold">{advisor.nombre}</p><p className="text-xs text-slate-500">{advisor.email ?? "Sin acceso configurado"}</p><CatalogoAdvisorContact id={id} advisor={advisor} refresh={() => client.invalidateQueries({ queryKey: ["owner-advisors", id] })} /></div><button onClick={() => toggle.mutate(advisor)} className={`rounded-xl px-3 py-2 text-xs font-bold ${advisor.activo ? "bg-emerald-50 text-emerald-700" : "bg-slate-100 text-slate-500"}`}>{advisor.activo ? "Activo" : "Inactivo"}</button></div>) : <StateBox>No hay asesores.</StateBox>}</div>
      <form onSubmit={(e) => { e.preventDefault(); if (!create.isPending) create.mutate() }} className="h-fit space-y-3 rounded-2xl border border-slate-200 bg-white p-5"><h2 className="font-semibold">Nuevo asesor</h2><input required className={field} placeholder="Nombre" value={form.nombre} onChange={(e) => setForm((f) => ({ ...f, nombre: e.target.value }))} /><label className="block text-xs">WhatsApp comercial<input type="tel" maxLength={40} className={field} placeholder="+54 9 11 1234 5678" value={form.whatsappComercial} onChange={e => setForm(f => ({ ...f, whatsappComercial: e.target.value }))} /></label><p className="text-xs text-slate-500">Email y contraseña son opcionales. Completá ambos únicamente para habilitar acceso al panel.</p><input aria-label="Email de acceso opcional" type="email" className={field} placeholder="Email de acceso" value={form.email} onChange={(e) => setForm((f) => ({ ...f, email: e.target.value }))} /><input aria-label="Contraseña inicial opcional" minLength={6} type="password" className={field} placeholder="Contraseña inicial" value={form.password} onChange={(e) => setForm((f) => ({ ...f, password: e.target.value }))} />{create.isError ? <p className="text-xs text-red-600">{catalogoError(create.error)}</p> : null}<button disabled={create.isPending} className="w-full rounded-xl bg-slate-950 py-2.5 text-sm font-semibold text-white">Crear asesor</button></form>
    </div>
  </section>
}

function MetricsSection({ id }: { id: string }) {
  const [desde, setDesde] = useState(() => formatDateInput(new Date(Date.now() - 29 * 86_400_000)))
  const [hasta, setHasta] = useState(() => formatDateInput(new Date()))
  const validRange = Boolean(desde && hasta && desde <= hasta)
  const query = useQuery({
    queryKey: ["owner-metrics", id, desde, hasta],
    queryFn: () => getStorefrontMetrics(id, desde, hasta),
    enabled: validRange,
  })

  return <section className="space-y-5">
    <div className="relative overflow-hidden rounded-3xl border border-orange-200 bg-gradient-to-br from-orange-50 via-white to-amber-50 p-5 shadow-sm sm:p-7">
      <div className="pointer-events-none absolute -right-16 -top-20 h-56 w-56 rounded-full bg-orange-200/40 blur-3xl" />
      <div className="relative flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
        <div className="flex items-start gap-4">
          <span className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-gradient-to-br from-orange-500 to-amber-500 text-white shadow-lg shadow-orange-200"><PremiumIcon className="h-6 w-6" /></span>
          <div><div className="flex flex-wrap items-center gap-2"><p className="text-xs font-extrabold uppercase tracking-[.2em] text-orange-700">Métricas · Premium</p><span className="rounded-full border border-orange-200 bg-white/80 px-2.5 py-1 text-[10px] font-bold uppercase tracking-wider text-orange-700">Incluido sin cargo</span></div><h1 className="mt-2 text-2xl font-bold tracking-tight sm:text-3xl">Rendimiento de tu tienda</h1><p className="mt-1 text-sm text-slate-600">Entendé qué atrae a tus clientes y cuáles son sus productos favoritos.</p></div>
        </div>
        <div className="grid grid-cols-2 gap-2 rounded-2xl border border-white/80 bg-white/85 p-3 shadow-sm backdrop-blur"><label className="text-xs font-semibold text-slate-500">Desde<input type="date" className={`${field} mt-1`} value={desde} max={hasta} onChange={(event) => setDesde(event.target.value)} /></label><label className="text-xs font-semibold text-slate-500">Hasta<input type="date" className={`${field} mt-1`} value={hasta} min={desde} max={formatDateInput(new Date())} onChange={(event) => setHasta(event.target.value)} /></label></div>
      </div>
    </div>
    {!validRange ? <StateBox tone="error">Elegí un rango de fechas válido.</StateBox> : query.isLoading ? <StateBox>Cargando métricas...</StateBox> : query.isError || !query.data ? <StateBox tone="error">No se pudieron cargar las métricas de este período.</StateBox> : <MetricsDashboard data={query.data} />}
  </section>
}

function MetricsDashboard({ data }: { data: StorefrontMetrics }) {
  const cards = [
    ["Visitantes únicos", data.visitantesUnicos, "Personas distintas", "from-orange-500 to-amber-400"],
    ["Visitas", data.visitas, "Sesiones en la tienda", "from-sky-500 to-cyan-400"],
    ["Productos vistos", data.vistasProductos, "Aperturas de fichas", "from-violet-500 to-fuchsia-400"],
    ["Consultas", data.consultas, "Consultas enviadas", "from-rose-500 to-pink-400"],
    ["WhatsApp", data.clicksWhatsapp, "Clics para conversar", "from-emerald-500 to-green-400"],
  ] as const
  return <div className="space-y-5">
    <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-5">{cards.map(([label, value, helper, accent]) => <article key={label} className="group relative overflow-hidden rounded-2xl border border-slate-200 bg-white p-5 shadow-sm transition hover:-translate-y-0.5 hover:shadow-md"><span className={`absolute inset-x-0 top-0 h-1 bg-gradient-to-r ${accent}`} /><p className="text-xs font-bold uppercase tracking-wider text-slate-400">{label}</p><p className="mt-3 text-3xl font-extrabold tracking-tight text-slate-950">{value.toLocaleString("es-AR")}</p><p className="mt-1 text-xs text-slate-500">{helper}</p></article>)}</div>
    <div className="grid gap-5 lg:grid-cols-2"><RankingPanel title="Rubros más vistos" items={data.rubrosMasVistos} empty="Todavía no hay vistas de rubros en este período." /><RankingPanel title="Artículos más vistos" items={data.productosMasVistos} empty="Todavía no hay vistas de artículos en este período." /></div>
    <div className="flex items-start gap-2 rounded-2xl border border-slate-200 bg-white px-4 py-3 text-xs text-slate-500"><span className="mt-0.5 flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-slate-100 font-bold text-slate-500">i</span><p>La medición es exclusiva de esta tienda y comienza a acumular datos desde la publicación de esta versión.</p></div>
  </div>
}

function RankingPanel({ title, items, empty }: { title: string; items: StorefrontMetrics["productosMasVistos"]; empty: string }) {
  const [expanded, setExpanded] = useState(false)
  const maximum = Math.max(1, ...items.map((item) => item.total))
  const visibleItems = expanded ? items : items.slice(0, 5)
  return <article className="overflow-hidden rounded-2xl border border-slate-200 bg-white shadow-sm"><div className="flex items-center justify-between border-b border-slate-100 px-5 py-4"><div><p className="text-[10px] font-bold uppercase tracking-[.18em] text-orange-600">Ranking</p><h2 className="mt-0.5 font-bold text-slate-900">{title}</h2></div><span className="rounded-full bg-orange-50 px-2.5 py-1 text-xs font-bold text-orange-700">Top {items.length}</span></div>{items.length ? <ol className="space-y-1 p-3 sm:p-4">{visibleItems.map((item, index) => <li key={item.productoId ?? item.nombre} className="rounded-xl px-2 py-2.5 transition hover:bg-slate-50"><div className="mb-2 flex items-center justify-between gap-3 text-sm"><span className="flex min-w-0 items-center gap-2.5 font-medium"><span className={`flex h-7 w-7 shrink-0 items-center justify-center rounded-lg text-xs font-extrabold ${index < 3 ? "bg-orange-100 text-orange-700" : "bg-slate-100 text-slate-500"}`}>{index + 1}</span><span className="truncate">{item.nombre}</span></span><strong className="rounded-lg bg-slate-50 px-2 py-1 text-xs text-slate-700">{item.total.toLocaleString("es-AR")}</strong></div><div className="ml-9 h-1.5 overflow-hidden rounded-full bg-slate-100"><div className="h-full rounded-full bg-gradient-to-r from-orange-500 to-amber-400 transition-[width] duration-500" style={{ width: `${Math.max(6, item.total / maximum * 100)}%` }} /></div></li>)}</ol> : <p className="p-5 text-sm text-slate-500">{empty}</p>}{items.length > 5 ? <div className="border-t border-slate-100 p-3"><button type="button" onClick={() => setExpanded((current) => !current)} aria-expanded={expanded} className="flex w-full items-center justify-center gap-2 rounded-xl px-4 py-2.5 text-sm font-bold text-orange-700 transition hover:bg-orange-50">{expanded ? "Mostrar menos" : `Mostrar más (${items.length - 5})`}<svg viewBox="0 0 20 20" fill="none" aria-hidden="true" className={`h-4 w-4 transition-transform ${expanded ? "rotate-180" : ""}`}><path d="m5 7.5 5 5 5-5" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" /></svg></button></div> : null}</article>
}

function PremiumIcon({ className = "h-5 w-5" }: { className?: string }) {
  return <svg viewBox="0 0 24 24" fill="none" aria-hidden="true" className={className}><path d="m4 8 3.2 9h9.6L20 8l-5 3-3-5-3 5-5-3Z" fill="currentColor" opacity=".95" /><path d="M7.5 20h9" stroke="currentColor" strokeWidth="2" strokeLinecap="round" /></svg>
}

function formatDateInput(date: Date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, "0")
  const day = String(date.getDate()).padStart(2, "0")
  return `${year}-${month}-${day}`
}

function OrdersSection({ id, query, notify, focusedOrderId }: { id: string; query: UseQueryResult<OwnerOrder[]>; notify: (message: string) => void; focusedOrderId: string | null }) {
  const queryClient = useQueryClient()
  const [shippingFilter, setShippingFilter] = useState<"activos" | "pendientes" | "enviados" | "entregados" | "cancelados">("activos")
  const [sort, setSort] = useState<"recientes" | "antiguos" | "mayor" | "menor">("recientes")
  const [search, setSearch] = useState("")
  const [desde, setDesde] = useState("")
  const [hasta, setHasta] = useState("")
  const [shippingDrafts, setShippingDrafts] = useState<Record<string, { transportistaNombre: string; tracking: string }>>({})
  const refresh = async () => queryClient.invalidateQueries({ queryKey: ["owner-orders", id] })
  const confirm = useMutation({ mutationFn: (pedidoId: string) => confirmarTransferencia(id, pedidoId), onSuccess: async () => { await refresh(); notify("Transferencia confirmada. El pedido ya puede prepararse para el envío.") } })
  const cancel = useMutation({ mutationFn: (pedidoId: string) => cancelarPedidoImpago(id, pedidoId), onSuccess: async () => { await refresh(); notify("Pedido impago cancelado y stock recuperado.") } })
  const label = useMutation({ mutationFn: (pedidoId: string) => abrirRotuloPedido(id, pedidoId) })
  const dispatch = useMutation({
    mutationFn: ({ pedidoId, input }: { pedidoId: string; input: { transportistaNombre: string; tracking: string } }) => despacharPedido(id, pedidoId, input),
    onSuccess: async (_, variables) => { await refresh(); setShippingDrafts((current) => { const next = { ...current }; delete next[variables.pedidoId]; return next }); notify("Pedido marcado como enviado. El cliente recibirá la actualización.") },
  })
  const deliver = useMutation({ mutationFn: (pedidoId: string) => entregarPedido(id, pedidoId), onSuccess: async () => { await refresh(); notify("Pedido marcado como entregado.") } })

  useEffect(() => {
    if (!focusedOrderId || !query.data?.some(order => order.pedidoId === focusedOrderId)) return
    const frame = window.requestAnimationFrame(() => {
      const element = document.getElementById(`pedido-${focusedOrderId}`)
      if (element instanceof HTMLDetailsElement) { element.open = true; element.scrollIntoView({ behavior: "smooth", block: "center" }) }
    })
    return () => window.cancelAnimationFrame(frame)
  }, [focusedOrderId, query.data])
  if (query.isLoading) return <StateBox>Cargando pedidos...</StateBox>
  if (query.isError) return <StateBox tone="error">No se pudieron cargar los pedidos.</StateBox>

  const orders = query.data ?? []
  const isPending = (order: OwnerOrder) => !["DESPACHADO", "ENTREGADO"].includes(order.estadoEnvio ?? "") && order.estadoPedido !== "CANCELADO"
  const isSent = (order: OwnerOrder) => ["DESPACHADO", "ENTREGADO"].includes(order.estadoEnvio ?? "")
  const normalizedSearch = search.trim().toLocaleLowerCase("es")
  const filteredOrders = orders.filter((order) => {
    const matchesState = (shippingFilter === "activos" && order.estadoPedido !== "CANCELADO")
      || (shippingFilter === "cancelados" && order.estadoPedido === "CANCELADO")
      || (shippingFilter === "pendientes" && isPending(order))
      || (shippingFilter === "enviados" && isSent(order))
      || (shippingFilter === "entregados" && order.estadoEnvio === "ENTREGADO")
    const orderDate = order.fechaCreacionPedido.slice(0, 10)
    const matchesDate = (!desde || orderDate >= desde) && (!hasta || orderDate <= hasta)
    const matchesSearch = !normalizedSearch || [order.pedidoId, order.clienteNombre, order.clienteEmail, order.clienteTelefono, ...(order.items ?? []).flatMap((item) => [item.nombre, item.sku])]
      .some((value) => value?.toLocaleLowerCase("es").includes(normalizedSearch))
    return matchesState && matchesDate && matchesSearch
  }).sort((a, b) => {
    if (sort === "mayor") return b.totalFinal - a.totalFinal
    if (sort === "menor") return a.totalFinal - b.totalFinal
    const difference = new Date(b.fechaCreacionPedido).getTime() - new Date(a.fechaCreacionPedido).getTime()
    return sort === "antiguos" ? -difference : difference
  })
  const pendingCount = orders.filter(isPending).length
  const sentCount = orders.filter(isSent).length
  const deliveredCount = orders.filter((order) => order.estadoEnvio === "ENTREGADO").length
  const hasFilters = Boolean(search || desde || hasta || shippingFilter !== "activos" || sort !== "recientes")
  const updateDraft = (pedidoId: string, key: "transportistaNombre" | "tracking", value: string) => setShippingDrafts((current) => ({ ...current, [pedidoId]: { transportistaNombre: current[pedidoId]?.transportistaNombre ?? "", tracking: current[pedidoId]?.tracking ?? "", [key]: value } }))

  return <section className="space-y-5">
    <div><p className="text-xs font-bold uppercase tracking-[.2em] text-blue-700">Operación de ventas</p><h1 className="mt-1 text-2xl font-semibold">Bandeja de pedidos</h1><p className="text-sm text-slate-500">Controlá el pago, prepará el envío y consultá todos los datos de cada compra.</p></div>

    <div className="grid gap-3 sm:grid-cols-3">
      <OrderSummaryCard label="Pendientes de envío" value={pendingCount} tone="amber" onClick={() => setShippingFilter("pendientes")} active={shippingFilter === "pendientes"} />
      <OrderSummaryCard label="Pedidos enviados" value={sentCount} tone="blue" onClick={() => setShippingFilter("enviados")} active={shippingFilter === "enviados"} />
      <OrderSummaryCard label="Entregados" value={deliveredCount} tone="emerald" onClick={() => setShippingFilter("entregados")} active={shippingFilter === "entregados"} />
    </div>

    <div className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm">
      <div className="grid gap-2 md:grid-cols-2 xl:grid-cols-[minmax(220px,1.2fr)_170px_150px_150px_180px_auto]">
        <input type="search" className={field} value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Pedido, cliente, email, teléfono o producto" />
        <select className={field} aria-label="Estado de envío" value={shippingFilter} onChange={(event) => setShippingFilter(event.target.value as typeof shippingFilter)}><option value="activos">Pedidos activos</option><option value="cancelados">Cancelados</option><option value="pendientes">Pendientes de envío</option><option value="enviados">Enviados</option><option value="entregados">Entregados</option></select>
        <label className="text-[11px] font-semibold text-slate-500">Desde<input type="date" className={`${field} mt-1`} value={desde} max={hasta || undefined} onChange={(event) => setDesde(event.target.value)} /></label>
        <label className="text-[11px] font-semibold text-slate-500">Hasta<input type="date" className={`${field} mt-1`} value={hasta} min={desde || undefined} onChange={(event) => setHasta(event.target.value)} /></label>
        <select className={field} aria-label="Ordenar pedidos" value={sort} onChange={(event) => setSort(event.target.value as typeof sort)}><option value="recientes">Más recientes</option><option value="antiguos">Más antiguos</option><option value="mayor">Mayor importe</option><option value="menor">Menor importe</option></select>
        {hasFilters ? <button type="button" onClick={() => { setSearch(""); setDesde(""); setHasta(""); setShippingFilter("activos"); setSort("recientes") }} className="rounded-xl border border-slate-200 px-3 py-2 text-sm font-semibold text-slate-600 hover:border-slate-400">Limpiar</button> : null}
      </div>
      <p className="mt-3 text-xs text-slate-500">Mostrando {filteredOrders.length} de {orders.length} pedidos</p>
    </div>

    {cancel.isError || confirm.isError || label.isError || dispatch.isError || deliver.isError ? <StateBox tone="error">No se pudo completar la última operación. Revisá el estado del pedido e intentá nuevamente.</StateBox> : null}
    <div className="space-y-3">{filteredOrders.length ? filteredOrders.map((order) => {
      const units = (order.items ?? []).reduce((total, item) => total + item.cantidad, 0)
      const paid = order.estadoPago === "CONFIRMADO" && ["PAGADO", "ENVIADO", "ENTREGADO"].includes(order.estadoPedido)
      const needsPreparation = paid && order.estadoEnvio === "PENDIENTE_PAGO"
      const canConfirmTransfer = order.metodoPago === "TRANSFERENCIA" && order.comprobanteCargado &&
        ((order.estadoPedido === "CREADO" && order.estadoPago === "PENDIENTE") || needsPreparation)
      const canCancel = order.estadoPedido === "CREADO" && ["PENDIENTE", "RECHAZADO"].includes(order.estadoPago ?? "")
      const canDispatch = paid && ["PENDIENTE_PREPARACION", "LISTO_PARA_RETIRO", "RETIRADO"].includes(order.estadoEnvio ?? "")
      const draft = shippingDrafts[order.pedidoId] ?? { transportistaNombre: "", tracking: "" }
      return <details id={`pedido-${order.pedidoId}`} key={order.pedidoId} className={`group overflow-hidden rounded-2xl border bg-white shadow-sm open:border-blue-200 open:shadow-md ${focusedOrderId === order.pedidoId ? "border-blue-400 ring-4 ring-blue-100" : "border-slate-200"}`}>
        <summary className="grid cursor-pointer list-none gap-3 p-4 marker:content-none sm:grid-cols-[1fr_auto] sm:items-center sm:p-5">
          <div className="min-w-0"><div className="flex flex-wrap items-center gap-2"><p className="font-bold text-slate-950">Pedido #{order.pedidoId.slice(0, 8).toUpperCase()}</p><OrderStatusBadge value={order.estadoEnvio || "SIN_ENVIO"} kind="shipping" /><OrderStatusBadge value={order.estadoPago || order.estadoPedido} kind="payment" /></div><p className="mt-1 truncate text-sm text-slate-600">{order.clienteNombre || order.clienteEmail || "Cliente"} · {units} unidad{units === 1 ? "" : "es"}</p><p className="mt-1 text-xs text-slate-400">{new Date(order.fechaCreacionPedido).toLocaleString("es-AR")}</p></div>
          <div className="flex items-center justify-between gap-4 sm:justify-end"><div className="text-left sm:text-right"><p className="text-xl font-extrabold">{money.format(order.totalFinal)}</p><p className="text-xs font-semibold text-blue-700">Ver detalle completo</p></div><span className="text-xl text-slate-400 transition group-open:rotate-180">⌄</span></div>
        </summary>
        <div className="border-t border-slate-200 bg-slate-50/70 p-4 sm:p-5">
          <div className="grid gap-4 lg:grid-cols-2">
            <OrderInfoPanel title="Pago"><OrderDetail label="Método" value={formatOrderState(order.metodoPago)} /><OrderDetail label="Importe" value={order.montoPago != null ? money.format(order.montoPago) : null} /><OrderDetail label="Referencia" value={order.referenciaPago} /></OrderInfoPanel><OrderInfoPanel title="Cliente y contacto"><OrderDetail label="Nombre" value={order.clienteNombre} /><OrderDetail label="Email" value={order.clienteEmail} /><OrderDetail label="WhatsApp" value={order.clienteTelefono} /></OrderInfoPanel>
            <OrderInfoPanel title="Datos del envío"><OrderDetail label="Dirección de entrega" value={order.direccionEnvio} wide /><OrderDetail label="Transportista" value={order.transportistaNombre} /><OrderDetail label="Tracking" value={order.tracking} /><OrderDetail label="Tipo de paquete" value={formatOrderState(order.categoriaPaquete)} /></OrderInfoPanel>
          </div>

          <div className="mt-4 overflow-hidden rounded-2xl border border-slate-200 bg-white"><div className="border-b border-slate-100 px-4 py-3"><h3 className="font-bold">Artículos del pedido</h3></div><div className="divide-y divide-slate-100">{(order.items ?? []).map((item) => <div key={item.itemId} className="grid gap-2 px-4 py-3 text-sm sm:grid-cols-[1fr_auto_auto] sm:items-center"><div className="min-w-0">{item.imagenPrincipal && <img src={item.imagenPrincipal} alt={item.nombre} className="h-14 w-14 object-contain" />}<p className="font-semibold text-slate-900">{item.nombre}</p><p className="text-xs text-slate-500">{item.sku ? `SKU ${item.sku} · ` : ""}{item.cantidad} unidad{item.cantidad === 1 ? "" : "es"}</p></div><p className="text-slate-600">{money.format(item.precioUnitario)} c/u</p><p className="font-bold sm:min-w-28 sm:text-right">{money.format(item.subtotal)}</p></div>)}</div></div>

          <div className="mt-4 grid gap-4 lg:grid-cols-[1fr_300px]">
            <div className="space-y-3">
              <div className="flex flex-wrap gap-2">{paid && <button type="button" disabled={label.isPending} onClick={() => label.mutate(order.pedidoId)} className="rounded-xl border border-blue-200 bg-blue-50 px-4 py-2.5 text-sm font-bold text-blue-700 hover:bg-blue-100 disabled:opacity-50">Imprimir etiqueta de envío</button>}{order.comprobanteCargado ? <button type="button" onClick={() => void abrirComprobante(id, order.pedidoId)} className="rounded-xl border border-slate-200 bg-white px-4 py-2.5 text-sm font-semibold text-slate-700">Ver comprobante</button> : null}{canConfirmTransfer ? <button disabled={confirm.isPending} type="button" onClick={() => confirm.mutate(order.pedidoId)} className="rounded-xl bg-emerald-600 px-4 py-2.5 text-sm font-bold text-white">{needsPreparation ? "Habilitar preparación" : "Confirmar transferencia"}</button> : null}</div>
              {canCancel && <button type="button" disabled={cancel.isPending} className="rounded-xl border border-red-200 px-4 py-2 text-red-600" onClick={() => { if (window.confirm("¿Cancelar este pedido impago? Se recuperará el stock y saldrá de la bandeja activa.")) cancel.mutate(order.pedidoId) }}>Cancelar pedido impago</button>}
              {canDispatch ? <div className="rounded-2xl border border-amber-200 bg-amber-50 p-4"><p className="font-bold text-amber-950">Preparar despacho</p><p className="mt-1 text-xs text-amber-800">El transportista y el tracking son opcionales para entregas propias.</p><div className="mt-3 grid gap-2 sm:grid-cols-2"><input className={field} placeholder="Transportista" value={draft.transportistaNombre} onChange={(event) => updateDraft(order.pedidoId, "transportistaNombre", event.target.value)} /><input className={field} placeholder="Código de seguimiento" value={draft.tracking} onChange={(event) => updateDraft(order.pedidoId, "tracking", event.target.value)} /></div><button type="button" disabled={dispatch.isPending} onClick={() => { if (window.confirm("¿Confirmás que este pedido ya fue enviado?")) dispatch.mutate({ pedidoId: order.pedidoId, input: draft }) }} className="mt-3 rounded-xl bg-amber-500 px-4 py-2.5 text-sm font-bold text-white hover:bg-amber-600 disabled:opacity-50">Marcar como enviado</button></div> : null}
              {order.estadoEnvio === "DESPACHADO" ? <button type="button" disabled={deliver.isPending} onClick={() => { if (window.confirm("¿Confirmás que el cliente recibió este pedido?")) deliver.mutate(order.pedidoId) }} className="rounded-xl bg-emerald-600 px-4 py-2.5 text-sm font-bold text-white hover:bg-emerald-700 disabled:opacity-50">Marcar como entregado</button> : null}
            </div>
            <div className="h-fit rounded-2xl border border-slate-200 bg-white p-4 text-sm"><OrderTotal label="Productos" value={order.totalBruto} /><OrderTotal label="Envío" value={order.totalEnvio} /><OrderTotal label="Seguro" value={order.totalSeguro} />{order.totalDescuento > 0 ? <OrderTotal label="Descuento" value={-order.totalDescuento} /> : null}<div className="mt-3 flex items-center justify-between border-t border-slate-200 pt-3 text-base font-extrabold"><span>Total</span><span>{money.format(order.totalFinal)}</span></div></div>
          </div>
        </div>
      </details>
    }) : <StateBox>{orders.length ? "No hay pedidos que coincidan con los filtros." : "Todavía no hay pedidos en esta tienda."}</StateBox>}</div>
  </section>
}

function OrderSummaryCard({ label, value, tone, active, onClick }: { label: string; value: number; tone: "amber" | "blue" | "emerald"; active: boolean; onClick: () => void }) {
  const tones = { amber: "border-amber-200 bg-amber-50 text-amber-800", blue: "border-blue-200 bg-blue-50 text-blue-800", emerald: "border-emerald-200 bg-emerald-50 text-emerald-800" }
  return <button type="button" onClick={onClick} className={`rounded-2xl border p-4 text-left transition hover:-translate-y-0.5 hover:shadow-md ${tones[tone]} ${active ? "ring-2 ring-slate-900/10" : ""}`}><p className="text-xs font-bold uppercase tracking-wider opacity-75">{label}</p><p className="mt-1 text-3xl font-extrabold">{value.toLocaleString("es-AR")}</p></button>
}

function OrderStatusBadge({ value, kind }: { value: string; kind: "shipping" | "payment" }) {
  const normalized = value.toUpperCase()
  const style = normalized === "ENTREGADO" || normalized === "CONFIRMADO" ? "bg-emerald-100 text-emerald-700" : normalized === "DESPACHADO" ? "bg-blue-100 text-blue-700" : normalized === "CANCELADO" ? "bg-red-100 text-red-700" : kind === "payment" ? "bg-violet-100 text-violet-700" : "bg-amber-100 text-amber-700"
  return <span className={`rounded-full px-2.5 py-1 text-[10px] font-bold uppercase tracking-wide ${style}`}>{formatOrderState(value)}</span>
}

function OrderInfoPanel({ title, children }: { title: string; children: ReactNode }) {
  return <div className="rounded-2xl border border-slate-200 bg-white p-4"><h3 className="mb-3 font-bold text-slate-900">{title}</h3><div className="grid gap-3 sm:grid-cols-2">{children}</div></div>
}

function OrderDetail({ label, value, wide = false }: { label: string; value: string | null | undefined; wide?: boolean }) {
  return <div className={wide ? "sm:col-span-2" : ""}><p className="text-[10px] font-bold uppercase tracking-wider text-slate-400">{label}</p><p className="mt-0.5 break-words text-sm font-medium text-slate-700">{value || "No informado"}</p></div>
}

function OrderTotal({ label, value }: { label: string; value: number }) {
  return <div className="flex items-center justify-between py-1.5 text-slate-600"><span>{label}</span><span className="font-semibold">{money.format(value)}</span></div>
}

function formatOrderState(value: string | null | undefined) {
  if (!value) return "No informado"
  return value.toLocaleLowerCase("es").replaceAll("_", " ").replace(/(^|\s)\S/g, (letter) => letter.toLocaleUpperCase("es"))
}

function PaymentsSection() {
  return <section className="space-y-4"><h1 className="text-2xl font-semibold">Pagos fuera de la plataforma</h1><p className="max-w-2xl text-sm leading-6 text-slate-600">MercadoDigital ya no utiliza Mercado Pago, tampoco para membresias. Las compras se coordinan directamente con la tienda. Los pedidos anteriores conservan su historial; cualquier pago pendiente debe revisarse con el cliente antes de volver a cobrar.</p></section>
}

function StorefrontContentSection({ id, initial, name, email, refresh, notify, modoCatalogo }: { modoCatalogo: boolean; id: string; initial: StorefrontContent; name: string; email: string; refresh: () => Promise<unknown>; notify: (message: string) => void }) {
  const initialPortadas = initial.portadas?.length ? initial.portadas : initial.heroImagenUrl ? [{ id: null, imagenUrl: initial.heroImagenUrl, titulo: initial.heroTitulo, subtitulo: initial.heroSubtitulo }] : []
  const [form, setForm] = useState<StorefrontContent>({ ...initial, smtpPassword: "", portadas: initialPortadas, quienesGaleria: initial.quienesGaleria ?? [] })
  const save = useMutation({ mutationFn: () => updateStorefrontContent(id, form), onSuccess: async (saved) => { setForm({ ...saved, smtpPassword: "", portadas: saved.portadas ?? [], quienesGaleria: saved.quienesGaleria ?? [] }); await refresh(); notify("La pagina de la tienda quedo actualizada.") } })
  const testEmail = useMutation({ mutationFn: () => probarCorreoStorefront(id), onSuccess: () => notify("Correo de prueba enviado. Revisa tambien la carpeta de spam.") })
  const upload = useMutation({ mutationFn: (file: File) => uploadStorefrontImage(id, file), onSuccess: (asset) => setForm((current) => ({ ...current, quienesGaleria: [...current.quienesGaleria, { id: null, imagenUrl: asset.url, titulo: "", comentario: "" }] })) })
  const uploadBrand = useMutation({ mutationFn: ({ file, type }: { file: File; type: "logo" | "favicon" }) => uploadStorefrontImage(id, file, type), onSuccess: (asset, variables) => setForm((current) => ({ ...current, [variables.type === "logo" ? "logoUrl" : "faviconUrl"]: asset.url })) })
  const uploadCover = useMutation({ mutationFn: (file: File) => uploadStorefrontImage(id, file, "portada"), onSuccess: (asset) => setForm((current) => { const portadas = [...current.portadas, { id: null, imagenUrl: asset.url, titulo: "", subtitulo: "" }]; return { ...current, portadas, heroImagenUrl: portadas[0]?.imagenUrl ?? null } }) })
  const setValue = (key: keyof StorefrontContent, value: string) => setForm((current) => ({ ...current, [key]: value }))
  const setCoverValue = (index: number, key: "titulo" | "subtitulo", value: string) => setForm((current) => ({ ...current, portadas: current.portadas.map((item, itemIndex) => itemIndex === index ? { ...item, [key]: value } : item) }))
  const setHeroCover = (index: number) => setForm((current) => { const selected = current.portadas[index]; if (!selected) return current; const portadas = [selected, ...current.portadas.filter((_, itemIndex) => itemIndex !== index)]; return { ...current, portadas, heroImagenUrl: selected.imagenUrl } })
  const setGallery = (index: number, key: "titulo" | "comentario", value: string) => setForm((current) => ({ ...current, quienesGaleria: current.quienesGaleria.map((item, itemIndex) => itemIndex === index ? { ...item, [key]: value } : item) }))
  const setGalleryCover = (index: number) => setForm((current) => {
    const selected = current.quienesGaleria[index]
    if (!selected) return current
    return { ...current, quienesGaleria: [selected, ...current.quienesGaleria.filter((_, itemIndex) => itemIndex !== index)] }
  })

  return <section className="space-y-5">
    <div><p className="text-xs font-bold uppercase tracking-[.2em] text-blue-700">Configuracion integral</p><h1 className="mt-1 text-2xl font-semibold">Mi pagina</h1><p className="text-sm text-slate-500">{modoCatalogo ? "Administrá la identidad, contenido y contactos de tu catálogo. La configuración de pagos se conserva para pedidos anteriores." : "Administra la identidad, portada, contacto, correo, redes y medios de pago de tu tienda."}</p></div>
    <div className="grid gap-5 xl:grid-cols-[1fr_340px]">
      {modoCatalogo && <CatalogoTextReview content={form} />}
    <form onSubmit={(event) => { event.preventDefault(); save.mutate() }} className="space-y-5 rounded-2xl border border-slate-200 bg-white p-5">
        <div className="space-y-4 rounded-2xl border border-slate-200 bg-slate-50 p-4">
          <div><h2 className="font-semibold">Identidad de la tienda</h2><p className="mt-1 text-xs leading-5 text-slate-500">Estos datos construyen la cabecera, la portada y la identidad visual del sitio.</p></div>
          <label className="block text-xs font-semibold text-slate-600">Nombre comercial<input required className={`${field} mt-1`} maxLength={180} value={form.nombreComercial ?? ""} onChange={(event) => setValue("nombreComercial", event.target.value)} /></label>
          <div className="grid gap-3 sm:grid-cols-2">
            <label className="text-xs font-semibold text-slate-600">Titulo de portada<input className={`${field} mt-1`} maxLength={180} placeholder="Tu propuesta principal" value={form.heroTitulo ?? ""} onChange={(event) => setValue("heroTitulo", event.target.value)} /></label>
            <label className="text-xs font-semibold text-slate-600">Texto de portada<input className={`${field} mt-1`} maxLength={255} placeholder="Una frase breve que genere confianza" value={form.heroSubtitulo ?? ""} onChange={(event) => setValue("heroSubtitulo", event.target.value)} /></label>
            <label className="text-xs font-semibold text-slate-600">Color principal<div className="mt-1 flex gap-2"><input aria-label="Selector de color principal" type="color" className="h-10 w-12 rounded-lg border border-slate-200 bg-white p-1" value={normalizarColor(form.colorPrimario)} onChange={(event) => setValue("colorPrimario", event.target.value)} /><input className={field} maxLength={30} value={form.colorPrimario ?? ""} onChange={(event) => setValue("colorPrimario", event.target.value)} placeholder="#0f172a" /></div></label>
            <label className="text-xs font-semibold text-slate-600">Color secundario<div className="mt-1 flex gap-2"><input aria-label="Selector de color secundario" type="color" className="h-10 w-12 rounded-lg border border-slate-200 bg-white p-1" value={normalizarColor(form.colorSecundario)} onChange={(event) => setValue("colorSecundario", event.target.value)} /><input className={field} maxLength={30} value={form.colorSecundario ?? ""} onChange={(event) => setValue("colorSecundario", event.target.value)} placeholder="#2563eb" /></div></label>
          </div>
          <label className="block text-xs font-semibold text-slate-600">Texto del pie de pagina<input className={`${field} mt-1`} maxLength={500} placeholder="Atencion, cobertura o lema institucional" value={form.footerTexto ?? ""} onChange={(event) => setValue("footerTexto", event.target.value)} /></label>
          <div className="grid gap-3 md:grid-cols-2">
            <AssetField label="Logo" value={form.logoUrl} busy={uploadBrand.isPending} onFile={(file) => uploadBrand.mutate({ file, type: "logo" })} />
            <AssetField label="Icono del navegador" value={form.faviconUrl} busy={uploadBrand.isPending} onFile={(file) => uploadBrand.mutate({ file, type: "favicon" })} />
          </div>
          {uploadBrand.isError ? <p className="text-sm text-red-600">No se pudo subir la imagen de identidad.</p> : null}
        </div>
        <div className="space-y-3 rounded-2xl border border-slate-200 bg-slate-50 p-4">
          <div className="flex flex-wrap items-center justify-between gap-3"><div><h2 className="font-semibold">Portadas del catalogo</h2><p className="mt-1 text-xs text-slate-500">Carrusel principal de la tienda. Hasta 10 imagenes.</p></div><label className="cursor-pointer rounded-xl border border-blue-200 bg-white px-4 py-2 text-sm font-semibold text-blue-700"><input type="file" accept="image/png,image/jpeg,image/webp,image/gif" className="hidden" disabled={uploadCover.isPending || form.portadas.length >= 10} onChange={(event) => { const file = event.target.files?.[0]; if (file) uploadCover.mutate(file); event.target.value = "" }} />{uploadCover.isPending ? "Subiendo..." : "Agregar portada"}</label></div>
          {uploadCover.isError ? <p className="text-sm text-red-600">No se pudo subir la portada.</p> : null}
          {form.portadas.length > 0 ? <div className="grid gap-3 sm:grid-cols-2">{form.portadas.map((item, index) => <article key={item.id ?? `${item.imagenUrl}-${index}`} className={`overflow-hidden rounded-2xl border bg-white ${index === 0 ? "border-blue-400 ring-2 ring-blue-100" : "border-slate-200"}`}><div className="relative"><img src={item.imagenUrl} alt="" className="h-36 w-full object-contain" />{index === 0 ? <span className="absolute left-3 top-3 rounded-full bg-blue-600 px-3 py-1 text-[10px] font-bold uppercase text-white">Principal</span> : null}</div><div className="space-y-2 border-t border-slate-100 p-3"><input className={field} maxLength={180} placeholder="Titulo opcional" value={item.titulo ?? ""} onChange={(event) => setCoverValue(index, "titulo", event.target.value)} /><input className={field} maxLength={255} placeholder="Texto opcional" value={item.subtitulo ?? ""} onChange={(event) => setCoverValue(index, "subtitulo", event.target.value)} /><div className="flex items-center justify-between gap-3">{index > 0 ? <button type="button" className="text-xs font-semibold text-blue-700" onClick={() => setHeroCover(index)}>Usar como principal</button> : <span className="text-xs font-semibold text-slate-400">Primera portada</span>}<button type="button" className="text-xs font-semibold text-red-600" onClick={() => setForm((current) => { const portadas = current.portadas.filter((_, itemIndex) => itemIndex !== index); return { ...current, portadas, heroImagenUrl: portadas[0]?.imagenUrl ?? null } })}>Quitar</button></div></div></article>)}</div> : <StateBox>Sin portadas cargadas.</StateBox>}
        </div>
        <div className="grid gap-3"><h2 className="font-semibold">Quienes somos</h2><input className={field} maxLength={180} placeholder="Titulo de la seccion" value={form.quienesTitulo ?? ""} onChange={(event) => setValue("quienesTitulo", event.target.value)} /><textarea className={`${field} min-h-32 resize-y`} maxLength={5000} placeholder="Conta la historia del comercio, experiencia y forma de trabajar" value={form.quienesTexto ?? ""} onChange={(event) => setValue("quienesTexto", event.target.value)} /></div>
        <div className="space-y-3"><div className="flex flex-wrap items-center justify-between gap-3"><div><h2 className="font-semibold">Galería de trabajos y comercio</h2><p className="text-xs text-slate-500">La primera imagen será la portada institucional. Hasta 20 imágenes, de 5 MB cada una.</p></div><label className="cursor-pointer rounded-xl border border-blue-200 bg-blue-50 px-4 py-2 text-sm font-semibold text-blue-700"><input type="file" accept="image/png,image/jpeg,image/webp,image/gif" className="hidden" onChange={(event) => { const file = event.target.files?.[0]; if (file) upload.mutate(file); event.target.value = "" }} />{upload.isPending ? "Subiendo..." : "Agregar imagen"}</label></div>
          {upload.isError ? <p className="text-sm text-red-600">No se pudo subir la imagen. Revisa el formato y el peso.</p> : null}
          <div className="grid gap-3 sm:grid-cols-2">{form.quienesGaleria.map((item, index) => <article key={item.id ?? `${item.imagenUrl}-${index}`} className="overflow-hidden rounded-2xl border border-slate-200"><div className="relative"><img src={item.imagenUrl} alt="" className="h-40 w-full bg-slate-50 object-cover" />{index === 0 ? <span className="absolute left-3 top-3 rounded-full bg-slate-950/85 px-3 py-1 text-[10px] font-bold uppercase tracking-wider text-white">Portada</span> : null}</div><div className="space-y-2 p-3"><input className={field} placeholder="Titulo" value={item.titulo ?? ""} onChange={(event) => setGallery(index, "titulo", event.target.value)} /><textarea className={`${field} min-h-20 resize-y`} placeholder="Comentario" value={item.comentario ?? ""} onChange={(event) => setGallery(index, "comentario", event.target.value)} /><div className="flex items-center justify-between gap-3">{index > 0 ? <button type="button" className="text-xs font-semibold text-blue-700" onClick={() => setGalleryCover(index)}>Usar como portada</button> : <span className="text-xs font-semibold text-slate-400">Imagen principal</span>}<button type="button" className="text-xs font-semibold text-red-600" onClick={() => setForm((current) => ({ ...current, quienesGaleria: current.quienesGaleria.filter((_, itemIndex) => itemIndex !== index) }))}>Quitar imagen</button></div></div></article>)}</div>
        </div>
        <div className="grid gap-3 rounded-2xl border border-slate-200 bg-slate-50 p-4 sm:grid-cols-2">
          <div className="sm:col-span-2"><h2 className="font-semibold">Datos comerciales públicos</h2><p className="mt-1 text-xs leading-5 text-slate-500">Se muestran en Quiénes somos y permiten que el cliente contacte y ubique el comercio.</p></div>
          <label className="text-xs font-semibold text-slate-600">Email comercial<input type="email" className={`${field} mt-1`} placeholder="ventas@mitienda.com" value={form.emailComercial ?? ""} onChange={(event) => setValue("emailComercial", event.target.value)} /></label>
          <label className="text-xs font-semibold text-slate-600">WhatsApp / teléfono<input className={`${field} mt-1`} maxLength={80} placeholder="Ej: 549342..." value={form.telefonoComercial ?? ""} onChange={(event) => setValue("telefonoComercial", event.target.value)} /></label>
          <label className="text-xs font-semibold text-slate-600 sm:col-span-2">Dirección comercial<input className={`${field} mt-1`} maxLength={500} placeholder="Calle, número, ciudad y provincia" value={form.direccionComercial ?? ""} onChange={(event) => setValue("direccionComercial", event.target.value)} /></label>
        </div>
        <div className="grid gap-3 sm:grid-cols-2"><h2 className="font-semibold sm:col-span-2">Redes sociales</h2>{([['instagramUrl', 'Instagram'], ['facebookUrl', 'Facebook'], ['tiktokUrl', 'TikTok'], ['youtubeUrl', 'YouTube']] as const).map(([key, label]) => <label key={key} className="text-xs font-semibold text-slate-600">{label}<input type="url" className={`${field} mt-1`} placeholder={`https://${label.toLowerCase()}.com/...`} value={form[key] ?? ""} onChange={(event) => setValue(key, event.target.value)} /></label>)}</div>
        <div className="space-y-4 rounded-2xl border border-blue-200 bg-blue-50/60 p-4">
          <div className="flex flex-wrap items-start justify-between gap-3"><div><h2 className="font-semibold text-slate-950">Correo y notificaciones</h2><p className="mt-1 max-w-2xl text-xs leading-5 text-slate-600">Las nuevas consultas llegan al email comercial. Configura el servidor SMTP de esa casilla para que la tienda pueda enviar notificaciones y respuestas.</p></div><span className={`rounded-full px-3 py-1 text-[10px] font-bold uppercase tracking-wider ${form.smtpConfigured ? "bg-emerald-100 text-emerald-700" : "bg-amber-100 text-amber-700"}`}>{form.smtpConfigured ? "Correo configurado" : "Falta configurar"}</span></div>
          <div className="grid gap-3 sm:grid-cols-2">
            <label className="text-xs font-semibold text-slate-600">Servidor SMTP<input className={`${field} mt-1`} maxLength={180} placeholder="smtp.gmail.com" value={form.smtpHost ?? ""} onChange={(event) => setValue("smtpHost", event.target.value)} /></label>
            <label className="text-xs font-semibold text-slate-600">Puerto<input type="number" min={1} max={65535} className={`${field} mt-1`} placeholder="587" value={form.smtpPort ?? ""} onChange={(event) => setForm((current) => ({ ...current, smtpPort: event.target.value ? Number(event.target.value) : null }))} /></label>
            <label className="text-xs font-semibold text-slate-600">Usuario SMTP<input className={`${field} mt-1`} maxLength={180} autoComplete="username" placeholder="notificaciones@mitienda.com" value={form.smtpUsername ?? ""} onChange={(event) => setValue("smtpUsername", event.target.value)} /></label>
            <label className="text-xs font-semibold text-slate-600">Clave SMTP<input type="password" className={`${field} mt-1`} maxLength={255} autoComplete="new-password" placeholder={form.smtpConfigured ? "Dejar vacio para conservar la actual" : "Clave o contraseña de aplicacion"} value={form.smtpPassword ?? ""} onChange={(event) => setValue("smtpPassword", event.target.value)} /></label>
          </div>
          <div className="flex flex-wrap gap-5 text-xs font-semibold text-slate-700"><label className="flex items-center gap-2"><input type="checkbox" checked={form.smtpAuth} onChange={(event) => setForm((current) => ({ ...current, smtpAuth: event.target.checked }))} />Requiere autenticacion</label><label className="flex items-center gap-2"><input type="checkbox" checked={form.smtpStartTls} onChange={(event) => setForm((current) => ({ ...current, smtpStartTls: event.target.checked }))} />Usar STARTTLS</label></div>
          <div className="flex flex-wrap items-center gap-3"><button type="button" disabled={!form.smtpConfigured || testEmail.isPending || save.isPending} onClick={() => testEmail.mutate()} className="rounded-xl border border-blue-300 bg-white px-4 py-2 text-sm font-semibold text-blue-700 disabled:opacity-50">{testEmail.isPending ? "Enviando..." : "Enviar correo de prueba"}</button><p className="text-xs text-slate-500">Si modificaste estos datos, guarda primero y luego envia la prueba.</p></div>
          {testEmail.isError ? <p className="text-sm font-medium text-red-600">La prueba no pudo enviarse. Revisa servidor, puerto, usuario, clave y permisos de la casilla.</p> : null}
        </div>
        <div className="grid gap-3 rounded-2xl border border-emerald-200 bg-emerald-50 p-4 sm:grid-cols-2"><label className="flex items-center gap-3 font-semibold text-emerald-950 sm:col-span-2"><input type="checkbox" checked={form.transferenciaHabilitada} onChange={(event) => setForm((current) => ({ ...current, transferenciaHabilitada: event.target.checked }))} className="h-4 w-4" />{modoCatalogo ? "Transferencias de pedidos anteriores" : "Habilitar pagos por transferencia"}</label><label className="text-xs font-semibold text-slate-700">Alias bancario<input className={`${field} mt-1`} required={form.transferenciaHabilitada} placeholder="mi.tienda.alias" value={form.transferenciaAlias ?? ""} onChange={(event) => setValue("transferenciaAlias", event.target.value)} /></label><label className="text-xs font-semibold text-slate-700">Titular de la cuenta<input className={`${field} mt-1`} placeholder="Nombre o razon social" value={form.transferenciaTitular ?? ""} onChange={(event) => setValue("transferenciaTitular", event.target.value)} /></label><p className="text-xs leading-5 text-emerald-800 sm:col-span-2">{modoCatalogo ? "Estos datos pertenecen al circuito anterior. El catálogo no solicita transferencias ni envía instrucciones de pago. Los pedidos existentes conservan sus datos registrados." : "El cliente recibira este alias y el monto por correo. El comprobante queda pendiente de revision."}</p></div>
        {save.isError ? <p className="text-sm text-red-600">No se pudieron guardar los cambios. Revisa los datos e intenta nuevamente.</p> : null}
        <button disabled={save.isPending || upload.isPending || uploadCover.isPending} className="rounded-xl bg-slate-950 px-5 py-3 text-sm font-semibold text-white disabled:opacity-50">{save.isPending ? "Guardando..." : "Guardar y publicar"}</button>
      </form>
      <aside className="h-fit rounded-2xl border border-slate-200 bg-white p-5"><h2 className="font-semibold">Cuenta propietaria</h2><dl className="mt-4 space-y-4"><div><dt className="text-xs font-bold uppercase tracking-widest text-slate-400">Propietario</dt><dd className="mt-1 font-semibold">{name}</dd></div><div><dt className="text-xs font-bold uppercase tracking-widest text-slate-400">Email</dt><dd className="mt-1 break-all font-semibold">{email}</dd></div><div><dt className="text-xs font-bold uppercase tracking-widest text-slate-400">Catalogo</dt><dd className="mt-1 font-semibold">Productos propios</dd></div></dl></aside>
    </div>
  </section>
}

function AssetField({ label, value, busy, onFile }: { label: string; value: string | null; busy: boolean; onFile: (file: File) => void }) {
  return <div className="rounded-xl border border-slate-200 bg-white p-3"><p className="text-xs font-semibold text-slate-700">{label}</p><div className="mt-2 flex h-20 items-center justify-center overflow-hidden rounded-lg bg-slate-100">{value ? <img src={value} alt="" className="h-full w-full object-contain" /> : <span className="text-[11px] text-slate-400">Sin imagen</span>}</div><label className="mt-2 block cursor-pointer rounded-lg border border-slate-200 px-3 py-2 text-center text-xs font-semibold text-slate-700 hover:border-blue-300"><input type="file" accept="image/png,image/jpeg,image/webp,image/gif,image/svg+xml" className="hidden" disabled={busy} onChange={(event) => { const file = event.target.files?.[0]; if (file) onFile(file); event.target.value = "" }} />{busy ? "Subiendo..." : "Cambiar imagen"}</label></div>
}

function normalizarColor(value: string | null) {
  return /^#[0-9a-f]{6}$/i.test(value ?? "") ? value! : "#0f172a"
}

function precioCompartido(product: ProductoPropio) {
  return product.precioPublicoArs ? formatPrecioArs(product.precioPublicoArs)
    : product.precioVenta === null ? "Precio no disponible" : money.format(product.precioVenta)
}
function totalCompartido(products: ProductoPropio[]) {
  if (products.length && products.every(p => p.precioPublicoArs)) {
    const cents = products.reduce((sum, p) => sum + BigInt(p.precioPublicoArs!.replace(".", "")), 0n).toString().padStart(3, "0")
    return formatPrecioArs(cents.slice(0, -2) + "." + cents.slice(-2))
  }
  return products.some(p => p.precioVenta === null) ? "Precio no disponible" : money.format(products.reduce((total, p) => total + (p.precioVenta ?? 0), 0))
}
