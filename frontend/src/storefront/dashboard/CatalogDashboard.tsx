import { useEffect, useRef, useState } from "react"
import { Link, useSearchParams } from "react-router-dom"
import { useQuery, useQueryClient } from "@tanstack/react-query"
import { useTenant } from "../../tenant/useTenant"
import { useStorefrontAuth } from "../auth/useStorefrontAuth"
import { getStorefrontContent } from "../api/storefrontOwnerApi"
import { applyStorefrontDocumentBranding } from "../utils/storefrontBranding"
import { confirmCatalogoLeave } from "../hooks/useCatalogoDraft"
import CatalogoAdminSection from "../components/CatalogoAdminSection"
import OwnerContactsSection from "./OwnerContactsSection"
import OwnerDiffusionSection from "./OwnerDiffusionSection"
import OwnerContentSection from "./OwnerContentSection"
import OwnerMetricsSection from "./OwnerMetricsSection"
import { DashboardHeading, DashboardState } from "./ui"

const sections = [
  { id: "productos", label: "Productos", description: "Fichas y publicación" },
  { id: "precios", label: "Precios y envíos", description: "Dólar y costos de tu tienda" },
  { id: "asesores", label: "Asesores y contacto", description: "Quién recibe las compras" },
  { id: "difusion", label: "Difusión", description: "Productos y listas para compartir" },
  { id: "cuenta", label: "Mi página", description: "Imagen y contenido de tu tienda" },
  { id: "metricas", label: "Métricas", description: "Visitas e interés en el catálogo" },
] as const
type Section = typeof sections[number]["id"]
function resolveSection(value: string | null): Section { if (value === "catalogo") return "precios"; if (value === "red") return "asesores"; return sections.find(s => s.id === value)?.id ?? "productos" }

export default function CatalogDashboard() {
  const tenant = useTenant(), auth = useStorefrontAuth(), client = useQueryClient()
  const id = auth.session!.revendedorId!
  const [params, setParams] = useSearchParams()
  const section = resolveSection(params.get("tab"))
  const [message, setMessage] = useState("")
  const timer = useRef<ReturnType<typeof setTimeout> | null>(null)
  const content = useQuery({ queryKey: ["owner-storefront-content", id], queryFn: () => getStorefrontContent(id), enabled: section === "cuenta", refetchOnWindowFocus: false, refetchOnReconnect: false })
  useEffect(() => { if (tenant.branding) applyStorefrontDocumentBranding(tenant.branding) }, [tenant.branding])
  useEffect(() => { if (params.get("tab") !== section || params.has("pedido")) setParams({ tab: section }, { replace: true }) }, [params, section, setParams])
  useEffect(() => () => { if (timer.current) clearTimeout(timer.current) }, [])
  const notify = (text: string) => { setMessage(text); if (timer.current) clearTimeout(timer.current); timer.current = setTimeout(() => setMessage(""), 6000) }
  const navigate = (value: Section) => { if (value === section || !confirmCatalogoLeave()) return; setParams({ tab: value }); setMessage(""); window.scrollTo({ top: 0 }) }
  return <div className="catalog-dashboard min-h-screen bg-slate-50 text-slate-950">
    <a href="#dashboard-main" className="sr-only focus:not-sr-only focus:block focus:p-3">Ir al contenido del panel</a>
    <header className="border-b border-slate-200 bg-white"><div className="mx-auto flex max-w-[1600px] flex-wrap items-center justify-between gap-3 px-4 py-4 sm:px-6"><div className="min-w-0"><p className="text-xs font-medium text-slate-500">Mi tienda</p><p className="mt-1 break-words text-base font-semibold">{tenant.branding?.nombreComercial ?? "Mi tienda"}</p></div><div className="flex flex-wrap gap-2"><Link to="/" className="dashboard-account-action">Ver catálogo</Link><button className="dashboard-account-action" onClick={() => { if (confirmCatalogoLeave()) { auth.logout(); window.location.assign("/") } }}>Cerrar sesión</button></div></div></header>
    <div className="mx-auto grid max-w-[1600px] items-start gap-5 px-4 py-5 sm:px-6 lg:grid-cols-[224px_minmax(0,1fr)] lg:gap-7 lg:py-7">
      <nav aria-label="Secciones del panel" className="min-w-0 lg:sticky lg:top-6"><label className="block text-sm font-medium lg:hidden">Sección del panel<select aria-label="Sección del panel" className="mt-2 min-h-12 w-full rounded-lg border border-slate-300 bg-white px-3 text-base" value={section} onChange={e => navigate(e.target.value as Section)}>{sections.map(s => <option key={s.id} value={s.id}>{s.label}</option>)}</select></label><div className="hidden space-y-1 lg:block">{sections.map(s => <button key={s.id} type="button" aria-current={section === s.id ? "page" : undefined} onClick={() => navigate(s.id)} className="dashboard-nav-item w-full text-left">{s.label}</button>)}</div></nav>
      <main id="dashboard-main" className="min-w-0 space-y-5">
        {message && <div role="status" className="rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-900">{message}</div>}
        {section === "productos" && <><CatalogoAdminSection id={id} modoCatalogo view="products" notify={notify} /></>}
        {section === "precios" && <><DashboardHeading title="Precios y envíos" /><CatalogoAdminSection id={id} modoCatalogo view="pricing" notify={notify} /></>}
        {section === "asesores" && <OwnerContactsSection id={id} notify={notify} />}
        {section === "difusion" && <OwnerDiffusionSection id={id} host={tenant.storefront?.dominio ?? tenant.host ?? ""} storeName={tenant.branding?.nombreComercial ?? "Mi tienda"} slogan={tenant.branding?.heroSubtitulo ?? ""} notify={notify} />}
        {section === "cuenta" && (content.isPending ? <DashboardState>Cargando configuración de tu página...</DashboardState> : content.isError ? <DashboardState error retry={() => void content.refetch()}>No se pudo cargar la página.</DashboardState> : <OwnerContentSection modoCatalogo id={id} initial={content.data} name={auth.session!.nombre} email={auth.session!.email} notify={notify} refresh={async () => { await client.invalidateQueries({ queryKey: ["owner-storefront-content", id] }); await tenant.refresh() }} />)}
        {section === "metricas" && <OwnerMetricsSection id={id} catalogo />}
      </main>
    </div>
  </div>
}
