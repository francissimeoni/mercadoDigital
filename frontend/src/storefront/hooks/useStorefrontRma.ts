import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import {
  agregarMensajeStorefrontRma,
  descargarRotuloDevolucionStorefrontRma,
  getStorefrontRmas,
  solicitarStorefrontRma,
} from "../api/storefrontRmaApi"
import type {
  StorefrontPublicAgregarRmaMensajeRequest,
  StorefrontPublicSolicitarRmaRequest,
} from "../types/storefrontPedido"

function getQueryKey(trackingToken: string | undefined, storefrontHost: string | null) {
  return ["storefront-rmas", trackingToken ?? null, storefrontHost ?? null]
}

export function useStorefrontRmas(trackingToken: string | undefined, storefrontHost: string | null) {
  return useQuery({
    queryKey: getQueryKey(trackingToken, storefrontHost),
    queryFn: () => getStorefrontRmas(trackingToken as string, storefrontHost as string),
    enabled: Boolean(trackingToken && storefrontHost),
  })
}

export function useSolicitarStorefrontRma(trackingToken: string | undefined, storefrontHost: string | null) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: StorefrontPublicSolicitarRmaRequest) =>
      solicitarStorefrontRma(trackingToken as string, storefrontHost as string, payload),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: getQueryKey(trackingToken, storefrontHost) })
    },
  })
}

export function useAgregarMensajeStorefrontRma(trackingToken: string | undefined, storefrontHost: string | null) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ rmaId, payload }: { rmaId: string; payload: StorefrontPublicAgregarRmaMensajeRequest }) =>
      agregarMensajeStorefrontRma(trackingToken as string, storefrontHost as string, rmaId, payload),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: getQueryKey(trackingToken, storefrontHost) })
    },
  })
}

export function useDescargarRotuloDevolucionStorefrontRma(
  trackingToken: string | undefined,
  storefrontHost: string | null,
) {
  return useMutation({
    mutationFn: (rmaId: string) => descargarRotuloDevolucionStorefrontRma(trackingToken as string, storefrontHost as string, rmaId),
  })
}
