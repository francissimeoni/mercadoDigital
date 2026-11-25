import MainLayout from "../components/layout/MainLayout"
import PublicStoreLayout from "../storefront/layout/PublicStoreLayout"
import StorefrontCatalogPage from "../storefront/pages/StorefrontCatalogPage"
import RoleHomeRedirect from "./RoleHomeRedirect"
import { useShouldUseOperationalLayout } from "./useStorefrontRouting"

export default function HomeRouteGate() {
  const shouldUseOperationalLayout = useShouldUseOperationalLayout()

  if (shouldUseOperationalLayout) {
    return (
      <MainLayout>
        <RoleHomeRedirect />
      </MainLayout>
    )
  }

  return (
    <PublicStoreLayout>
      <StorefrontCatalogPage />
    </PublicStoreLayout>
  )
}
