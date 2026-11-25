import { useQuery } from "@tanstack/react-query"
import { getProductoDetalle } from "../api/catalogoApi"

export function useProductoDetalle(productoId: string | undefined) {
  return useQuery({
    queryKey: ["producto-detalle", productoId],
    queryFn: () => getProductoDetalle(productoId as string),
    enabled: Boolean(productoId),
  })
}
