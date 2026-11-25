import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import {
  aprobarRma,
  agregarMensajeRma,
  descargarRotuloDevolucionRma,
  descargarRotuloReposicionRma,
  entregarReposicionClienteRma,
  getRmaDetalle,
  getRmas,
  recibirReposicionMdRma,
  recibirRmaEnDeposito,
  reenviarReposicionClienteRma,
  resolverRma,
  solicitarReposicionProveedorRma,
  solicitarEnvioRma,
  solicitarRma,
} from "../api/rmaApi"
import type { ActualizarReposicionManualRequest, AgregarRmaMensajeRequest, ResolverRmaRequest, SolicitarRmaRequest } from "../types/rma"

type UseRmasParams = {
  pedidoId?: string
}

function getRmasQueryKey(params?: UseRmasParams) {
  return ["rmas", params?.pedidoId ?? null]
}

async function invalidateRmaQueries(queryClient: ReturnType<typeof useQueryClient>, rmaId?: string) {
  await Promise.all([
    queryClient.invalidateQueries({ queryKey: ["rmas"] }),
    rmaId ? queryClient.invalidateQueries({ queryKey: ["rma-detalle", rmaId] }) : Promise.resolve(),
  ])
}

export function useRmas(params?: UseRmasParams) {
  return useQuery({
    queryKey: getRmasQueryKey(params),
    queryFn: () => getRmas(params),
  })
}

export function useRmaDetalle(rmaId: string | undefined) {
  return useQuery({
    queryKey: ["rma-detalle", rmaId],
    queryFn: () => getRmaDetalle(rmaId as string),
    enabled: Boolean(rmaId),
  })
}

export function useSolicitarRma() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (payload: SolicitarRmaRequest) => solicitarRma(payload),
    onSuccess: async (rma) => {
      queryClient.setQueryData(["rma-detalle", rma.id], rma)
      await invalidateRmaQueries(queryClient, rma.id)
    },
  })
}

export function useAprobarRma() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (rmaId: string) => aprobarRma(rmaId),
    onSuccess: async (rma) => {
      queryClient.setQueryData(["rma-detalle", rma.id], rma)
      await invalidateRmaQueries(queryClient, rma.id)
    },
  })
}

export function useSolicitarEnvioRma() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (rmaId: string) => solicitarEnvioRma(rmaId),
    onSuccess: async (rma) => {
      queryClient.setQueryData(["rma-detalle", rma.id], rma)
      await invalidateRmaQueries(queryClient, rma.id)
    },
  })
}

export function useRecibirRmaEnDeposito() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (rmaId: string) => recibirRmaEnDeposito(rmaId),
    onSuccess: async (rma) => {
      queryClient.setQueryData(["rma-detalle", rma.id], rma)
      await invalidateRmaQueries(queryClient, rma.id)
    },
  })
}

export function useResolverRma() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ rmaId, payload }: { rmaId: string; payload: ResolverRmaRequest }) =>
      resolverRma(rmaId, payload),
    onSuccess: async (rma) => {
      queryClient.setQueryData(["rma-detalle", rma.id], rma)
      await invalidateRmaQueries(queryClient, rma.id)
    },
  })
}

export function useAgregarMensajeRma() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ rmaId, payload }: { rmaId: string; payload: AgregarRmaMensajeRequest }) =>
      agregarMensajeRma(rmaId, payload),
    onSuccess: async (rma) => {
      queryClient.setQueryData(["rma-detalle", rma.id], rma)
      await invalidateRmaQueries(queryClient, rma.id)
    },
  })
}

export function useDescargarRotuloDevolucionRma() {
  return useMutation({
    mutationFn: (rmaId: string) => descargarRotuloDevolucionRma(rmaId),
  })
}

export function useDescargarRotuloReposicionRma() {
  return useMutation({
    mutationFn: (rmaId: string) => descargarRotuloReposicionRma(rmaId),
  })
}

export function useSolicitarReposicionProveedorRma() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ rmaId, payload }: { rmaId: string; payload?: ActualizarReposicionManualRequest }) =>
      solicitarReposicionProveedorRma(rmaId, payload),
    onSuccess: async (rma) => {
      queryClient.setQueryData(["rma-detalle", rma.id], rma)
      await invalidateRmaQueries(queryClient, rma.id)
    },
  })
}

export function useRecibirReposicionMdRma() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ rmaId, payload }: { rmaId: string; payload?: ActualizarReposicionManualRequest }) =>
      recibirReposicionMdRma(rmaId, payload),
    onSuccess: async (rma) => {
      queryClient.setQueryData(["rma-detalle", rma.id], rma)
      await invalidateRmaQueries(queryClient, rma.id)
    },
  })
}

export function useReenviarReposicionClienteRma() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ rmaId, payload }: { rmaId: string; payload: ActualizarReposicionManualRequest }) =>
      reenviarReposicionClienteRma(rmaId, payload),
    onSuccess: async (rma) => {
      queryClient.setQueryData(["rma-detalle", rma.id], rma)
      await invalidateRmaQueries(queryClient, rma.id)
    },
  })
}

export function useEntregarReposicionClienteRma() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ rmaId, payload }: { rmaId: string; payload?: ActualizarReposicionManualRequest }) =>
      entregarReposicionClienteRma(rmaId, payload),
    onSuccess: async (rma) => {
      queryClient.setQueryData(["rma-detalle", rma.id], rma)
      await invalidateRmaQueries(queryClient, rma.id)
    },
  })
}
