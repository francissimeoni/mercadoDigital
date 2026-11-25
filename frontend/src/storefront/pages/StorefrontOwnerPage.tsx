import { Navigate } from "react-router-dom"
import { useTenant } from "../../tenant/useTenant"
import { useStorefrontAuth } from "../auth/useStorefrontAuth"
import { lazy, Suspense } from "react"
// El guard de POP debe instalarse antes que BrowserRouter, también con paneles diferidos.
import "../hooks/useCatalogoDraft"
import { DashboardState } from "../dashboard/ui"
const LegacyStorefrontOwnerPage = lazy(() => import("./LegacyStorefrontOwnerPage"))
const CatalogDashboard = lazy(() => import("../dashboard/CatalogDashboard"))

export default function StorefrontOwnerPage() {
  const tenant = useTenant()
  const { session } = useStorefrontAuth()
  if (tenant.isLoading || tenant.status === "idle" || tenant.status === "loading") return <DashboardState>Cargando panel de la tienda...</DashboardState>
  if (tenant.status !== "resolved" || !tenant.storefront) return <DashboardState error retry={() => void tenant.refresh()}>No se pudo resolver la tienda.</DashboardState>
  if (!session) return <Navigate to="/ingresar" replace />
  if (!session.revendedorId || session.revendedorId !== tenant.revendedorId || !session.roles.some(r => r === "REVENDEDOR" || r === "ADMIN_MD")) return <Navigate to="/" replace />
  return <Suspense fallback={<DashboardState>Cargando panel…</DashboardState>}>{tenant.storefront.modoCatalogo ? <CatalogDashboard key={session.revendedorId} /> : <LegacyStorefrontOwnerPage />}</Suspense>
}
