import { Link, useLocation } from "react-router-dom"
import { useTenant } from "../../tenant/useTenant"
import StorefrontAboutGallery from "../components/StorefrontAboutGallery"
import { buildStorefrontWhatsAppUrl } from "../utils/storefrontContact"
import { trackStorefrontEvent } from "../api/storefrontMetricsApi"
import { catalogLink } from "../utils/catalogNavigation"

export default function StorefrontHomePage() {
  const tenant = useTenant(), location = useLocation(), branding = tenant.branding
  const name = branding?.nombreComercial || "Nuestra tienda"
  const whatsapp = branding?.telefonoComercial ? buildStorefrontWhatsAppUrl(branding.telefonoComercial, name) : null
  const gallery = branding?.quienesGaleria ?? []
  const social = [{ label: "Instagram", href: branding?.instagramUrl }, { label: "Facebook", href: branding?.facebookUrl }, { label: "TikTok", href: branding?.tiktokUrl }, { label: "YouTube", href: branding?.youtubeUrl }].filter(item => item.href && /^https?:\/\//i.test(item.href))
  const hasContact = Boolean(whatsapp || branding?.emailComercial || branding?.direccionComercial || social.length)
  return <div className={`sf-about sf-about-editorial sf-about-showcase${hasContact ? "" : " sf-about-no-contact"}${!branding?.quienesTexto?.trim() && !gallery.length ? " sf-about-compact" : ""}`}>
    <section className="sf-about-intro">
      <div><p className="sf-eyebrow">Quiénes somos</p><h1>{branding?.quienesTitulo || name}</h1>
        {branding?.quienesTexto && <p className="sf-about-copy">{branding.quienesTexto}</p>}
        
        <div className="sf-actions"><Link className="sf-button sf-primary" to={catalogLink(location.search)}>Ver catálogo</Link></div>
      </div>

    </section>
    {gallery.length > 0 && <StorefrontAboutGallery key={gallery.map(item => item.imagenUrl).join("|")} items={gallery} name={name} />}
    {hasContact && <section className="sf-about-contact" aria-labelledby="store-contact-title"><div><h2 id="store-contact-title">Contacto</h2></div><div className="sf-contact-list">
      {whatsapp && <a href={whatsapp} target="_blank" rel="noopener noreferrer" onClick={() => trackStorefrontEvent("CLICK_WHATSAPP", { host: tenant.host })}><span className="sf-contact-label"><ContactIcon kind="chat" />WhatsApp</span><strong>{branding?.telefonoComercial}</strong><span>Escribir a la tienda ↗</span></a>}
      {branding?.emailComercial && <a href={`mailto:${branding.emailComercial}`}><span className="sf-contact-label"><ContactIcon kind="mail" />Correo electrónico</span><strong>{branding.emailComercial}</strong></a>}
      {branding?.direccionComercial && <a href={`https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(branding.direccionComercial)}`} target="_blank" rel="noopener noreferrer"><span className="sf-contact-label"><ContactIcon kind="pin" />Dirección</span><strong>{branding.direccionComercial}</strong><span>Ver en el mapa ↗</span></a>}
    </div>{social.length > 0 && <nav className="sf-social" aria-label="Redes sociales">{social.map(item => <a key={item.label} href={item.href!} target="_blank" rel="noopener noreferrer">{item.label} ↗</a>)}</nav>}</section>}
  </div>
}

function ContactIcon({ kind }: { kind: "chat" | "mail" | "pin" }) { return <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" aria-hidden="true">{kind === "mail" ? <><rect x="3" y="5" width="18" height="14" rx="2"/><path d="m3 6 9 7 9-7"/></> : kind === "chat" ? <path d="M21 11.5a8.5 8.5 0 0 1-12.5 7.5L3 21l1.9-5.5A8.5 8.5 0 1 1 21 11.5Z"/> : <><path d="M19 10c0 5-7 11-7 11S5 15 5 10a7 7 0 1 1 14 0Z"/><circle cx="12" cy="10" r="2.5"/></>}</svg> }
