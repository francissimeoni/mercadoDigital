import { Link, useLocation } from "react-router-dom"
import type { TenantBranding } from "../../tenant/tenantTypes"
import { buildStorefrontWhatsAppUrl } from "../utils/storefrontContact"
import { catalogLink } from "../utils/catalogNavigation"
export default function StorefrontFooter({ branding }: { branding: TenantBranding | null; modoCatalogo?: boolean }) {
  const location = useLocation(), about = location.pathname === "/quienes-somos"
  const whatsapp = branding?.telefonoComercial ? buildStorefrontWhatsAppUrl(branding.telefonoComercial, branding.nombreComercial) : null
  return <footer className="sf-footer"><div className="sf-footer-inner"><div><p className="sf-footer-name">{branding?.nombreComercial || "Tienda online"}</p>{branding?.footerTexto && <p>{branding.footerTexto}</p>}</div><nav aria-label="Información de la tienda"><Link to={catalogLink(location.search)}>Catálogo</Link><Link to={`/quienes-somos${location.search}`}>Quiénes somos y contacto</Link>{!about && <>{whatsapp && <a href={whatsapp} target="_blank" rel="noopener noreferrer">WhatsApp: {branding?.telefonoComercial}</a>}{branding?.emailComercial && <a href={`mailto:${branding.emailComercial}`}>{branding.emailComercial}</a>}{branding?.direccionComercial && <span>{branding.direccionComercial}</span>}</>}</nav></div></footer>
}
