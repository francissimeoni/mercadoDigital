import MainLayout from "../components/layout/MainLayout"
import PublicStoreLayout from "../storefront/layout/PublicStoreLayout"
import StorefrontHomePage from "../storefront/pages/StorefrontHomePage"
import RoleHomeRedirect from "./RoleHomeRedirect"
import { useShouldUseOperationalLayout } from "./useStorefrontRouting"

export default function QuienesSomosRouteGate() {
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
      <StorefrontHomePage />
    </PublicStoreLayout>
  )
}
