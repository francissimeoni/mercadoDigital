import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { getPedidosProveedorPendientes, marcarPedidoListoParaRetiro } from "../api/proveedorApi"

export function usePedidosProveedor(proveedorId: string | null) {
  return useQuery({
    queryKey: ["proveedor-pedidos", proveedorId],
    queryFn: () => getPedidosProveedorPendientes(proveedorId as string),
    enabled: Boolean(proveedorId),
  })
}

export function useMarcarListoParaRetiro(proveedorId: string | null) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (pedidoId: string) => marcarPedidoListoParaRetiro(pedidoId),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["proveedor-pedidos", proveedorId] })
    },
  })
}
