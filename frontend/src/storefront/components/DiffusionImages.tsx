import { useState } from "react"
import { resolveStorefrontAssetUrl } from "../utils/storefrontBranding"
type Product = { id: string; nombre: string; imagenPrincipalUrl: string | null }
function ShareImage({ product }: { product: Product }) {
  const [failed, setFailed] = useState(false)
  const url = resolveStorefrontAssetUrl(product.imagenPrincipalUrl)
  return <figure className="diffusion-share-image">{url && !failed ? <><img src={url} alt={product.nombre} loading="lazy" onError={() => setFailed(true)} /><a href={url} target="_blank" rel="noopener noreferrer">Abrir imagen original<span className="sr-only"> de {product.nombre}</span></a></> : <p>Imagen no disponible</p>}<figcaption>{product.nombre}</figcaption></figure>
}
export default function DiffusionImages({ products }: { products: Product[] }) {
  if (!products.length) return null
  const images = <div className="diffusion-share-images">{products.map(product => <ShareImage key={`${product.id}:${product.imagenPrincipalUrl}`} product={product} />)}</div>
  return <div className="diffusion-photo-preview">{products.length === 1 ? images : <details><summary>Ver imágenes de los {products.length} productos</summary>{images}</details>}</div>
}
