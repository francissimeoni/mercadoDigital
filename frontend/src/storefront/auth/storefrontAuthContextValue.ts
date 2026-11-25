import { createContext } from "react"
import type { StorefrontSession } from "./storefrontSession"

export type StorefrontLoginInput = { storefrontHost: string; email: string; password: string }
export type StorefrontRegisterInput = StorefrontLoginInput & {
  nombre: string
  telefonoWhatsapp: string
  provincia: string
  localidad: string
  codigoPostal: string
  calle: string
  numero: string
  pisoDepto?: string
  observacionesEnvio?: string
}

export type StorefrontAuthValue = {
  session: StorefrontSession | null
  login: (input: StorefrontLoginInput) => Promise<StorefrontSession>
  register: (input: StorefrontRegisterInput) => Promise<StorefrontSession>
  logout: () => void
}

export const StorefrontAuthContext = createContext<StorefrontAuthValue | null>(null)
