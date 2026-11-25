import { useRef, useState } from "react"
import { useQuery } from "@tanstack/react-query"
import * as api from "../api/catalogoAdminApi"
import { listarCategorias } from "../api/storefrontOwnerApi"
import CatalogoProductEditor from "./CatalogoProductEditor"
import { confirmCatalogoLeave, catalogoError } from "../hooks/useCatalogoDraft"
import { Pagination } from "../dashboard/ui"
import { dineroCatalogo } from "../utils/catalogoDecimal"

export default function CatalogoProductManager({ id, products, categories, config, refresh, notify, modoCatalogo }: {
  id: string; products: api.ProductoCatalogo[]; categories: api.CategoriaEnvioCatalogo[]; config: api.ConfigCatalogo
  refresh: () => Promise<unknown>; notify: (text: string) => void; modoCatalogo: boolean
}) {
  const [selected, setSelected] = useState<string | null>(null)
  const [generation, setGeneration] = useState(0)
  const [search, setSearch] = useState("")
  const [page, setPage] = useState(0)
  const [active, setActive] = useState("todos")
  const [status, setStatus] = useState("todos")
  const area = useRef<HTMLDivElement>(null)
  const ficha = useQuery({ queryKey: ["catalogo-ficha", id, selected, generation], queryFn: ({ signal }) => api.getFichaCatalogo(id, selected!, signal),
    enabled: Boolean(selected && selected !== "new"), retry: false, staleTime: Infinity, refetchOnWindowFocus: false, refetchOnReconnect: false })
  const rubros = useQuery({ queryKey: ["owner-categories", id], queryFn: () => listarCategorias(id), enabled: selected !== null, staleTime: Infinity })
  const configured = products.filter(p => p.calculo.estado === "LISTO").length
  const visible = products.filter(p => `${p.nombre} ${p.sku}`.toLocaleLowerCase("es").includes(search.toLocaleLowerCase("es"))
    && (active === "todos" || p.activo === (active === "activos"))
    && (status === "todos" || (status === "listos" ? p.calculo.estado === "LISTO" : p.calculo.estado !== "LISTO")))
  const pages = Math.max(1, Math.ceil(visible.length / 12))
  const currentPage = Math.min(page, pages - 1)
  const listPosition = useRef(0)
  const close = () => {
    if (!confirmCatalogoLeave()) return
    const previous = selected
    setSelected(null)
    window.requestAnimationFrame(() => {
      const target = [...document.querySelectorAll<HTMLButtonElement>(".product-row button")].find(button => button.dataset.productId === previous)
        ?? document.querySelector<HTMLButtonElement>(".product-heading .product-primary")
      target?.focus({ preventScroll: true })
      window.scrollTo({ top: listPosition.current })
    })
  }
  const open = (key: string) => { if (!confirmCatalogoLeave()) return; listPosition.current = window.scrollY; setSelected(key); setGeneration(n => n + 1)
    window.requestAnimationFrame(() => { area.current?.scrollIntoView({ behavior: "smooth", block: "start" }); area.current?.focus() }) }
  const savedProduct = products.find(p => p.productoId === selected)
  return <div className="product-workspace">
    {!selected && <><div className="product-heading"><div><h1 className="product-title">Productos</h1><p className="product-subtitle">{products.length} productos · {configured} con precio configurado</p></div><button type="button" className="product-primary" onClick={() => open("new")}>Agregar producto</button></div><div className="product-panel">
    {!modoCatalogo && <p className="rounded-xl bg-amber-50 p-3 text-sm text-amber-900">Estás preparando el catálogo. Las fichas nuevas no tienen precio ni stock para el circuito de compra anterior y no se publican en ese circuito. En productos existentes, nombre, imágenes y estado son compartidos con la tienda actual; sus cambios se aplican al guardar. El nuevo precio se prepara por separado.</p>}
    <div className="product-filters"><label className="product-label">Buscar por nombre o SKU<input type="search" className="product-field" value={search} onChange={e => { setSearch(e.target.value); setPage(0) }} /></label>
      <label className="product-label">Precio<select className="product-field" value={status} onChange={e => { setStatus(e.target.value); setPage(0) }}><option value="todos">Todos los productos</option><option value="listos">Configurado</option><option value="pendientes">Pendiente</option></select></label><label className="product-label">Publicación<select aria-label="Publicación" className="product-field" value={active} onChange={e => { setActive(e.target.value); setPage(0) }}><option value="todos">Activos e inactivos</option><option value="activos">Activos</option><option value="inactivos">Inactivos</option></select></label></div>
    <div className="product-rows">{visible.length ? visible.slice(currentPage * 12, (currentPage + 1) * 12).map(p => <article key={p.productoId} className="product-row"><div className="product-row-info"><h3 className="product-row-title">{p.nombre}</h3><div className="product-row-meta"><span>SKU {p.sku}</span><span className={`product-status ${modoCatalogo && p.activo && p.calculo.estado === "LISTO" ? "is-published" : p.calculo.estado !== "LISTO" ? "is-pending" : ""}`}>{modoCatalogo && p.activo && p.calculo.estado === "LISTO" ? "Publicado" : !p.activo ? "Inactivo" : "Pendiente"}</span></div><p className="product-row-price">{p.calculo.estado === "LISTO" ? `Precio público: ${dineroCatalogo(p.calculo.precioPublicoArs)}` : p.calculo.pendientes.join(" ")}</p></div><button type="button" className="product-link" onClick={() => open(p.productoId)} data-product-id={p.productoId} aria-label={`Editar ${p.nombre}`}>Editar</button></article>) : <p className="py-4 text-sm text-slate-600">{products.length ? "No hay productos con esos filtros." : "Todavía no hay productos."}</p>}</div>
    <div className="product-pagination"><Pagination page={currentPage} pages={pages} change={setPage} /></div></div></>}
    <div ref={area} tabIndex={-1} className="min-w-0 outline-none">
      {selected && (rubros.isPending || (selected !== "new" && ficha.isPending)) ? <p role="status">Cargando formulario...</p>
        : selected && (rubros.isError || (selected !== "new" && ficha.isError)) ? <div role="alert">{catalogoError(rubros.error ?? ficha.error)} <button type="button" className="underline" onClick={() => { void rubros.refetch(); if (selected !== "new") void ficha.refetch() }}>Reintentar</button></div>
        : selected && <CatalogoProductEditor key={`${selected}-${generation}`} id={id} initial={selected === "new" ? undefined : ficha.data}
          categories={categories} productCategories={rubros.data ?? []} config={config} refresh={refresh} notify={notify} onClose={close} modoCatalogo={modoCatalogo} currentProduct={savedProduct} />}
    </div>
  </div>
}
