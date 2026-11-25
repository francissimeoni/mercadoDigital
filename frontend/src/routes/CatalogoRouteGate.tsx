import MainLayout from "../components/layout/MainLayout"
import CatalogoPage from "../pages/CatalogoPage"
import PublicStoreLayout from "../storefront/layout/PublicStoreLayout"
import StorefrontCatalogPage from "../storefront/pages/StorefrontCatalogPage"
import { useShouldUseOperationalLayout } from "./useStorefrontRouting"

export default function CatalogoRouteGate() {
  const shouldUseOperationalLayout = useShouldUseOperationalLayout()

  if (shouldUseOperationalLayout) {
    return (
      <MainLayout>
        <CatalogoPage />
      </MainLayout>
    )
  }

  return (
    <PublicStoreLayout>
      <StorefrontCatalogPage />
    </PublicStoreLayout>
  )
}
