import { useRef, useState } from "react"
import type { TenantBranding } from "../../tenant/tenantTypes"
import { resolveStorefrontAssetUrl } from "../utils/storefrontBranding"
import StorefrontImageDialog from "./StorefrontImageDialog"
export default function StorefrontHeroBanner({ branding }: { branding: TenantBranding | null }) {
  const [index, setIndex] = useState(0), [preview, setPreview] = useState(false), [failed, setFailed] = useState("")
  const touch = useRef<{ x: number; y: number } | null>(null), swiped = useRef(false)
  const slides = branding?.portadas?.filter(p => p.imagenUrl?.trim()) ?? []
  const slide = slides[index % Math.max(1, slides.length)] ?? (branding?.heroImagenUrl ? { imagenUrl: branding.heroImagenUrl, titulo: null, subtitulo: null } : null)
  if (!slide) return branding?.heroTitulo || branding?.heroSubtitulo ? <section className="sf-hero-text"><h2>{branding.heroTitulo}</h2>{branding.heroSubtitulo && <p>{branding.heroSubtitulo}</p>}</section> : null
  const src = resolveStorefrontAssetUrl(slide.imagenUrl) ?? slide.imagenUrl
  return <section className="sf-hero" aria-label="Portadas de la tienda" aria-roledescription="carrusel">
    <div className="sf-hero-art" onPointerDown={() => { swiped.current = false }} onKeyDown={() => { swiped.current = false }} onTouchStart={e => { touch.current = { x: e.touches[0].clientX, y: e.touches[0].clientY }; swiped.current = false }} onTouchEnd={e => {
      if (!touch.current || slides.length < 2) return
      const dx = e.changedTouches[0].clientX - touch.current.x, dy = e.changedTouches[0].clientY - touch.current.y
      if (Math.abs(dx) > 50 && Math.abs(dx) > Math.abs(dy)) { swiped.current = true; setIndex((index + (dx < 0 ? 1 : slides.length - 1)) % slides.length) }
      touch.current = null
    }}>
      {failed !== src && <img className="sf-hero-backdrop" src={src} alt="" aria-hidden="true" />}
      <button type="button" className="sf-hero-image" aria-label="Ampliar portada" disabled={failed === src} onClick={() => { if (swiped.current) { swiped.current = false; return }; setPreview(true) }}>{failed === src ? <span className="sf-hero-unavailable">{branding?.heroTitulo || branding?.nombreComercial || "Nuestro catálogo"}</span> : <img src={src} alt={slide.titulo || branding?.heroTitulo || `Portada de ${branding?.nombreComercial || "la tienda"}`} fetchPriority="high" width="1600" height="640" onError={() => setFailed(src)} />}</button>
    </div>
    {(slide.titulo || slide.subtitulo) && <div className="sf-hero-caption"><h2>{slide.titulo}</h2>{slide.subtitulo && <p>{slide.subtitulo}</p>}</div>}
    {slides.length > 1 && <div className="sf-hero-controls"><button type="button" className="sf-icon-button" aria-label="Portada anterior" onClick={() => setIndex((index + slides.length - 1) % slides.length)}>‹</button><span role="status">{index % slides.length + 1} de {slides.length}</span><button type="button" className="sf-icon-button" aria-label="Portada siguiente" onClick={() => setIndex((index + 1) % slides.length)}>›</button></div>}
    {preview && <StorefrontImageDialog src={src} alt={slide.titulo || branding?.nombreComercial || "Portada"} close={() => setPreview(false)} />}
  </section>
}
