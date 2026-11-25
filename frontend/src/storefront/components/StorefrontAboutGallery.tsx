import { useRef, useState } from "react"
import type { StorefrontGalleryItem } from "../../tenant/tenantTypes"
import { resolveStorefrontAssetUrl } from "../utils/storefrontBranding"

export default function StorefrontAboutGallery({ items, name }: { items: StorefrontGalleryItem[]; name: string }) {
  const [selected, setSelected] = useState(0)
  const [failed, setFailed] = useState<string | null>(null)
  const thumbs = useRef<HTMLDivElement>(null)
  const touch = useRef<{ x: number; y: number } | null>(null)
  const index = Math.min(selected, items.length - 1), item = items[index]
  if (!item) return null
  const src = resolveStorefrontAssetUrl(item.imagenUrl) ?? item.imagenUrl
  const choose = (value: number) => {
    const next = (value + items.length) % items.length
    setSelected(next)
    const row = thumbs.current, button = row?.children[next] as HTMLElement | undefined
    if (row && button) row.scrollTo({ left: button.offsetLeft - row.offsetLeft - (row.clientWidth - button.clientWidth) / 2, behavior: "instant" })
  }
  return <section className="sf-experience-gallery" aria-label={`Galería de ${name}`} aria-roledescription="carrusel" onKeyDown={event => {
    if (event.key === "ArrowRight" || event.key === "ArrowLeft") { event.preventDefault(); choose(index + (event.key === "ArrowRight" ? 1 : -1)) }
  }}>
    <figure>
      <div className="sf-experience-stage" onTouchStart={event => { const point = event.touches[0]; touch.current = { x: point.clientX, y: point.clientY } }} onTouchEnd={event => {
        const start = touch.current; touch.current = null
        if (!start) return
        const point = event.changedTouches[0], dx = point.clientX - start.x, dy = point.clientY - start.y
        if (Math.abs(dx) > 50 && Math.abs(dx) > Math.abs(dy) * 1.5) choose(index + (dx < 0 ? 1 : -1))
      }} onTouchCancel={() => { touch.current = null }}>
        {failed === src ? <div className="sf-image-fallback">Imagen no disponible</div> : <img key={src} src={src} alt={item.titulo || `Imagen ${index + 1} de ${name}`} onError={() => setFailed(src)} />}
        {items.length > 1 && <><button type="button" className="sf-experience-prev" aria-label="Imagen anterior" onClick={() => choose(index - 1)}>‹</button><button type="button" className="sf-experience-next" aria-label="Imagen siguiente" onClick={() => choose(index + 1)}>›</button></>}
      </div>
      <figcaption aria-live="polite" aria-atomic="true"><div>{item.titulo && <strong>{item.titulo}</strong>}{item.comentario && <p>{item.comentario}</p>}</div><span>{index + 1} / {items.length}</span></figcaption>
    </figure>
    {items.length > 1 && <div ref={thumbs} className="sf-experience-thumbs" aria-label="Seleccionar imagen">{items.map((photo, position) => <button key={photo.id || `${photo.imagenUrl}-${position}`} type="button" aria-label={`Ver imagen ${position + 1}${photo.titulo ? `: ${photo.titulo}` : ""}`} aria-pressed={position === index} onClick={() => choose(position)}><img src={resolveStorefrontAssetUrl(photo.imagenUrl) ?? photo.imagenUrl} alt="" loading="lazy" /></button>)}</div>}
  </section>
}
