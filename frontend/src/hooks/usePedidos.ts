import { useQuery } from "@tanstack/react-query"
import { getPedidoDetalle, getPedidosRevendedor } from "../api/pedidoApi"

export function usePedidos(revendedorId: string | null) {
  return useQuery({
    queryKey: ["pedidos", revendedorId],
    queryFn: () => getPedidosRevendedor(revendedorId as string),
    enabled: Boolean(revendedorId),
  })
}

export function usePedidoDetalle(pedidoId: string | undefined) {
  return useQuery({
    queryKey: ["pedido-detalle", pedidoId],
    queryFn: () => getPedidoDetalle(pedidoId as string),
    enabled: Boolean(pedidoId),
  })
}
