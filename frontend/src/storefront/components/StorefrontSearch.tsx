import { useState, type FormEvent } from "react"
import { useLocation, useNavigate } from "react-router-dom"
import { catalogReturn } from "../utils/catalogNavigation"
export default function StorefrontSearch() {
  const location = useLocation()
  const source = location.pathname === "/" || location.pathname === "/catalogo" ? location.search : new URL(catalogReturn(location).path, "https://local.invalid").search
  const query = new URLSearchParams(source).get("q") ?? ""
  return <SearchForm key={query + location.key} query={query} source={source} />
}
function SearchForm({ query, source }: { query: string; source: string }) {
  const [draft, setDraft] = useState(query)
  const navigate = useNavigate()
  const submit = (event: FormEvent) => {
    event.preventDefault()
    const params = new URLSearchParams(source)
    if (draft.trim()) params.set("q", draft.trim()); else params.delete("q")
    navigate(`/catalogo${params.size ? `?${params}` : ""}`)
    requestAnimationFrame(() => { document.getElementById("catalog-results")?.focus(); document.getElementById("catalog-results")?.scrollIntoView({ block: "start" }) })
  }
  return <form role="search" aria-label="Buscar en el catálogo" className="sf-search" onSubmit={submit}>
    <label className="sr-only" htmlFor="storefront-search">Buscar productos</label>
    <input id="storefront-search" type="search" enterKeyHint="search" autoComplete="off" placeholder="¿Qué estás buscando?" value={draft} onChange={e => setDraft(e.target.value)} />
    {draft && <button type="button" aria-label="Limpiar búsqueda" className="sf-search-clear" onClick={() => { setDraft(""); document.getElementById("storefront-search")?.focus() }}>×</button>}
    <button type="submit" aria-label="Buscar" className="sf-search-submit"><svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="10.5" cy="10.5" r="6.5" /><path d="m16 16 4.5 4.5" /></svg></button>
  </form>
}
