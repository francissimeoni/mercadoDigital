import { useMutation, useQuery } from "@tanstack/react-query"
import { useState } from "react"
import { dineroCatalogo, cotizacionDosDecimales } from "../utils/catalogoDecimal"
import * as api from "../api/catalogoAdminApi"
import { catalogoError, useCatalogoDraft } from "../hooks/useCatalogoDraft"

const field = "mt-1 w-full rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-100"
const money = dineroCatalogo

export default function CatalogoProductPriceEditor({ id, product, categories, configVersion, refresh, notify }: {
  id: string; product: api.ProductoCatalogo; categories: api.CategoriaEnvioCatalogo[]; configVersion: number
  refresh: () => Promise<unknown>; notify: (text: string) => void
}) {
  const [snapshot, setSnapshot] = useState(product)
  const original: api.ProductoCatalogoInput = { moneda: snapshot.moneda ?? "", precioOriginal: snapshot.precioOriginal?.toString() ?? "", incluyeEnvio: snapshot.incluyeEnvio, categoriaEnvioId: snapshot.categoriaEnvioId, version: snapshot.version }
  const [form, setForm] = useState(original)
  useCatalogoDraft(JSON.stringify(form) !== JSON.stringify(original))
  const ready = Boolean(form.moneda && form.precioOriginal && form.incluyeEnvio !== null && (form.incluyeEnvio || form.categoriaEnvioId))
  // Cada borrador tiene una clave propia. Al cambiarlo se oculta inmediatamente el resultado anterior.
  // React Query cancela la solicitud anterior a través de AbortSignal y no la aplica al borrador nuevo.
  const preview = useQuery({
    queryKey: ["catalogo-preview-producto", id, product.productoId, form, configVersion, categories],
    queryFn: ({ signal }) => api.previewProductoCatalogo(id, product.productoId, form, signal), enabled: ready,
    staleTime: 0, retry: false,
  })
  const save = useMutation({ mutationFn: () => api.saveProductoCatalogo(id, product.productoId, { ...form, previewToken: preview.data?.token }), onSuccess: async saved => { setSnapshot(saved); setForm({ moneda: saved.moneda ?? "", precioOriginal: saved.precioOriginal?.toString() ?? "", incluyeEnvio: saved.incluyeEnvio, categoriaEnvioId: saved.categoriaEnvioId, version: saved.version }); await refresh(); notify("Precio preparado para el nuevo catálogo.") } })
  const calculated = ready ? preview.data?.calculo : undefined
  return <form className="space-y-4 rounded-2xl border border-blue-200 bg-blue-50/40 p-4" onSubmit={e => { e.preventDefault(); if (!save.isPending && !preview.isFetching && calculated?.estado === "LISTO") save.mutate() }}>
    <h3 className="font-semibold">Precio de catálogo · {product.nombre}</h3>
    <p className="text-xs text-slate-600">Precio de venta con tu ganancia incluida. Se aplica al modo catálogo cuando esté habilitado para esta tienda.</p>
    <fieldset disabled={save.isPending} className="grid gap-3 sm:grid-cols-2">
      <label className="text-sm">Moneda del artículo<select required className={field} value={form.moneda} onChange={e => setForm({ ...form, moneda: e.target.value })}><option value="">Elegir moneda</option><option value="ARS">Pesos argentinos (ARS)</option><option value="USD">Dólares (USD)</option></select></label>
      <label className="text-sm">Precio original del artículo<input required type="number" min="0.01" step="0.01" className={field} value={form.precioOriginal} onChange={e => setForm({ ...form, precioOriginal: e.target.value })} /></label>
      <label className="text-sm">¿El precio cargado ya incluye envío?<select required className={field} value={form.incluyeEnvio === null ? "" : String(form.incluyeEnvio)} onChange={e => setForm({ ...form, incluyeEnvio: e.target.value === "" ? null : e.target.value === "true", categoriaEnvioId: null })}><option value="">Elegir una opción</option><option value="true">Sí, ya está incluido</option><option value="false">No, sumar una categoría</option></select></label>
      {form.incluyeEnvio === false && <label className="text-sm">Categoría de costo de envío<select required className={field} value={form.categoriaEnvioId ?? ""} onChange={e => setForm({ ...form, categoriaEnvioId: e.target.value || null })}><option value="">Elegir categoría</option>{categories.map(c => <option key={c.id} value={c.id}>{c.nombre} · {money(c.costoArs)}</option>)}</select>{categories.length === 0 && <span className="text-xs text-amber-800">Creá una categoría de envío para continuar.</span>}</label>}
    </fieldset>
    <div aria-live="polite" className="rounded-xl bg-white p-3 text-sm">
      {!ready ? <p>Completá moneda, precio y envío para calcular.</p> : preview.isFetching ? <p>Calculando precio en pesos...</p> : preview.isError ? <p role="alert" className="text-red-700">{catalogoError(preview.error)}</p> : calculated?.estado === "PENDIENTE" ? <ul className="list-disc pl-4 text-amber-800">{calculated.pendientes.map(p => <li key={p}>{p}</li>)}</ul> : calculated?.precioPublicoArs != null ? <div className="space-y-1">{form.moneda === "USD" && <p>Cotización de la tienda: {money(cotizacionDosDecimales(calculated.cotizacionUsd))} por USD</p>}<p>Artículo en pesos: {money(calculated.baseArs!)}</p><p>Envío adicional: {money(calculated.envioArs!)}</p><p className="font-bold">Precio público preparado: {money(calculated.precioPublicoArs)} ARS</p></div> : null}
    </div>
    {save.isError && <p role="alert" className="text-sm text-red-700">{catalogoError(save.error)} <button type="button" className="underline" onClick={() => { save.reset(); void preview.refetch() }}>Volver a calcular</button></p>}
    <button disabled={save.isPending || !ready || preview.isFetching || calculated?.estado !== "LISTO"} className="rounded-xl bg-slate-950 px-4 py-2 text-sm font-semibold text-white disabled:opacity-50">{save.isPending ? "Guardando..." : "Guardar precio de catálogo"}</button>
  </form>
}
