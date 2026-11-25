import { useMutation } from "@tanstack/react-query"
import { crearStorefrontPedidoDirecto } from "../api/storefrontPedidoApi"
import type { StorefrontPedidoDirectoRequest } from "../types/storefrontPedido"

export function useCrearPedidoDirecto() {
  return useMutation({
    mutationFn: (payload: StorefrontPedidoDirectoRequest) => crearStorefrontPedidoDirecto(payload),
  })
}
