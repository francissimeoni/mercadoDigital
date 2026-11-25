import { useMutation, useQueryClient } from "@tanstack/react-query"
import { crearPedidoCanonico } from "../api/pedidoApi"
import type { CrearPedidoCanonicoRequest } from "../types/pedido"

export function useCrearPedido() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (payload: CrearPedidoCanonicoRequest) => crearPedidoCanonico(payload),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["pedidos"] })
    },
  })
}
