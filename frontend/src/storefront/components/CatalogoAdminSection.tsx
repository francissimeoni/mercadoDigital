import { useEffect, useState, type FormEvent } from "react"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import * as api from "../api/catalogoAdminApi"
import { catalogoError, confirmCatalogoLeave, useCatalogoDraft } from "../hooks/useCatalogoDraft"
import { contactStatus, normalizeCommercialPhone } from "../dashboard/whatsapp"
import CatalogoProductManager from "./CatalogoProductManager"
import { dineroCatalogo, decimalCatalogo, cotizacionDosDecimales } from "../utils/catalogoDecimal"

const field = "dashboard-field mt-1 min-h-11 text-base sm:text-sm min-w-0 w-full rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-100"
const button = "dashboard-primary min-h-11 rounded-xl bg-slate-950 px-4 py-2 text-sm font-semibold text-white disabled:opacity-50"
const money = dineroCatalogo

export function CatalogoImpact({ impact }: { impact: api.ImpactoCatalogo }) {
  const [page, setPage] = useState(0)
  const pages = Math.max(1, Math.ceil(impact.productos.length / 25))
  const currentPage = Math.min(page, pages - 1)
  return <div className="space-y-2 rounded-xl border border-blue-200 bg-blue-50 p-3" role="status">
    <p className="text-sm font-semibold">Precios afectados: {impact.productos.length}</p>
    {impact.productos.length === 0 ? <p className="text-xs">Este cambio no afecta precios de productos configurados.</p> : <div className="max-h-64 overflow-auto"><table className="w-full text-left text-xs"><caption className="sr-only">Comparación antes de guardar</caption><thead><tr><th className="p-2">Producto</th><th className="p-2">Anterior ARS</th><th className="p-2">Propuesto ARS</th></tr></thead><tbody>{impact.productos.slice(currentPage * 25, (currentPage + 1) * 25).map(p => <tr key={p.productoId}><td className="p-2">{p.nombre}</td><td className="p-2">{money(p.anterior.precioPublicoArs)}</td><td className="p-2">{money(p.propuesto.precioPublicoArs)}</td></tr>)}</tbody></table></div>}
    {pages > 1 && <div className="flex flex-wrap gap-3 text-xs"><button type="button" disabled={currentPage === 0} onClick={() => setPage(currentPage - 1)}>Anterior</button><span>Página {currentPage + 1} de {pages}</span><button type="button" disabled={currentPage + 1 === pages} onClick={() => setPage(currentPage + 1)}>Siguiente</button></div>}
  </div>
}

export default function CatalogoAdminSection({ id, notify, refreshBranding, modoCatalogo = false, view = "all" }: { id: string; notify: (text: string) => void; refreshBranding?: () => Promise<unknown>; modoCatalogo?: boolean; view?: "all" | "products" | "pricing" | "contact" }) {
  const client = useQueryClient()
  // La recarga se hace al guardar explícitamente: un refetch de fondo no debe desmontar borradores.
  const queryOptions = { refetchOnWindowFocus: false, refetchOnReconnect: false, staleTime: Infinity }
  const config = useQuery({ ...queryOptions, queryKey: ["catalogo-config", id], queryFn: () => api.getConfigCatalogo(id) })
  const categories = useQuery({ ...queryOptions, queryKey: ["catalogo-envios", id], queryFn: () => api.getCategoriasEnvio(id), enabled: view !== "contact" })
  const products = useQuery({ ...queryOptions, queryKey: ["catalogo-productos", id], queryFn: () => api.getProductosCatalogo(id), enabled: view !== "contact" })
  const [reloadVersion, setReloadVersion] = useState(0)
  const refresh = async () => {
    await Promise.all([client.invalidateQueries({ queryKey: ["catalogo-config", id] }), client.invalidateQueries({ queryKey: ["catalogo-envios", id] }), client.invalidateQueries({ queryKey: ["catalogo-productos", id] }), client.invalidateQueries({ queryKey: ["catalogo-ficha", id] }), client.invalidateQueries({ queryKey: ["owner-storefront-content", id] }), client.invalidateQueries({ queryKey: ["owner-products", id] }), client.invalidateQueries({ queryKey: ["storefront-catalogo"] }), client.invalidateQueries({ queryKey: ["storefront-producto-detalle"] })])
  }
  const retry = () => { void config.refetch(); void categories.refetch(); void products.refetch() }
  if (config.isPending || (view !== "contact" && (categories.isPending || products.isPending))) return <p role="status">Cargando configuración del catálogo...</p>
  if ((config.isError && !config.data) || (view !== "contact" && ((categories.isError && !categories.data) || (products.isError && !products.data)))) return <div role="alert" className="rounded-xl border border-red-200 p-4">No se pudo cargar el catálogo. <button type="button" onClick={retry} className="underline">Reintentar</button></div>
  return <section className="space-y-5">
    {view === "all" && <><div><h1 className="text-2xl font-semibold">{modoCatalogo ? "Configurar catálogo" : "Preparar catálogo"}</h1><p className="text-sm text-slate-600">{modoCatalogo ? "El catálogo está activo: los cambios guardados se reflejan en sus precios públicos." : "Configurá precios y contactos. El catálogo público todavía no está activado para esta tienda."}</p><button type="button" className="mt-2 text-sm underline" onClick={async () => { if (confirmCatalogoLeave()) { await refresh(); setReloadVersion(value => value + 1) } }}>Recargar configuración</button></div>
    <p className="rounded-xl border border-slate-200 bg-white p-3 text-sm">El WhatsApp del titular se configura abajo. Administrá los asesores en <a href="/mi-tienda?tab=red" className="font-semibold text-blue-700 underline">Contactos y difusión</a> y revisá los textos personalizados en <a href="/mi-tienda?tab=cuenta" className="font-semibold text-blue-700 underline">Mi página</a>. El titular se destaca en el selector público.</p></>}
    {(view === "all" || view === "products") && <CatalogoProductManager key={`productos-${id}-${reloadVersion}`} id={id} products={products.data ?? []} categories={categories.data ?? []} config={config.data} refresh={refresh} notify={notify} modoCatalogo={modoCatalogo} />}
    {view !== "products" && <ConfigEditor key={`config-${id}-${reloadVersion}`} id={id} initial={config.data} view={view} refresh={async () => { await refresh(); await refreshBranding?.() }} notify={notify} />}
    {(view === "all" || view === "pricing") && <CategoryManager key={`envios-${id}-${reloadVersion}`} id={id} categories={categories.data ?? []} products={products.data ?? []} refresh={refresh} notify={notify} />}

  </section>
}

function ConfigEditor({ id, initial, refresh, notify, view = "all" }: { id: string; initial: api.ConfigCatalogo; view?: string; refresh: () => Promise<unknown>; notify: (text: string) => void }) {
  const [snapshot, setSnapshot] = useState(initial)
  const original = { cotizacionUsd: cotizacionDosDecimales(snapshot.cotizacionUsd), telefonoComercial: snapshot.telefonoComercial ?? "", condicionesEntrega: snapshot.condicionesEntrega ?? "" }
  const [form, setForm] = useState(original)
  const [review, setReview] = useState<{ key: string; impact: api.ImpactoCatalogo } | null>(null)
  const key = JSON.stringify(form)
  const dirty = key !== JSON.stringify(original)
  useCatalogoDraft(dirty)
  // Mostrar dos decimales no debe modificar una cotización histórica al guardar otro campo.
  const input = (): api.ConfigCatalogoInput => ({ ...form, cotizacionUsd: form.cotizacionUsd === original.cotizacionUsd ? snapshot.cotizacionUsd?.toString() ?? null : form.cotizacionUsd ? decimalInput(decimalInput(form.cotizacionUsd, 2), 6) : null, version: snapshot.version })
  const preview = useMutation({ mutationFn: () => api.previewConfigCatalogo(id, input()), onSuccess: impact => setReview({ key, impact }) })
  const save = useMutation({ mutationFn: () => api.saveConfigCatalogo(id, { ...input(), impactoToken: review?.impact.token }), onError: () => setReview(null), onSuccess: async saved => { setSnapshot(saved); setForm({ cotizacionUsd: cotizacionDosDecimales(saved.cotizacionUsd), telefonoComercial: saved.telefonoComercial ?? "", condicionesEntrega: saved.condicionesEntrega ?? "" }); setReview(null); await refresh(); notify("Configuración del catálogo guardada.") } })
  const busy = preview.isPending || save.isPending
  const reviewed = review?.key === key
  const submit = (e: FormEvent) => { e.preventDefault(); if (!busy) { if (reviewed) save.mutate(); else preview.mutate() } }
  return <form onSubmit={submit} className="dashboard-panel catalogo-config-form space-y-4 rounded-2xl border border-slate-200 bg-white p-5">
    <h2 id="catalogo-configuracion" className="font-semibold">{view === "contact" ? "Responsable de la tienda" : "Cotización y condiciones de entrega"}</h2>
    {view === "contact" && <p className="rounded-xl bg-emerald-50 p-3 text-sm text-emerald-950">Responsable destacado · {contactStatus(true, snapshot.telefonoComercial)}</p>}
    {initial.pendientes.length > 0 && <ul className="list-disc pl-5 text-sm text-amber-800">{initial.pendientes.map(p => <li key={p}>{p}</li>)}</ul>}
    <fieldset disabled={busy} className="grid gap-4 sm:grid-cols-2">
      {view !== "contact" && <label className="text-sm">Pesos por 1 USD<input className={field} type="text" inputMode="decimal" aria-label="Pesos por 1 USD" value={form.cotizacionUsd} onChange={e => setForm({ ...form, cotizacionUsd: e.target.value })} onBlur={() => setForm(current => ({ ...current, cotizacionUsd: decimalCatalogo(current.cotizacionUsd, 2) ?? current.cotizacionUsd }))} /><span className="text-xs text-slate-500">Hasta 2 decimales, sin separadores de miles.</span></label>}
      {view !== "pricing" && <label className="text-sm">WhatsApp comercial<input className={field} type="tel" placeholder="+54 9 11 1234 5678" value={form.telefonoComercial} onChange={e => setForm({ ...form, telefonoComercial: e.target.value })} /></label>}
      {view !== "contact" && <label className="text-sm sm:col-span-2">Condiciones y cobertura de entrega<textarea className={field} maxLength={2000} rows={3} value={form.condicionesEntrega} onChange={e => setForm({ ...form, condicionesEntrega: e.target.value })} /></label>}
    </fieldset>
    {reviewed && review && <CatalogoImpact impact={review.impact} />}
    {(preview.isError || save.isError) && <p role="alert" className="text-sm text-red-700">{catalogoError(save.error ?? preview.error)}</p>}
    <button disabled={busy || (view === "contact" && Boolean(form.telefonoComercial && !normalizeCommercialPhone(form.telefonoComercial)))} className={button}>{busy ? "Procesando..." : reviewed ? "Confirmar y guardar configuración" : "Revisar cambios antes de guardar"}</button>
  </form>
}

function CategoryManager({ id, categories, products, refresh, notify }: { id: string; categories: api.CategoriaEnvioCatalogo[]; products: api.ProductoCatalogo[]; refresh: () => Promise<unknown>; notify: (text: string) => void }) {
  const [selected, setSelected] = useState<string | null>(null)
  const [dirty, setDirty] = useState(false)
  const [deleting, setDeleting] = useState(false)
  const select = (value: string | null) => {
    if (dirty && !window.confirm("¿Descartar los cambios de esta categoría? El borrador del producto se conserva.")) return
    setSelected(value)
  }
  const current = categories.find(c => c.id === selected)
  const remove = useMutation({ mutationFn: (c: api.CategoriaEnvioCatalogo) => api.deleteCategoriaEnvio(id, c.id, c.version), onSuccess: async () => { setSelected(null); await refresh(); notify("Categoría de envío eliminada.") }, onSettled: () => setDeleting(false) })
  return <div className="space-y-3 rounded-2xl border border-slate-200 bg-white p-5">
    <h2 id="catalogo-envios" className="font-semibold">Categorías de envío de tu tienda</h2>
    
    {categories.map(c => <div key={c.id} className="flex flex-wrap items-center justify-between gap-2 border-b py-2 text-sm"><span>{c.nombre} · {money(c.costoArs)} · {products.filter(p => p.incluyeEnvio === false && p.categoriaEnvioId === c.id).length} producto(s) vinculados</span><div className="flex gap-3"><button type="button" className="underline" disabled={deleting} onClick={() => select(c.id)}>Editar</button><button type="button" className="text-red-700 underline" disabled={deleting} onClick={() => { if (window.confirm(`¿Eliminar ${c.nombre}? Las categorías en uso no se pueden eliminar. Reasigná sus productos primero.`)) { setDeleting(true); remove.mutate(c) } }}>Eliminar</button></div></div>)}
    <button type="button" className="text-sm font-semibold underline" onClick={() => select(null)}>Nueva categoría</button>
    {remove.isError && <p role="alert" className="text-sm text-red-700">{catalogoError(remove.error)}</p>}
    <CategoryEditor key={current?.id ?? "new"} id={id} category={current} refresh={refresh} notify={notify} onDirty={setDirty} />
  </div>
}

function CategoryEditor({ id, category, refresh, notify, onDirty }: { id: string; category?: api.CategoriaEnvioCatalogo; refresh: () => Promise<unknown>; notify: (text: string) => void; onDirty: (dirty: boolean) => void }) {
  const [snapshot, setSnapshot] = useState(category)
  const original = { nombre: snapshot?.nombre ?? "", costoArs: snapshot?.costoArs.toString() ?? "" }
  const [form, setForm] = useState(original)
  const [review, setReview] = useState<{ key: string; impact: api.ImpactoCatalogo } | null>(null)
  const key = JSON.stringify(form)
  useCatalogoDraft(key !== JSON.stringify(original))
  const dirty = key !== JSON.stringify(original)
  useEffect(() => { onDirty(dirty) }, [dirty, onDirty])
  const input = (): api.CategoriaEnvioInput => ({ ...form, costoArs: decimalInput(form.costoArs), version: snapshot?.version ?? -1 })
  const preview = useMutation({ mutationFn: () => api.previewCategoriaEnvio(id, category!.id, input()), onSuccess: impact => setReview({ key, impact }) })
  const save = useMutation({ mutationFn: () => category ? api.saveCategoriaEnvio(id, category.id, { ...input(), impactoToken: review?.impact.token }) : api.createCategoriaEnvio(id, input()), onError: () => setReview(null), onSuccess: async saved => { if (category) { setSnapshot(saved); setForm({ nombre: saved.nombre, costoArs: saved.costoArs.toString() }) } else setForm({ nombre: "", costoArs: "" }); setReview(null); await refresh(); notify("Categoría de envío guardada.") } })
  const busy = preview.isPending || save.isPending
  const reviewed = review?.key === key
  return <form className="space-y-3 rounded-xl bg-slate-50 p-3" onSubmit={e => { e.preventDefault(); if (!busy) { if (category && !reviewed) preview.mutate(); else save.mutate() } }}>
    <p className="text-sm font-semibold">{category ? "Editar categoría" : "Crear categoría"}</p>
    <fieldset disabled={busy} className="grid gap-3 sm:grid-cols-2"><label className="text-sm">Nombre<input required maxLength={100} className={field} value={form.nombre} onChange={e => setForm({ ...form, nombre: e.target.value })} /></label><label className="text-sm">Costo en ARS<input required inputMode="decimal" type="text" aria-label="Costo en ARS" className={field} value={form.costoArs} onChange={e => setForm({ ...form, costoArs: e.target.value })} /></label></fieldset>
    {reviewed && review && <div className="space-y-2"><p className="text-sm">Costo anterior: {money(snapshot?.costoArs)}. Costo propuesto: {money(decimalCatalogo(form.costoArs))}.</p><CatalogoImpact impact={review.impact} /></div>}
    {(preview.isError || save.isError) && <p role="alert" className="text-sm text-red-700">{catalogoError(save.error ?? preview.error)}</p>}
    <button disabled={busy} className={button}>{busy ? "Procesando..." : !category ? "Crear categoría" : reviewed ? "Confirmar y guardar categoría" : "Revisar impacto en precios"}</button>
  </form>
}

function decimalInput(value: string, fraction = 2) {
  const decimal = decimalCatalogo(value, fraction)
  if (decimal === null) throw new Error(`Revisá el importe: usá coma o punto decimal, sin separadores de miles y hasta ${fraction} decimales.`)
  return decimal
}
