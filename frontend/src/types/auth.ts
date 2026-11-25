export type LoginRequest = {
  email: string
  password: string
}

export type AuthResponse = {
  token: string
  email: string
  issuedAt: string
  revendedorId: string | null
  proveedorId: string | null
  clienteId: string | null
}

export type AuthSession = {
  token: string
  email: string
  issuedAt: string
  userId: string
  roles: string[]
  exp: number | null
  revendedorId: string | null
  proveedorId: string | null
  clienteId: string | null
}
