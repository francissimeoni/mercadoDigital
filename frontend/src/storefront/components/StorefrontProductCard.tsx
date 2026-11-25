import { Link, useLocation, useNavigate } from "react-router-dom"
import { useTenant } from "../../tenant/useTenant"
import { precioCatalogo } from "../utils/catalogoPublico"
import { campaignSearch, type CatalogReturn } from "../utils/catalogNavigation"
import CatalogoContactButton from "./CatalogoContactButton"
import StorefrontProductImage from "./StorefrontProductImage"
import type { StorefrontCatalogoItem } from "../types/storefrontCatalogo"
type Props = { item: StorefrontCatalogoItem; tipoStorefront: "REVENDEDOR" | "PROVEEDOR"; accentColor?: string; catalogOrigin?: CatalogReturn; priority?: boolean }
export default function StorefrontProductCard({ item, tipoStorefront, catalogOrigin, priority }: Props) {
  const tenant = useTenant(), location = useLocation(), navigate = useNavigate()
  const reference = tipoStorefront === "PROVEEDOR" ? item.productoId : item.productoCanonicoId || item.productoId
  const search = campaignSearch(location.search), path = `/producto/${reference}${search}`
  return <article className="sf-product-card">
    <Link id={`product-${reference}`} className="sf-card-link" to={path} onClick={e => {
      if (catalogOrigin && e.button === 0 && !e.metaKey && !e.ctrlKey && !e.shiftKey && !e.altKey) { e.preventDefault(); navigate(path, { state: { catalogReturn: { ...catalogOrigin, product: reference, scroll: window.scrollY } } }) }
    }}>
      <div className="sf-card-image"><StorefrontProductImage src={item.imagenPrincipal} alt={item.nombrePublico} className="sf-contained-image" priority={priority} />{(item.promocion || item.novedad) && <span className="sf-product-tag">{item.promocion ? "Promoción" : "Novedad"}</span>}</div>
      <h2 className="sf-card-name">{item.nombrePublico}</h2>
    </Link>
    <p className="sf-card-price">{precioCatalogo(item)}</p>
    {!item.modoCatalogo && <p className="sf-card-availability">{(item.stockDisponible ?? 0) > 0 ? `Stock ${item.stockDisponible}` : "Sin stock"}</p>}
    <div className="sf-card-action">{item.modoCatalogo ? <CatalogoContactButton compacto productoId={reference} host={tenant.host ?? ""} precioMostrado={item.precioPublicoArs ?? ""} /> : <Link className="sf-button" to={`/producto/${reference}/checkout${search}`}>Comprar</Link>}</div>
  </article>
}
