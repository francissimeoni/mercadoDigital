import StorefrontProductCard from "./StorefrontProductCard"
import type { StorefrontCatalogoItem } from "../types/storefrontCatalogo"
import type { CatalogReturn } from "../utils/catalogNavigation"
type Props = { items: StorefrontCatalogoItem[]; tipoStorefront: "REVENDEDOR" | "PROVEEDOR"; accentColor?: string; catalogOrigin?: CatalogReturn }
export default function StorefrontProductGrid({ items, tipoStorefront, catalogOrigin }: Props) {
  return <div className="sf-product-grid">{items.map((item, index) => <StorefrontProductCard key={item.productoId || item.productoCanonicoId || item.nombrePublico} item={item} tipoStorefront={tipoStorefront} catalogOrigin={catalogOrigin} priority={index < 4} />)}</div>
}
