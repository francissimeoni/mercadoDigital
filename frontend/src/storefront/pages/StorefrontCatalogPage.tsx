import { useEffect, useLayoutEffect, useRef, useState } from "react"
import { useLocation, useNavigationType, useSearchParams } from "react-router-dom"
import StorefrontCatalogoSkeleton from "../components/StorefrontCatalogoSkeleton"
import StorefrontErrorState from "../components/StorefrontErrorState"
import StorefrontHeroBanner from "../components/StorefrontHeroBanner"
import StorefrontProductGrid from "../components/StorefrontProductGrid"
import StorefrontCollection from "../components/StorefrontCollection"
import StorefrontProductImage from "../components/StorefrontProductImage"
import { useStorefrontCatalogo } from "../hooks/useStorefrontCatalogo"
import { useTenant } from "../../tenant/useTenant"
import { compararPrecios } from "../utils/catalogoPublico"
import { campaignParams, matchesSearch, type CatalogReturn } from "../utils/catalogNavigation"
const positions = new Map<string, number>()
export default function StorefrontCatalogPage() {
  const [filtersOpen, setFiltersOpen] = useState(false)
  const [params, setParams] = useSearchParams(), location = useLocation(), navigation = useNavigationType()
  const tenant = useTenant()
  const target = tenant.tipoStorefront === "PROVEEDOR" && tenant.proveedorId ? { tipo: "PROVEEDOR" as const, proveedorId: tenant.proveedorId, host: tenant.host } : tenant.revendedorId ? { tipo: "REVENDEDOR" as const, revendedorId: tenant.revendedorId, host: tenant.host } : null
  const catalog = useStorefrontCatalogo(target)
  const ready = Boolean(target) && (tenant.status === "resolved" || Boolean(tenant.storefront))
  const products = ready ? catalog.data ?? [] : []
  const mode = Boolean(tenant.storefront?.modoCatalogo || products.some(p => p.modoCatalogo))
  const query = params.get("q") ?? "", category = params.get("categoria") ?? "", sort = params.get("sort") ?? "nombre"
  const stock = !mode && params.get("stock") === "1"
  const collection = ["promocion", "novedad"].includes(params.get("coleccion") ?? "") ? params.get("coleccion") : ""
  const list = (params.get("lista") ?? "").split(",").map(x => x.trim()).filter(Boolean)
  const categories = [...new Set(products.map(p => p.categoriaProducto?.trim() || "Otros"))].sort((a, b) => a.localeCompare(b, "es"))
  const visible = products.filter(p => (!list.length || list.includes(p.productoId || p.productoCanonicoId || "")) && matchesSearch(p.nombrePublico, query) && (!category || (p.categoriaProducto?.trim() || "Otros") === category) && (!stock || (p.stockDisponible ?? 0) > 0))
    .filter(p => !collection || (collection === "promocion" ? p.promocion : p.novedad))
    .sort((a, b) => (sort === "precio-asc" ? compararPrecios(a, b) : sort === "precio-desc" ? compararPrecios(b, a) : sort === "stock-desc" && !mode ? (b.stockDisponible ?? 0) - (a.stockDisponible ?? 0) : 0) || a.nombrePublico.localeCompare(b.nombrePublico, "es"))
  const count = Number(params.get("ver")), limit = Number.isSafeInteger(count) && count >= 24 ? Math.min(count, 10000) : 24
  const update = (changes: Record<string, string>) => {
    const next = new URLSearchParams(params); next.delete("ver")
    for (const [key, value] of Object.entries(changes)) { if (value) next.set(key, value); else next.delete(key) }
    setParams(next)
  }
  const restore = location.state?.catalogRestore as CatalogReturn | undefined
  const positionKey = `${tenant.host}:${location.key}`, restored = useRef("")
  useLayoutEffect(() => {
    if (!ready || catalog.isPending || restored.current === positionKey) return
    restored.current = positionKey
    const y = restore?.scroll ?? (navigation === "POP" ? positions.get(positionKey) ?? 0 : 0)
    const frame = requestAnimationFrame(() => { window.scrollTo(0, y); if (restore?.product) document.getElementById(`product-${restore.product}`)?.focus({ preventScroll: true }) })
    return () => cancelAnimationFrame(frame)
  }, [ready, catalog.isPending, positionKey, restore, navigation])
  useEffect(() => {
    const save = () => { positions.set(positionKey, window.scrollY); if (positions.size > 100) positions.delete(positions.keys().next().value!) }
    window.addEventListener("scroll", save, { passive: true }); return () => window.removeEventListener("scroll", save)
  }, [positionKey])
  const origin: CatalogReturn = { path: location.pathname + location.search, visible: limit, scroll: 0 }
  const active = Boolean(query || category || stock || collection || (sort !== "nombre" && !(mode && sort === "stock-desc")))
  const landing = !active && !list.length
  const collectionTitle = collection === "promocion" ? "Promociones" : "Novedades"
  const clear = () => { setParams(campaignParams(location.search)); setFiltersOpen(false) }
  return <div className="sf-catalog">
    {landing && <StorefrontHeroBanner branding={tenant.branding} />}
    {landing && !catalog.isError && <StorefrontCollection title="Promociones" items={products.filter(p => p.promocion)} origin={origin} provider={tenant.tipoStorefront === "PROVEEDOR"} onViewAll={() => update({ coleccion: "promocion" })} />}
    {landing && categories.length > 0 && <section className="sf-category-section" aria-labelledby="categories-title"><h2 id="categories-title">Explorá por categoría</h2><nav className="sf-category-tiles" aria-label="Categorías de productos">{categories.map(name => {
      const image = products.find(p => (p.categoriaProducto?.trim() || "Otros") === name && p.imagenPrincipal)?.imagenPrincipal ?? null
      return <button key={name} type="button" onClick={() => update({ categoria: name })}><span className="sf-category-image"><StorefrontProductImage src={image} alt="" className="sf-contained-image" /></span><span>{name}</span></button>
    })}</nav></section>}
    {landing && !catalog.isError && <StorefrontCollection title="Novedades" items={products.filter(p => p.novedad)} origin={origin} provider={tenant.tipoStorefront === "PROVEEDOR"} onViewAll={() => update({ coleccion: "novedad" })} />}
    <section id="catalog-results" tabIndex={-1} className="sf-results" aria-labelledby="catalog-title" aria-busy={catalog.isPending || tenant.isLoading}>
      <div className="sf-catalog-toolbar"><div><h1 id="catalog-title">{list.length ? "Selección compartida" : query ? `Resultados para “${query}”` : category || (collection ? collectionTitle : "Nuestros productos")}</h1><p role="status" aria-live="polite">{catalog.isPending || tenant.isLoading ? "Cargando productos…" : `${visible.length} producto${visible.length === 1 ? "" : "s"}${active ? " encontrados" : ""}`}</p></div>
        <div className="sf-toolbar-actions"><button className="sf-button" type="button" aria-expanded={filtersOpen} aria-controls="catalog-filters" onClick={() => setFiltersOpen(!filtersOpen)}>Filtros{category || stock ? " · activos" : ""}</button><label className="sf-sort"><span className="sr-only">Ordenar productos</span><select aria-label="Ordenar productos" value={mode && sort === "stock-desc" ? "nombre" : sort} onChange={e => update({ sort: e.target.value === "nombre" ? "" : e.target.value })}><option value="nombre">Ordenar: nombre</option><option value="precio-asc">Menor precio</option><option value="precio-desc">Mayor precio</option>{!mode && <option value="stock-desc">Mayor disponibilidad</option>}</select></label></div>
      </div>
      <div id="catalog-filters" className="sf-filter-panel" hidden={!filtersOpen}><label>Categoría<select value={category} onChange={e => update({ categoria: e.target.value })}><option value="">Todas las categorías</option>{categories.map(c => <option key={c}>{c}</option>)}</select></label>{!mode && <label className="sf-check"><input type="checkbox" checked={stock} onChange={e => update({ stock: e.target.checked ? "1" : "" })} />Solo disponibles</label>}<button type="button" className="sf-text-button" disabled={!active} onClick={clear}>Limpiar filtros y búsqueda</button></div>
      {active && <div className="sf-active-filters" aria-label="Filtros activos">{query && <button type="button" onClick={() => update({ q: "" })}>Búsqueda: {query} ×</button>}{category && <button type="button" onClick={() => update({ categoria: "" })}>{category} ×</button>}{stock && <button type="button" onClick={() => update({ stock: "" })}>Solo disponibles ×</button>}</div>}
      {collection && <div className="sf-active-filters"><button type="button" onClick={() => update({ coleccion: "" })}>{collectionTitle} ×</button></div>}
      {list.length > 0 && <p className="sf-list-notice">Esta selección{params.get("asesor") ? ` fue compartida por ${params.get("asesor")}` : " contiene los productos del enlace"}.</p>}
      {tenant.isLoading || catalog.isLoading ? <StorefrontCatalogoSkeleton /> : catalog.isError ? <StorefrontErrorState title="No se pudo cargar el catálogo" message="Reintentá en unos segundos." actionLabel="Reintentar" onAction={() => void catalog.refetch()} /> : ready && <>
        {visible.length ? <StorefrontProductGrid items={visible.slice(0, limit)} tipoStorefront={tenant.tipoStorefront === "PROVEEDOR" ? "PROVEEDOR" : "REVENDEDOR"} catalogOrigin={origin} /> : <div className="sf-empty" role="status"><h2>{products.length ? "No encontramos productos" : "Todavía no hay productos publicados"}</h2><p>{products.length ? "Probá con otro nombre o quitá alguno de los filtros." : "Podés consultar a la tienda por sus canales de contacto."}</p>{active && <button className="sf-button" onClick={clear}>Limpiar búsqueda y filtros</button>}</div>}
        {visible.length > limit && <div className="sf-load-more"><p>Mostrando {Math.min(limit, visible.length)} de {visible.length} productos</p><button className="sf-button" type="button" onClick={() => { const next = new URLSearchParams(params); next.set("ver", String(limit + 24)); setParams(next, { replace: true, state: { catalogRestore: { ...origin, scroll: window.scrollY } } }) }}>Ver más productos</button></div>}
      </>}
    </section>
  </div>
}
