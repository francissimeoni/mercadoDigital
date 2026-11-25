import { useEffect, useRef, useState } from "react"
import { Link, useLocation } from "react-router-dom"
import type { TenantBranding } from "../../tenant/tenantTypes"
import { resolveStorefrontAssetUrl } from "../utils/storefrontBranding"
import { useStorefrontAuth } from "../auth/useStorefrontAuth"
import StorefrontSearch from "./StorefrontSearch"
import { catalogLink } from "../utils/catalogNavigation"
type Props = { branding: TenantBranding | null; isLoading?: boolean; isReady?: boolean; modoCatalogo?: boolean }
export default function StorefrontHeader({ branding, isReady = false }: Props) {
  const [open, setOpen] = useState(false)
  const menu = useRef<HTMLButtonElement>(null), location = useLocation()
  const { session } = useStorefrontAuth()
  const accountPath = session?.roles.includes("REVENDEDOR") ? "/mi-tienda" : session?.roles.includes("CLIENTE") ? "/mi-cuenta" : "/ingresar"
  const accountLabel = session?.roles.includes("REVENDEDOR") ? "Mi tienda" : session ? "Mi cuenta" : "Acceso"
  const logo = isReady ? resolveStorefrontAssetUrl(branding?.logoUrl) : null
  const name = branding?.nombreComercial || "Tienda online"
  const isCatalog = location.pathname === "/" || location.pathname === "/catalogo"
  useEffect(() => {
    const close = (event: KeyboardEvent) => { if (event.key === "Escape") { setOpen(false); menu.current?.focus() } }
    if (open) document.addEventListener("keydown", close)
    return () => document.removeEventListener("keydown", close)
  }, [open])
  const links = <>
    <Link to={catalogLink(location.search)} aria-current={isCatalog ? "page" : undefined} onClick={() => setOpen(false)}>Catálogo</Link>
    <Link to={`/quienes-somos${location.search}`} aria-current={location.pathname === "/quienes-somos" ? "page" : undefined} onClick={() => setOpen(false)}>Quiénes somos</Link>
    <Link className="sf-account" to={accountPath} onClick={() => setOpen(false)}>{accountLabel}</Link>
  </>
  return <header className="sf-header"><div className="sf-header-inner">
    <Link className="sf-brand" to={catalogLink(location.search)} aria-label={`${name}, catálogo`}>
      {logo ? <img src={logo} alt={name} width="240" height="48" /> : <span>{isReady ? name : "Cargando tienda…"}</span>}
    </Link>
    <StorefrontSearch />
    <nav className="sf-desktop-nav" aria-label="Navegación principal">{links}</nav>
    <button ref={menu} className="sf-icon-button sf-menu-toggle" type="button" aria-label={open ? "Cerrar menú" : "Abrir menú"} aria-expanded={open} aria-controls="storefront-menu" onClick={() => setOpen(!open)}><svg viewBox="0 0 24 24" aria-hidden="true"><path d={open ? "m6 6 12 12M6 18 18 6" : "M4 6h16M4 12h16M4 18h16"} /></svg></button>
    <nav id="storefront-menu" className="sf-mobile-nav" aria-label="Navegación móvil" hidden={!open}>{links}</nav>
  </div></header>
}
