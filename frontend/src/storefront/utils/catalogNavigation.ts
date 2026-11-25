import type { Location } from "react-router-dom"
export type CatalogReturn = { path: string; visible: number; scroll: number; product?: string }
const campaignKeys = ["lista", "asesor", "asesorId", "storefrontHost"]
export function campaignParams(search: string) {
  const source = new URLSearchParams(search), result = new URLSearchParams()
  for (const key of campaignKeys) { const value = source.get(key); if (value?.trim()) result.set(key, value) }
  return result
}
export function campaignSearch(search: string) {
  const value = campaignParams(search).toString()
  return value ? `?${value}` : ""
}
export function catalogLink(search: string) { const params = campaignParams(search); return `/catalogo${params.size ? `?${params}` : ""}` }
export function catalogReturn(location: Pick<Location, "state" | "search">): CatalogReturn {
  const value = location.state?.catalogReturn as CatalogReturn | undefined
  if (value && /^\/(?:catalogo)?(?:\?|$)/.test(value.path) && Number.isFinite(value.scroll) && Number.isFinite(value.visible)) return value
  return { path: `/catalogo${campaignSearch(location.search)}`, scroll: 0, visible: 24 }
}
export function normalizeSearch(value: string) {
  return value.normalize("NFD").replace(/[\u0300-\u036f]/g, "").toLocaleLowerCase("es").trim()
}
export function matchesSearch(name: string, query: string) {
  const normalized = normalizeSearch(name)
  return normalizeSearch(query).split(/\s+/).filter(Boolean).every(word => normalized.includes(word))
}
