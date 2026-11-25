import { useEffect, type ReactNode } from "react"
import { Outlet, useLocation } from "react-router-dom"
import StorefrontDocumentBranding from "../components/StorefrontDocumentBranding"
import StorefrontFooter from "../components/StorefrontFooter"
import StorefrontHeader from "../components/StorefrontHeader"
import StorefrontWhatsAppFab from "../components/StorefrontWhatsAppFab"
import StorefrontEmptyState from "../components/StorefrontEmptyState"
import StorefrontErrorState from "../components/StorefrontErrorState"
import StorefrontUnavailableState from "../components/StorefrontUnavailableState"
import { useTenant } from "../../tenant/useTenant"
import { trackStorefrontVisit } from "../api/storefrontMetricsApi"

export default function PublicStoreLayout({ children }: { children?: ReactNode }) {
  const tenant = useTenant()
  const location = useLocation()
  const productCatalog = Boolean(tenant.storefront?.modoCatalogo && location.pathname.startsWith("/producto/"))
  const isStorefrontReady = tenant.status === "resolved" || Boolean(tenant.storefront)

  useEffect(() => {
    if (tenant.status === "resolved" && tenant.tipoStorefront === "REVENDEDOR") {
      trackStorefrontVisit(tenant.host)
    }
  }, [tenant.host, tenant.status, tenant.tipoStorefront])

  return (
    <div className="sf-storefront" style={{ "--sf-brand": /^#[0-9a-f]{6}$/i.test(tenant.branding?.colorPrimario || "") ? tenant.branding!.colorPrimario! : "#0f172a" } as React.CSSProperties}>
      <a href="#storefront-main" className="sf-skip">Saltar al contenido</a>
      <StorefrontDocumentBranding branding={tenant.branding} isReady={isStorefrontReady} />
      <StorefrontHeader branding={tenant.branding} isLoading={tenant.isLoading} isReady={isStorefrontReady} modoCatalogo={tenant.storefront?.modoCatalogo} />

      <main id="storefront-main" tabIndex={-1} className={`sf-main${productCatalog ? " sf-main-product" : ""}`}>
        {tenant.isLoading ? (
          <section className="rounded-3xl border border-slate-200 bg-white p-10 shadow-sm">
            <div className="space-y-4">
              <div className="h-4 w-24 animate-pulse rounded bg-slate-200" />
              <div className="h-10 w-2/3 animate-pulse rounded bg-slate-200" />
              <div className="h-5 w-full animate-pulse rounded bg-slate-200" />
              <div className="h-5 w-4/5 animate-pulse rounded bg-slate-200" />
            </div>
          </section>
        ) : tenant.status === "local_unresolved" ? (
          <StorefrontEmptyState
            title="Tienda online local no resuelta"
            message={tenant.error?.message || "Define VITE_STOREFRONT_HOST para validar la tienda publica en entorno local."}
          />
        ) : tenant.status === "error" ? (
          tenant.error?.kind === "not_available" ? (
            <StorefrontUnavailableState
              host={tenant.host}
              message={tenant.error?.message || "La tienda no esta disponible para este dominio en este momento."}
            />
          ) : (
            <StorefrontErrorState
              title="No se pudo cargar la tienda"
              message={tenant.error?.message || "No se pudo resolver la tienda actual."}
              actionLabel={tenant.error?.kind === "network" ? "Reintentar" : undefined}
              onAction={tenant.error?.kind === "network" ? () => void tenant.refresh() : undefined}
            />
          )
        ) : (
          children ?? <Outlet />
        )}
      </main>

      <StorefrontFooter branding={tenant.branding} modoCatalogo={tenant.storefront?.modoCatalogo} />

      {!tenant.storefront?.modoCatalogo && tenant.branding?.telefonoComercial?.trim() && tenant.status === "resolved" ? (
        <StorefrontWhatsAppFab branding={tenant.branding} />
      ) : null}
    </div>
  )
}
