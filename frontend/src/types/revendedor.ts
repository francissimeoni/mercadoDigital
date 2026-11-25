export type RevendedorAsesorResponse = {
  id: string
  revendedorId: string
  nombre: string
  fotoUrl: string | null
  activo: boolean
  createdAt: string
  updatedAt: string
}

export type RevendedorStorefrontHostResponse = {
  revendedorId: string
  storefrontHost: string | null
}

export type RevendedorAsesorRequest = {
  nombre: string
  fotoUrl?: string | null
  activo?: boolean
}
