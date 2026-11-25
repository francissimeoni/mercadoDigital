import { useState } from "react"
import { useMutation } from "@tanstack/react-query"
import { ajustarPreciosMasivamente, type ProductoAnterior } from "../api/storefrontOwnerApi"

// Production PA, offset 832434: selection survives filtering; manual amounts override percentage.
export default function StorefrontBulkPrices({ id, products, refresh, notify }: {
  id: string; products: ProductoAnterior[]; refresh: () => Promise<unknown>; notify: (message: string) => void
}) {
  const [search, setSearch] = useState("")
  const [category, setCategory] = useState("")
  const [selected, setSelected] = useState(new Set<string>())
  const [percentage, setPercentage] = useState("")
  const [overrides, setOverrides] = useState<Record<string, string>>({})
  const normalized = search.trim().toLocaleLowerCase("es")
  const categories = Array.from(new Map(products.filter(p => p.categoriaProductoId).map(p => [p.categoriaProductoId!, p.categoriaProductoNombre || "Sin categoría"]))).sort((a, b) => a[1].localeCompare(b[1], "es"))
  const visible = products.filter(p => (!normalized || p.nombre.toLocaleLowerCase("es").includes(normalized) || p.sku.toLocaleLowerCase("es").includes(normalized)) && (!category || (category === "sin-categoria" ? !p.categoriaProductoId : p.categoriaProductoId === category)))
  const chosen = products.filter(p => selected.has(p.id))
  const rate = Number(percentage.replace(",", "."))
  const validRate = Number.isFinite(rate) && rate >= -99.99 && rate <= 1000 && rate !== 0
  const calculated = (price: number) => Math.round(price * (1 + rate / 100) * 100) / 100
  const finalPrice = (p: ProductoAnterior) => overrides[p.id] !== undefined
    ? (overrides[p.id].trim() ? Number(overrides[p.id].replace(",", ".")) : Number.NaN)
    : validRate ? calculated(p.precioVenta) : Number.NaN
  const validPrices = chosen.every(p => Number.isFinite(finalPrice(p)) && finalPrice(p) >= 0)
  const allVisible = visible.length > 0 && visible.every(p => selected.has(p.id))
  const toggle = (ids: string[], remove: boolean) => {
    setSelected(current => { const next = new Set(current); ids.forEach(id => remove ? next.delete(id) : next.add(id)); return next })
    if (remove) setOverrides(current => { const next = { ...current }; ids.forEach(id => delete next[id]); return next })
  }
  const mutation = useMutation({
    mutationFn: () => ajustarPreciosMasivamente(id, chosen.map(p => ({ productoId: p.id, precioFinal: finalPrice(p) }))),
    onSuccess: async result => { await refresh(); setSelected(new Set()); setPercentage(""); setOverrides({}); notify(`Se actualizaron ${result.length} precios.`) },
  })
  const money = new Intl.NumberFormat("es-AR", { style: "currency", currency: "ARS" })
  const field = "rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm"
  return <div className="space-y-4 rounded-3xl border border-blue-200 bg-blue-50 p-5">
    <h2 className="text-lg font-bold">Actualización masiva · precios actuales en ARS</h2>
    <p className="text-sm">Los precios preparados en «Preparar catálogo» se administran por separado y no cambian con este ajuste.</p>
    <p>Filtrá, seleccioná únicamente los artículos alcanzados y revisá el precio anterior y el nuevo antes de aplicar.</p>
    <div className="flex flex-wrap gap-3">
      <input className={field} type="search" aria-label="Buscar por producto o SKU" placeholder="Buscar por producto o SKU" value={search} onChange={e => setSearch(e.target.value)} />
      <select className={field} aria-label="Categoría" value={category} onChange={e => setCategory(e.target.value)}><option value="">Todas las categorías</option><option value="sin-categoria">Sin categoría</option>{categories.map(([id, name]) => <option key={id} value={id}>{name}</option>)}</select>
      <input className={field} aria-label="Ajuste porcentual" inputMode="decimal" placeholder="Ej. 12,5 %" value={percentage} onChange={e => setPercentage(e.target.value)} />
      <button type="button" disabled={!visible.length} onClick={() => toggle(visible.map(p => p.id), allVisible)}>{allVisible ? "Quitar visibles" : "Seleccionar visibles"}</button>
    </div>
    <div className="max-h-96 overflow-auto rounded-2xl bg-white">{visible.length ? visible.map(p => <div key={p.id} className="grid items-center gap-3 border-b p-3 sm:grid-cols-[auto_1fr_150px_180px]">
      <input type="checkbox" aria-label={`Seleccionar ${p.nombre}`} checked={selected.has(p.id)} onChange={() => toggle([p.id], selected.has(p.id))} />
      <button type="button" className="text-left" onClick={() => toggle([p.id], selected.has(p.id))}><strong>{p.nombre}</strong><small className="block">{p.sku} · {p.categoriaProductoNombre || "Sin categoría"}</small></button>
      <span>Antes: {money.format(p.precioVenta)}</span>
      <label>Después <input className={`${field} w-full`} aria-label={`Precio final de ${p.nombre}`} inputMode="decimal" disabled={!selected.has(p.id) || !validRate} value={selected.has(p.id) ? overrides[p.id] ?? (validRate ? calculated(p.precioVenta).toFixed(2) : "") : ""} onChange={e => setOverrides(current => ({ ...current, [p.id]: e.target.value }))} /></label>
    </div>) : <p className="p-4">No hay productos que coincidan con los filtros.</p>}</div>
    <p>{chosen.length} seleccionados</p>
    {!!chosen.length && validRate && <p>Total actual: {money.format(chosen.reduce((sum, p) => sum + p.precioVenta, 0))} · Total nuevo: {validPrices ? money.format(chosen.reduce((sum, p) => sum + finalPrice(p), 0)) : "Revisá los importes"}</p>}
    {mutation.isError && <p role="alert">No se pudo aplicar el ajuste. Ningún precio fue modificado.</p>}
    <p className="text-sm">Admite aumentos y reducciones. Podés retocar cada precio final antes de confirmar.</p>
    <button type="button" className="rounded-xl bg-slate-950 px-5 py-3 font-bold text-white disabled:opacity-40" disabled={!chosen.length || !validRate || !validPrices || mutation.isPending} onClick={() => { if (window.confirm(`¿Actualizar el precio de ${chosen.length} producto(s)? Se aplicará la vista previa, incluidos los ajustes manuales.`)) mutation.mutate() }}>{mutation.isPending ? "Aplicando..." : `Actualizar ${chosen.length} producto(s)`}</button>
  </div>
}
