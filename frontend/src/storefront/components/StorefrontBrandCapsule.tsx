import type { TenantBranding } from "../../tenant/tenantTypes"
import { resolveStorefrontAssetUrl, resolveStorefrontDisplayName } from "../utils/storefrontBranding"

type StorefrontBrandCapsuleProps = {
  branding: TenantBranding | null
  isLoading?: boolean
  isReady?: boolean
  accentColor?: string
}

export default function StorefrontBrandCapsule({
  branding,
  isLoading = false,
  isReady = false,
  accentColor = "#0f172a",
}: StorefrontBrandCapsuleProps) {
  const displayName = isReady ? resolveStorefrontDisplayName(branding) : null
  const logoUrl = isReady ? resolveStorefrontAssetUrl(branding?.logoUrl) : null

  return (
    <div className="flex min-w-0 max-w-[min(100%,18rem)] items-center overflow-hidden rounded-full bg-white shadow-lg ring-1 ring-black/5 sm:max-w-xs">
      <div className="flex shrink-0 items-center justify-center bg-white px-2 py-1.5 sm:px-2.5 sm:py-2">
        {isLoading || !isReady ? (
          <div className="h-9 w-9 animate-pulse rounded-md bg-slate-700 sm:h-10 sm:w-10 sm:rounded-lg" />
        ) : logoUrl ? (
          <img
            src={logoUrl}
            alt=""
            className="h-9 w-14 object-contain sm:h-10 sm:w-16"
          />
        ) : (
          <div
            className="flex h-9 w-9 items-center justify-center rounded-md text-xs font-bold text-white sm:h-10 sm:w-10 sm:rounded-lg sm:text-sm"
            style={{ backgroundColor: accentColor }}
          >
            {(displayName || "T").slice(0, 2).toUpperCase()}
          </div>
        )}
      </div>

      <div className="flex min-w-0 items-center px-3 py-2 sm:px-4">
        {isLoading || !isReady ? (
          <div className="h-4 w-24 animate-pulse rounded bg-slate-200 sm:h-5 sm:w-28" />
        ) : displayName ? (
          <span className="truncate text-sm font-bold tracking-wide text-slate-950 sm:text-base">{displayName}</span>
        ) : null}
      </div>
    </div>
  )
}
