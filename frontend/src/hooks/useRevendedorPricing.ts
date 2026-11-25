import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import {
  deleteRevendedorMarkupOverride,
  getRevendedorPricingConfig,
  getRevendedorPricingPreview,
  updateRevendedorPricingConfig,
  upsertRevendedorMarkupOverride,
} from "../api/pricingApi"
import type { RevendedorMarkupOverrideRequest, RevendedorPricingConfigRequest } from "../types/pricing"

function getConfigKey(revendedorId: string | null) {
  return ["revendedor-pricing-config", revendedorId]
}

function getPreviewKey(revendedorId: string | null) {
  return ["revendedor-pricing-preview", revendedorId]
}

async function invalidatePricingQueries(queryClient: ReturnType<typeof useQueryClient>, revendedorId: string) {
  await Promise.all([
    queryClient.invalidateQueries({ queryKey: getConfigKey(revendedorId) }),
    queryClient.invalidateQueries({ queryKey: getPreviewKey(revendedorId) }),
  ])
}

export function useRevendedorPricingConfig(revendedorId: string | null) {
  return useQuery({
    queryKey: getConfigKey(revendedorId),
    queryFn: () => getRevendedorPricingConfig(revendedorId as string),
    enabled: Boolean(revendedorId),
  })
}

export function useRevendedorPricingPreview(revendedorId: string | null) {
  return useQuery({
    queryKey: getPreviewKey(revendedorId),
    queryFn: () => getRevendedorPricingPreview(revendedorId as string),
    enabled: Boolean(revendedorId),
  })
}

export function useUpdateRevendedorPricingConfig(revendedorId: string | null) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (request: RevendedorPricingConfigRequest) =>
      updateRevendedorPricingConfig(revendedorId as string, request),
    onSuccess: async () => {
      if (revendedorId) {
        await invalidatePricingQueries(queryClient, revendedorId)
      }
    },
  })
}

export function useUpsertRevendedorMarkupOverride(revendedorId: string | null) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ productoId, request }: { productoId: string; request: RevendedorMarkupOverrideRequest }) =>
      upsertRevendedorMarkupOverride(revendedorId as string, productoId, request),
    onSuccess: async () => {
      if (revendedorId) {
        await invalidatePricingQueries(queryClient, revendedorId)
      }
    },
  })
}

export function useDeleteRevendedorMarkupOverride(revendedorId: string | null) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (productoId: string) => deleteRevendedorMarkupOverride(revendedorId as string, productoId),
    onSuccess: async () => {
      if (revendedorId) {
        await invalidatePricingQueries(queryClient, revendedorId)
      }
    },
  })
}
