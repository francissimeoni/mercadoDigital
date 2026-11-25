import { useQuery } from "@tanstack/react-query"
import { getStorefrontPedidoTracking } from "../api/storefrontTrackingApi"

export function usePedidoTracking(trackingToken: string | undefined, storefrontHost: string | null) {
  return useQuery({
    queryKey: ["storefront-pedido-tracking", trackingToken, storefrontHost],
    queryFn: () => getStorefrontPedidoTracking(trackingToken as string, storefrontHost as string),
    enabled: Boolean(trackingToken && storefrontHost),
  })
}
