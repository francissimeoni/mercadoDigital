import { useQuery } from "@tanstack/react-query"
import { getCatalogo } from "../api/catalogoApi"

export function useCatalogo(revendedorId: string | null) {
  return useQuery({
    queryKey: ["catalogo", revendedorId],
    queryFn: () => getCatalogo(revendedorId as string),
    enabled: Boolean(revendedorId),
  })
}
