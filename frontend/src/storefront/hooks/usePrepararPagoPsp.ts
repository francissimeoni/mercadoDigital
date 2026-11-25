import { useMutation } from "@tanstack/react-query"
import { prepararStorefrontPagoPsp } from "../api/storefrontPagoApi"

export function usePrepararPagoPsp() {
  return useMutation({
    mutationFn: ({ pedidoId, trackingToken, storefrontHost }: { pedidoId: string; trackingToken: string; storefrontHost: string }) =>
      prepararStorefrontPagoPsp(pedidoId, trackingToken, storefrontHost),
  })
}
