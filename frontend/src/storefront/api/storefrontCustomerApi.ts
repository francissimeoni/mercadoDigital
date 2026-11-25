import storefrontOwnerClient from "./storefrontOwnerClient"
import type { StorefrontPedidoTrackingEnvio, StorefrontPedidoTrackingItem } from "../types/storefrontPedido"

export type StorefrontCustomerProfile = {
  nombre: string
  email: string
  telefonoWhatsapp: string | null
  provincia: string | null
  localidad: string | null
  codigoPostal: string | null
  calle: string | null
  numero: string | null
  pisoDepto: string | null
  observacionesEnvio: string | null
}
export type StorefrontCustomerProfileInput = { [K in Exclude<keyof StorefrontCustomerProfile, "email">]: string }
export type StorefrontCustomerOrder = {
  pedidoId: string
  trackingToken: string
  fechaCreacion: string
  estadoPedido: string
  estadoPago: string
  metodoPago: string
  totalFinal: number
  items: StorefrontPedidoTrackingItem[]
  envio: StorefrontPedidoTrackingEnvio | null
  transferenciaToken: string | null
  comprobanteCargado: boolean
}
export async function getCustomerOrders(storefrontHost: string) {
  return (await storefrontOwnerClient.get<StorefrontCustomerOrder[]>("/cliente/storefront/pedidos", { params: { storefrontHost } })).data
}
export async function getCustomerProfile(storefrontHost: string) {
  return (await storefrontOwnerClient.get<StorefrontCustomerProfile>("/cliente/storefront/perfil", { params: { storefrontHost } })).data
}
export async function updateCustomerProfile(input: StorefrontCustomerProfileInput & { storefrontHost: string }) {
  return (await storefrontOwnerClient.put<StorefrontCustomerProfile>("/cliente/storefront/perfil", input)).data
}
