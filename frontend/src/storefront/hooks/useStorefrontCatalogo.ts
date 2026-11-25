import { useQuery } from "@tanstack/react-query"
import { getStorefrontCatalogo, getStorefrontProductoDetalle, type CatalogoTarget } from "../api/storefrontCatalogoApi"

export function useStorefrontCatalogo(target: CatalogoTarget | null) {
  return useQuery({
    queryKey: ["storefront-catalogo", target],
    queryFn: ({ signal }) => getStorefrontCatalogo(target as CatalogoTarget, signal),
    enabled: Boolean(target),
    staleTime: 0,
    refetchInterval: 60_000,
  })
}

export function useStorefrontProductoDetalle(productoId: string | undefined, target: CatalogoTarget | null) {
  return useQuery({
    queryKey: ["storefront-producto-detalle", productoId, target],
    queryFn: ({ signal }) => getStorefrontProductoDetalle(productoId as string, target as CatalogoTarget, signal),
    enabled: Boolean(productoId && target),
    staleTime: 0,
    refetchInterval: 60_000,
  })
}
