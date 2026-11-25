import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { addItemToCarrito, getCarrito } from "../api/carritoApi"
import type { AddItemToCarritoPayload } from "../types/carrito"

export function useCarrito() {
  return useQuery({
    queryKey: ["carrito"],
    queryFn: getCarrito,
  })
}

export function useAgregarAlCarrito() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (payload: AddItemToCarritoPayload) => addItemToCarrito(payload),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["carrito"] })
    },
  })
}
