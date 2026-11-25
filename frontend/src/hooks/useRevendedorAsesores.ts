import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { createRevendedorAsesor, getRevendedorAsesores, getRevendedorStorefrontHost, updateRevendedorAsesor } from "../api/revendedorApi"
import type { RevendedorAsesorRequest } from "../types/revendedor"

function getAsesoresKey(revendedorId: string | null) {
  return ["revendedor-asesores", revendedorId]
}

export function useRevendedorAsesores(revendedorId: string | null) {
  return useQuery({
    queryKey: getAsesoresKey(revendedorId),
    queryFn: () => getRevendedorAsesores(revendedorId as string),
    enabled: Boolean(revendedorId),
  })
}

export function useRevendedorStorefrontHost(revendedorId: string | null) {
  return useQuery({
    queryKey: ["revendedor-storefront-host", revendedorId],
    queryFn: () => getRevendedorStorefrontHost(revendedorId as string),
    enabled: Boolean(revendedorId),
  })
}

export function useCreateRevendedorAsesor(revendedorId: string | null) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: RevendedorAsesorRequest) => createRevendedorAsesor(revendedorId as string, payload),
    onSuccess: async () => {
      if (revendedorId) {
        await queryClient.invalidateQueries({ queryKey: getAsesoresKey(revendedorId) })
      }
    },
  })
}

export function useUpdateRevendedorAsesor(revendedorId: string | null) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ asesorId, payload }: { asesorId: string; payload: RevendedorAsesorRequest }) =>
      updateRevendedorAsesor(revendedorId as string, asesorId, payload),
    onSuccess: async () => {
      if (revendedorId) {
        await queryClient.invalidateQueries({ queryKey: getAsesoresKey(revendedorId) })
      }
    },
  })
}
