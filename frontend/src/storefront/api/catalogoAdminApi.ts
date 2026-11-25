import client from "./storefrontOwnerClient"
import type { ProductoPropio } from "./storefrontOwnerApi"

export type DecimalCatalogo = string

export type PrecioCatalogo = {
  estado: "LISTO" | "PENDIENTE"; pendientes: string[]; monedaOriginal: string | null
  precioOriginal: DecimalCatalogo | null; cotizacionUsd: DecimalCatalogo | null; baseArs: DecimalCatalogo | null
  envioArs: DecimalCatalogo | null; precioPublicoArs: DecimalCatalogo | null
}
export type ConfigCatalogo = {
  cotizacionUsd: DecimalCatalogo | null; condicionesEntrega: string | null; telefonoComercial: string | null
  version: number; modo: "PREPARACION"; pendientes: string[]
}
export type CategoriaEnvioCatalogo = { id: string; nombre: string; costoArs: DecimalCatalogo; version: number }
export type ProductoCatalogo = {
  productoId: string; nombre: string; sku: string; moneda: "ARS" | "USD" | null
  precioOriginal: DecimalCatalogo | null; incluyeEnvio: boolean | null; categoriaEnvioId: string | null
  version: number; calculo: PrecioCatalogo; activo: boolean
}
export type ImpactoCatalogo = {
  productos: { productoId: string; nombre: string; anterior: PrecioCatalogo; propuesto: PrecioCatalogo }[]
  token: string
}
// Los importes se envían como cadenas decimales para no calcular dinero con floats en el navegador.
export type ConfigCatalogoInput = {
  cotizacionUsd: string | null; condicionesEntrega: string; telefonoComercial: string; version: number; impactoToken?: string
}
export type CategoriaEnvioInput = { nombre: string; costoArs: string; version: number; impactoToken?: string }
export type ProductoCatalogoInput = {
  moneda: string; precioOriginal: string; incluyeEnvio: boolean | null; categoriaEnvioId: string | null; version: number; previewToken?: string
}
const base = (id: string) => `/revendedores/${id}/catalogo-config`
export const getConfigCatalogo = async (id: string) => (await client.get<ConfigCatalogo>(base(id))).data
export const getCategoriasEnvio = async (id: string) => (await client.get<CategoriaEnvioCatalogo[]>(`${base(id)}/categorias`)).data
export const getProductosCatalogo = async (id: string) => (await client.get<ProductoCatalogo[]>(`${base(id)}/productos`)).data
export const previewConfigCatalogo = async (id: string, input: ConfigCatalogoInput, signal?: AbortSignal) => (await client.post<ImpactoCatalogo>(`${base(id)}/vista-previa`, input, { signal })).data
export const saveConfigCatalogo = async (id: string, input: ConfigCatalogoInput) => (await client.put<ConfigCatalogo>(base(id), input)).data
export const createCategoriaEnvio = async (id: string, input: CategoriaEnvioInput) => (await client.post<CategoriaEnvioCatalogo>(`${base(id)}/categorias`, input)).data
export const previewCategoriaEnvio = async (id: string, category: string, input: CategoriaEnvioInput, signal?: AbortSignal) => (await client.post<ImpactoCatalogo>(`${base(id)}/categorias/${category}/vista-previa`, input, { signal })).data
export const saveCategoriaEnvio = async (id: string, category: string, input: CategoriaEnvioInput) => (await client.put<CategoriaEnvioCatalogo>(`${base(id)}/categorias/${category}`, input)).data
export const deleteCategoriaEnvio = async (id: string, category: string, version: number) => client.delete(`${base(id)}/categorias/${category}`, { params: { version } })
export const previewProductoCatalogo = async (id: string, product: string, input: ProductoCatalogoInput, signal?: AbortSignal) => (await client.post<{ calculo: PrecioCatalogo; token: string }>(`${base(id)}/productos/${product}/vista-previa`, input, { signal })).data
export const saveProductoCatalogo = async (id: string, product: string, input: ProductoCatalogoInput) => (await client.put<ProductoCatalogo>(`${base(id)}/productos/${product}`, input)).data

export type FichaCatalogo = { datos: ProductoPropio; versionFicha: number; precio: ProductoCatalogo }
export type FichaCatalogoInput = {
  operacionId: string; sku: string; nombre: string; descripcion: string; categoriaProductoId: string | null
  activo: boolean; promocion: boolean; novedad: boolean; versionFicha: number; precio: ProductoCatalogoInput
  imagenes: { id: string | null; url: string }[]
  archivos: { nombre: string; size: number; sha256: string }[]; previewToken?: string
}
export const getFichaCatalogo = async (id: string, product: string, signal?: AbortSignal) => (await client.get<FichaCatalogo>(`${base(id)}/fichas/${product}`, { signal })).data
export const previewFichaCatalogo = async (id: string, product: string | null, input: FichaCatalogoInput, signal?: AbortSignal) =>
  (await client.post<{ calculo: PrecioCatalogo; token: string }>(`${base(id)}/fichas${product ? `/${product}` : ""}/vista-previa`, input, { signal })).data
export async function saveFichaCatalogo(id: string, product: string | null, input: FichaCatalogoInput, files: File[]) {
  const body = new FormData()
  body.append("ficha", new Blob([JSON.stringify(input)], { type: "application/json" }))
  files.forEach(file => body.append("imagenes", file))
  return (await client.request<FichaCatalogo>({ method: product ? "put" : "post", url: `${base(id)}/fichas${product ? `/${product}` : ""}`, data: body })).data
}
