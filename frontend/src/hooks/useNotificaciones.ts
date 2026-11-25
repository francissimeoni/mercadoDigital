import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { getNotificaciones, marcarNotificacionComoLeida } from "../api/notificacionApi"

export function useNotificaciones(usuarioId: string | null) {
  return useQuery({
    queryKey: ["notificaciones", usuarioId],
    queryFn: () => getNotificaciones(usuarioId as string),
    enabled: Boolean(usuarioId),
  })
}

export function useMarcarNotificacionLeida(usuarioId: string | null) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: string) => marcarNotificacionComoLeida(id),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["notificaciones", usuarioId] })
    },
  })
}
