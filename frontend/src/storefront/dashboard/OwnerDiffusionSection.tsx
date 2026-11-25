import { useRef, useState } from "react"
import { useQuery } from "@tanstack/react-query"
import { listarProductos, listarCategorias, listarAsesores, type ProductoPropio, type Asesor } from "../api/storefrontOwnerApi"
import { getStorefrontCatalogo } from "../api/storefrontCatalogoApi"
import type { StorefrontCatalogoItem } from "../types/storefrontCatalogo"
import { formatPrecioArs } from "../utils/catalogoPublico"
import DiffusionImages from "../components/DiffusionImages"
import ProductThumbnail from "../components/ProductThumbnail"
import { normalizeCommercialPhone } from "./whatsapp"
import { DashboardHeading, DashboardState, Pagination, field, button, secondary } from "./ui"

type Props = { id: string; host: string; storeName: string; slogan: string; notify: (message: string) => void }
type ShareProduct = ProductoPropio & { precioPublicoArs: string }
const usableAdvisors = (items: Asesor[]) => items.filter(a => a.activo && normalizeCommercialPhone(a.whatsappComercial))
function combine(own: ProductoPropio[], published: StorefrontCatalogoItem[]): ShareProduct[] {
  const byId = new Map(own.map(p => [p.id, p]))
  return published.flatMap(p => {
    const original = byId.get(p.productoId)
    if (!original || !p.precioPublicoArs || !/^\d+\.\d{2}$/.test(p.precioPublicoArs) || BigInt(p.precioPublicoArs.replace(".", "")) <= 0n) return []
    const photo = p.imagenPrincipal || original.imagenPrincipalUrl || [...original.media].filter(m => m.tipoMedia === "IMAGEN").sort((a, b) => Number(b.esPrincipal) - Number(a.esPrincipal) || a.orden - b.orden)[0]?.url || null
    return [{ ...original, imagenPrincipalUrl: photo, precioPublicoArs: p.precioPublicoArs }]
  })
}
export default function OwnerDiffusionSection({ id, host, storeName, slogan, notify }: Props) {
  const [search, setSearch] = useState("")
  const [category, setCategory] = useState("")
  const [advisorId, setAdvisorId] = useState("")
  const [selected, setSelected] = useState<string[]>([])
  const [page, setPage] = useState(0)
  const [actionError, setActionError] = useState("")
  const [acting, setActing] = useState(false)
  const actionLock = useRef(false)
  const own = useQuery({ queryKey: ["owner-products", id], queryFn: () => listarProductos(id) })
  const published = useQuery({ queryKey: ["storefront-catalogo", { host }], queryFn: () => getStorefrontCatalogo({ tipo: "REVENDEDOR", revendedorId: id, host }), refetchInterval: 60_000 })
  const categories = useQuery({ queryKey: ["owner-categories", id], queryFn: () => listarCategorias(id) })
  const advisors = useQuery({ queryKey: ["owner-advisors", id], queryFn: () => listarAsesores(id) })
  const queries = [own, published, categories, advisors]
  const ready = usableAdvisors(advisors.data ?? [])
  const advisor = ready.find(a => a.id === advisorId)
  const products = combine(own.data ?? [], published.data ?? [])
  const visible = products.filter(p => `${p.nombre} ${p.sku}`.toLocaleLowerCase("es").includes(search.toLocaleLowerCase("es")) && (!category || p.categoriaProductoId === category))
  const pages = Math.max(1, Math.ceil(visible.length / 12)), currentPage = Math.min(page, pages - 1)
  const chosen = selected.flatMap(key => products.find(p => p.id === key) ?? [])
  const shareUrl = (items: ShareProduct[], contact?: Asesor) => {
    const product = items.length === 1 ? items[0] : null
    const url = new URL(product ? `/api/public/share/producto/${product.id}` : "/catalogo", location.origin)
    url.searchParams.set("storefrontHost", host)
    if (product) url.searchParams.set("v", `og2-${product.precioPublicoArs.replace(".", "-")}-${product.imagenPrincipalUrl ?? ""}`)
    else if (items.length) url.searchParams.set("lista", items.map(p => p.id).join(","))
    if (contact) { url.searchParams.set("asesorId", contact.id); url.searchParams.set("asesor", contact.nombre) }
    return url.toString()
  }
  const message = (items: ShareProduct[], contact?: Asesor) => {
    if (items.length === 1) return [items[0].nombre, `Precio: ${formatPrecioArs(items[0].precioPublicoArs)} ARS`, slogan, "Consultá y coordiná tu compra por WhatsApp:", shareUrl(items, contact)].filter(Boolean).join("\n")
    const cents = items.reduce((sum, p) => sum + BigInt(p.precioPublicoArs.replace(".", "")), 0n).toString().padStart(3, "0")
    return [storeName, ...items.map(p => `${p.nombre} — ${formatPrecioArs(p.precioPublicoArs)}`), `Total: ${formatPrecioArs(cents.slice(0, -2) + "." + cents.slice(-2))} ARS`, contact ? `Te asesora: ${contact.nombre}` : "", shareUrl(items, contact)].filter(Boolean).join("\n")
  }
  const act = async (kind: "whatsapp" | "message" | "link") => {
    if (actionLock.current || !chosen.length) return
    actionLock.current = true; setActing(true); setActionError("")
    const popup = kind === "whatsapp" ? window.open("about:blank", "_blank") : null
    if (popup) popup.opener = null
    try {
      const [a, b, c] = await Promise.all([own.refetch(), published.refetch(), advisors.refetch()])
      if (a.isError || b.isError || c.isError || !a.data || !b.data || !c.data) throw new Error("No pudimos verificar los datos actuales. Reintentá antes de compartir.")
      const fresh = combine(a.data, b.data)
      const items = selected.flatMap(key => fresh.find(p => p.id === key) ?? [])
      if (items.length !== selected.length) throw new Error("Un producto seleccionado ya no está disponible para compartir. Revisá la selección o vaciala.")
      const contact = usableAdvisors(c.data).find(item => item.id === advisorId)
      if (advisorId && !contact) { setAdvisorId(""); throw new Error("El asesor ya no está disponible. Elegí otro contacto antes de compartir.") }
      if (message(items, contact) !== message(chosen, advisor)) throw new Error("Actualizamos los datos de la selección. Revisá la vista previa y volvé a compartir.")
      if (kind === "whatsapp") {
        if (!popup) throw new Error("El navegador bloqueó la ventana. Permití abrir WhatsApp o copiá el mensaje.")
        popup.location.href = `https://wa.me/?text=${encodeURIComponent(message(items, contact))}`
      } else { await navigator.clipboard.writeText(kind === "link" ? shareUrl(items, contact) : message(items, contact)); notify(kind === "link" ? "Enlace copiado." : "Mensaje copiado.") }
    } catch (error) { popup?.close(); setActionError(error instanceof Error ? error.message : "No se pudo compartir. Reintentá.") }
    finally { actionLock.current = false; setActing(false) }
  }
  const filters = <><label>Rubro<select aria-label="Rubro" className={field} value={category} onChange={e => { setCategory(e.target.value); setPage(0) }}><option value="">Todos los rubros</option>{categories.data?.map(c => <option key={c.id} value={c.id}>{c.nombre}</option>)}</select></label><label>Asesor del enlace<select aria-label="Asesor del enlace" className={field} value={advisor?.id ?? ""} onChange={e => setAdvisorId(e.target.value)}><option value="">Sin asesor asignado</option>{ready.map(a => <option key={a.id} value={a.id}>{a.nombre}</option>)}</select></label></>
  return <section className="dashboard-section"><DashboardHeading title="Difusión" />
    {queries.some(q => q.isPending) ? <DashboardState>Cargando productos y contactos…</DashboardState> : queries.some(q => q.isError) ? <DashboardState error retry={() => queries.forEach(q => void q.refetch())}>No se pudieron actualizar los datos para compartir.</DashboardState> : <>
      <div className="dashboard-panel diffusion-filters"><label>Buscar por nombre o SKU<input type="search" className={field} value={search} onChange={e => { setSearch(e.target.value); setPage(0) }} /></label><div className="diffusion-desktop-filters">{filters}</div><details className="diffusion-mobile-filters"><summary>Filtros y asesor{category || advisor ? " · activos" : ""}</summary><div className="dashboard-fields">{filters}</div></details></div>
      {selected.length > 0 && <div className="diffusion-mobile-selection"><span>{selected.length} seleccionado{selected.length === 1 ? "" : "s"}</span><button type="button" className="dashboard-link" onClick={() => { const target = document.getElementById("diffusion-preview"); target?.scrollIntoView({ behavior: "smooth", block: "start" }); target?.focus({ preventScroll: true }) }}>Ver mensaje</button></div>}<div className="diffusion-layout"><div className="diffusion-products"><p className="dashboard-help">{visible.length} productos publicados · {selected.length} seleccionados</p><div className="dashboard-panel diffusion-list">{visible.slice(currentPage * 12, (currentPage + 1) * 12).map(p => <label key={p.id} className="diffusion-row"><input type="checkbox" aria-label={`Seleccionar ${p.nombre}`} checked={selected.includes(p.id)} disabled={acting || (!selected.includes(p.id) && selected.length >= 20)} onChange={() => setSelected(ids => ids.includes(p.id) ? ids.filter(key => key !== p.id) : [...ids, p.id])} /><ProductThumbnail src={p.imagenPrincipalUrl} /><span className="diffusion-product-name">{p.nombre}</span><span className="diffusion-price">{formatPrecioArs(p.precioPublicoArs)}</span></label>)}{!visible.length && <p className="dashboard-empty">No hay productos publicados con estos filtros.</p>}</div><Pagination page={currentPage} pages={pages} change={setPage} /><p className="dashboard-help">Máximo 20 productos.</p></div>
      <aside id="diffusion-preview" tabIndex={-1} className="dashboard-panel diffusion-preview" aria-label="Vista previa para compartir"><div className="dashboard-section-heading"><h2>{!selected.length ? "Vista previa" : chosen.length === 1 ? "Compartir producto" : "Compartir lista"}</h2>{selected.length > 0 && <button type="button" className="dashboard-link" disabled={acting} onClick={() => setSelected([])}>Vaciar</button>}</div>{!selected.length ? <p className="dashboard-empty">Seleccioná productos para compartir.</p> : <><p className="dashboard-help">{selected.length} seleccionado{selected.length === 1 ? "" : "s"}{advisor ? ` · ${advisor.nombre}` : ""}</p><DiffusionImages products={chosen} /><p className="diffusion-message">{message(chosen, advisor)}</p><div className="diffusion-share-actions"><button type="button" className={button} disabled={acting || chosen.length !== selected.length} onClick={() => void act("whatsapp")}>{acting ? "Verificando…" : "Compartir por WhatsApp"}</button><button type="button" className={secondary} disabled={acting} onClick={() => void act("message")}>Copiar mensaje</button><button type="button" className="dashboard-link" disabled={acting} onClick={() => void act("link")}>Copiar enlace</button></div></>}{actionError && <DashboardState error>{actionError}</DashboardState>}</aside></div>
    </>}
  </section>
}
