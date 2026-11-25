import { useId, useRef } from "react"
import { Link, useLocation, useNavigate } from "react-router-dom"
import type { StorefrontCatalogoItem } from "../types/storefrontCatalogo"
import { campaignSearch, type CatalogReturn } from "../utils/catalogNavigation"
import { precioCatalogo } from "../utils/catalogoPublico"
import StorefrontProductImage from "./StorefrontProductImage"

export default function StorefrontCollection({ title, items, origin, onViewAll, provider }: {
  title: string; items: StorefrontCatalogoItem[]; origin: CatalogReturn; onViewAll: () => void; provider: boolean
}) {
  const id = useId(), rail = useRef<HTMLDivElement>(null), location = useLocation(), navigate = useNavigate()
  if (!items.length) return null
  const move = (direction: number) => rail.current?.scrollBy({ left: direction * rail.current.clientWidth * .8, behavior: matchMedia("(prefers-reduced-motion: reduce)").matches ? "instant" : "smooth" })
  return <section className="sf-collection" aria-labelledby={id}>
    <div className="sf-collection-intro"><span className="sf-eyebrow">Para descubrir</span><h2 id={id}>{title}</h2><button type="button" className="sf-text-button" onClick={onViewAll}>Ver todas <span aria-hidden="true">→</span></button><div className="sf-collection-controls"><button type="button" className="sf-icon-button" aria-label={`${title}: anteriores`} onClick={() => move(-1)}>‹</button><button type="button" className="sf-icon-button" aria-label={`${title}: siguientes`} onClick={() => move(1)}>›</button></div></div>
    <div className="sf-collection-rail" ref={rail}>
      {items.slice(0, 12).map(item => {
        const reference = provider ? item.productoId : item.productoCanonicoId || item.productoId
        const path = `/producto/${reference}${campaignSearch(location.search)}`
        const focusId = `${title}-${reference}`
        return <Link key={reference} id={`product-${focusId}`} className="sf-collection-item" to={path} onClick={e => {
          if (e.button === 0 && !e.metaKey && !e.ctrlKey && !e.shiftKey && !e.altKey) { e.preventDefault(); navigate(path, { state: { catalogReturn: { ...origin, product: focusId, scroll: window.scrollY } } }) }
        }}><div className="sf-collection-image"><StorefrontProductImage src={item.imagenPrincipal} alt="" className="sf-contained-image" /></div><h3>{item.nombrePublico}</h3><p>{precioCatalogo(item)}</p></Link>
      })}
    </div>
  </section>
}
