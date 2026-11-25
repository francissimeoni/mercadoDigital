import { useEffect } from "react"
import type { TenantBranding } from "../../tenant/tenantTypes"
import { applyPlatformDocumentBranding, applyStorefrontDocumentBranding } from "../utils/storefrontBranding"

type StorefrontDocumentBrandingProps = {
  branding: TenantBranding | null
  isReady: boolean
}

export default function StorefrontDocumentBranding({ branding, isReady }: StorefrontDocumentBrandingProps) {
  useEffect(() => {
    if (!isReady) {
      return
    }

    applyStorefrontDocumentBranding(branding)

    return () => {
      applyPlatformDocumentBranding()
    }
  }, [branding, isReady])

  return null
}
