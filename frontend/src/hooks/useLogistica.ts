import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import {
  crearTransportista,
  despacharEnvio,
  descargarRotulo,
  getDespachosPendientes,
  getEnvioByPedido,
  getRetirosPendientes,
  getTransportistas,
  marcarEntregado,
  marcarRetirado,
} from "../api/logisticaApi"
import type { DespacharEnvioRequest } from "../types/logistica"

export function useRetirosPendientes() {
  return useQuery({
    queryKey: ["logistica-retiros-pendientes"],
    queryFn: getRetirosPendientes,
  })
}

export function useDespachosPendientes() {
  return useQuery({
    queryKey: ["logistica-despachos-pendientes"],
    queryFn: getDespachosPendientes,
  })
}

export function useTransportistas() {
  return useQuery({
    queryKey: ["logistica-transportistas"],
    queryFn: getTransportistas,
  })
}

export function useCrearTransportista() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: crearTransportista,
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["logistica-transportistas"] })
    },
  })
}

export function useEnvioLogistica(pedidoId: string | null) {
  return useQuery({
    queryKey: ["logistica-envio", pedidoId],
    queryFn: () => getEnvioByPedido(pedidoId as string),
    enabled: Boolean(pedidoId),
  })
}

export function useMarcarRetirado() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (pedidoId: string) => marcarRetirado(pedidoId),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["logistica-retiros-pendientes"] }),
        queryClient.invalidateQueries({ queryKey: ["logistica-despachos-pendientes"] }),
      ])
    },
  })
}

export function useDespacharEnvio() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ pedidoId, payload }: { pedidoId: string; payload: DespacharEnvioRequest }) =>
      despacharEnvio(pedidoId, payload),
    onSuccess: async (_data, variables) => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["logistica-retiros-pendientes"] }),
        queryClient.invalidateQueries({ queryKey: ["logistica-despachos-pendientes"] }),
        queryClient.invalidateQueries({ queryKey: ["logistica-envio", variables.pedidoId] }),
      ])
    },
  })
}

export function useMarcarEntregado() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (pedidoId: string) => marcarEntregado(pedidoId),
    onSuccess: async (_data, pedidoId) => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["logistica-despachos-pendientes"] }),
        queryClient.invalidateQueries({ queryKey: ["logistica-envio", pedidoId] }),
      ])
    },
  })
}

export function useDescargarRotulo() {
  return useMutation({
    mutationFn: (pedidoId: string) => descargarRotulo(pedidoId),
  })
}
