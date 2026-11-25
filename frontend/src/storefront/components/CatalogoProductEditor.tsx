import { useEffect, useRef, useState } from "react"
import { useMutation, useQuery } from "@tanstack/react-query"
import * as api from "../api/catalogoAdminApi"
import type { CategoriaProducto } from "../api/storefrontOwnerApi"
import { catalogoError, confirmCatalogoLeave, useCatalogoDraft } from "../hooks/useCatalogoDraft"
import { decimalCatalogo, dineroCatalogo, cotizacionDosDecimales } from "../utils/catalogoDecimal"

const field = "product-field"
const toForm = (ficha?: api.FichaCatalogo): api.FichaCatalogoInput => ({
  operacionId: crypto.randomUUID(), sku: ficha?.datos.sku ?? "", nombre: ficha?.datos.nombre ?? "",
  descripcion: ficha?.datos.descripcion ?? "", categoriaProductoId: ficha?.datos.categoriaProductoId ?? null,
  activo: ficha?.datos.activo ?? false, promocion: ficha?.datos.promocion ?? false, novedad: ficha?.datos.novedad ?? false,
  versionFicha: ficha?.versionFicha ?? -1,
  precio: { moneda: ficha?.precio.moneda ?? "", precioOriginal: ficha?.precio.precioOriginal?.toString() ?? "",
    incluyeEnvio: ficha?.precio.incluyeEnvio ?? null, categoriaEnvioId: ficha?.precio.categoriaEnvioId ?? null, version: ficha?.precio.version ?? -1 },
  imagenes: ficha?.datos.media.filter(m => m.tipoMedia === "IMAGEN").map(m => ({ id: m.id, url: m.url })) ?? [], archivos: [],
})

export default function CatalogoProductEditor({ id, initial, categories, productCategories, config, refresh, notify, onClose, modoCatalogo = false, currentProduct }: {
  id: string; initial?: api.FichaCatalogo; categories: api.CategoriaEnvioCatalogo[]; productCategories: CategoriaProducto[]
  config: api.ConfigCatalogo; refresh: () => Promise<unknown>; notify: (text: string) => void; onClose?: () => void; modoCatalogo?: boolean; currentProduct?: api.ProductoCatalogo
}) {
  const [baseline, setBaseline] = useState(() => toForm(initial))
  const [form, setForm] = useState(baseline)
  const [saved, setSaved] = useState(initial)
  const [files, setFiles] = useState<File[]>([])
  const [link, setLink] = useState("")
  const [linkError, setLinkError] = useState("")
  const [fileError, setFileError] = useState("")
  const [hashing, setHashing] = useState(false)
  const [refreshError, setRefreshError] = useState(false)
  const [reload, setReload] = useState(0)
  const heading = useRef<HTMLHeadingElement>(null)
  const errorNotice = useRef<HTMLDivElement>(null)
  useEffect(() => {
    const viewport = window.visualViewport
    const update = () => document.getElementById("catalogo-ficha")?.style.setProperty("--product-keyboard-offset", `${Math.max(0, window.innerHeight - (viewport?.height ?? window.innerHeight) - (viewport?.offsetTop ?? 0))}px`)
    viewport?.addEventListener("resize", update); viewport?.addEventListener("scroll", update); update()
    return () => { viewport?.removeEventListener("resize", update); viewport?.removeEventListener("scroll", update) }
  }, [])
  useEffect(() => { heading.current?.focus() }, [])
  const submitting = useRef(false)
  const fileRevision = useRef(0)
  const fileInput = useRef<HTMLInputElement>(null)
  const dirty = JSON.stringify(form) !== JSON.stringify(baseline)
  useCatalogoDraft(dirty || hashing || Boolean(link.trim()))
  const amount = decimalCatalogo(form.precio.precioOriginal)
  const input = { ...form, precio: { ...form.precio, precioOriginal: amount ?? "" } }
  const canPreview = Boolean(form.nombre.trim() && form.sku.trim() && amount && form.precio.moneda && form.precio.incluyeEnvio !== null
    && (form.precio.incluyeEnvio || form.precio.categoriaEnvioId) && !hashing && !fileError)
  const preview = useQuery({ queryKey: ["catalogo-preview-ficha", id, saved?.datos.id, input, config.version, categories, reload],
    queryFn: ({ signal }) => api.previewFichaCatalogo(id, saved?.datos.id ?? null, input, signal), enabled: canPreview,
    retry: false, staleTime: 0, refetchOnWindowFocus: false })
  const save = useMutation({ mutationFn: () => api.saveFichaCatalogo(id, saved?.datos.id ?? null,
    { ...input, previewToken: preview.data?.token }, files),
    onSuccess: async result => {
      const next = toForm(result); setSaved(result); setBaseline(next); setForm(next); setFiles([])
      if (fileInput.current) fileInput.current.value = ""
      notify("Producto y precio de catálogo guardados.")
      try { await refresh(); setRefreshError(false) } catch { setRefreshError(true) }
    }, onError: () => { window.requestAnimationFrame(() => { errorNotice.current?.focus(); errorNotice.current?.scrollIntoView({ block: "center", behavior: "smooth" }) }) }, onSettled: () => { submitting.current = false } })
  useCatalogoDraft(save.isPending)
  const setPrice = <K extends keyof api.ProductoCatalogoInput>(key: K, value: api.ProductoCatalogoInput[K]) =>
    setForm(f => ({ ...f, precio: { ...f.precio, [key]: value } }))
  const selectFiles = async (selected: File[]) => {
    const revision = ++fileRevision.current
    setFileError(""); setHashing(true)
    try {
      if (selected.length > 5 || form.imagenes.length + selected.length > 20 || selected.some(file => file.size === 0 || file.size > 5 * 1024 * 1024 || !["image/png", "image/jpeg", "image/webp"].includes(file.type)))
        throw new Error("Elegí hasta 5 imágenes PNG, JPEG o WEBP de hasta 5 MB cada una. El producto admite 20 imágenes en total.")
      if (selected.reduce((total, file) => total + file.size, 0) > 19 * 1024 * 1024) throw new Error("Las imágenes seleccionadas superan 19 MB en total. Subí menos archivos por guardado.")
      const descriptors = await Promise.all(selected.map(async file => ({ nombre: file.name, size: file.size,
        sha256: [...new Uint8Array(await crypto.subtle.digest("SHA-256", await file.arrayBuffer()))].map(b => b.toString(16).padStart(2, "0")).join("") })))
      if (revision !== fileRevision.current) return
      setFiles(selected); setForm(f => ({ ...f, archivos: descriptors }))
      if (fileInput.current) fileInput.current.value = ""
    } catch (error) { if (revision === fileRevision.current) setFileError(error instanceof Error ? error.message : "No se pudieron preparar las imágenes.") }
    finally { if (revision === fileRevision.current) { setHashing(false); if (fileInput.current) fileInput.current.value = "" } }
  }
  const calculated = canPreview ? preview.data?.calculo : undefined
  const busy = save.isPending || hashing
  const canSave = (!saved || dirty) && !busy && !fileError && !link.trim() && canPreview && !preview.isFetching && calculated?.estado === "LISTO" && Boolean(preview.data?.token)
  const persisted = currentProduct ?? saved?.precio
  const published = modoCatalogo && persisted?.activo && persisted.calculo.estado === "LISTO"
  const removeFile = (index: number) => {
    setFiles(old => old.filter((_, i) => i !== index))
    setForm(f => ({ ...f, archivos: f.archivos.filter((_, i) => i !== index) })); setFileError("")
  }
  const addLink = () => {
    const value = link.trim()
    try {
      const url = new URL(value)
      if (!["https:", "http:"].includes(url.protocol) || value.length > 500) throw new Error()
      if (form.imagenes.length + files.length >= 20) { setLinkError("El producto admite hasta 20 imágenes."); return }
      setForm(f => ({ ...f, imagenes: [...f.imagenes, { id: null, url: value }] })); setLink(""); setLinkError("")
    } catch { setLinkError("Ingresá una dirección completa de imagen que empiece con https:// o http://.") }
  }
  const configuration = (targetId: string) => {
    const target = document.getElementById(targetId)
    if (target) target.scrollIntoView({ behavior: "smooth" })
    else if (confirmCatalogoLeave()) window.location.assign("/mi-tienda?tab=precios")
  }
  return <form id="catalogo-ficha" className="product-workspace product-editor" onInvalidCapture={event => { (event.target as HTMLElement).scrollIntoView({ block: "center" }) }} onSubmit={event => {
    event.preventDefault()
    if (submitting.current || !canSave) return
    submitting.current = true; save.mutate()
  }}>
    <div className="product-editor-toolbar">
    {onClose && <button type="button" className="product-link product-back" disabled={busy} onClick={onClose}>← Productos</button>}
    <div className="product-heading"><div className="product-editor-name"><h1 ref={heading} tabIndex={-1} className="product-title outline-none">{saved ? saved.datos.nombre : "Nuevo producto"}</h1><p className="product-subtitle"><span className={`product-status ${published ? "is-published" : persisted?.calculo.estado === "PENDIENTE" ? "is-pending" : ""}`}>{published ? "Publicado" : saved ? "No publicado" : "Sin guardar"}</span></p></div></div>
    <div className="product-actions"><p role="status" className="product-help">{busy ? "Guardando o preparando imágenes…" : link.trim() ? "Agregá el enlace pendiente o borrá su contenido para guardar." : fileError ? "Revisá las imágenes seleccionadas." : dirty && !canPreview ? "Completá los datos obligatorios y revisá el precio." : preview.isFetching ? "Revisando precio…" : dirty ? "Cambios sin guardar" : saved ? "Todos los cambios guardados" : "Completá los datos del producto"}</p><button className="product-primary" disabled={!canSave}>{save.isPending ? "Guardando…" : saved ? "Guardar cambios" : "Crear producto"}</button></div>
    </div>
    <fieldset disabled={busy} className="product-editor-grid">
      <legend className="sr-only">Editar ficha del producto</legend>
      <div className="product-editor-column"><section className="product-panel" aria-labelledby="product-info-title"><h2 id="product-info-title" className="product-section-title">Información del producto</h2><div className="product-fields">
          <label className="product-label">Nombre del producto<input required maxLength={180} className={field} value={form.nombre} onChange={e => setForm(f => ({ ...f, nombre: e.target.value }))} /></label>
          <label className="product-label">Descripción<textarea maxLength={20000} rows={5} className={field} value={form.descripcion} onChange={e => setForm(f => ({ ...f, descripcion: e.target.value }))} /></label>
          <div className="product-split"><label className="product-label">SKU<input required maxLength={80} className={field} value={form.sku} onChange={e => setForm(f => ({ ...f, sku: e.target.value }))} /></label><label className="product-label">Rubro del producto<select className={field} value={form.categoriaProductoId ?? ""} onChange={e => setForm(f => ({ ...f, categoriaProductoId: e.target.value || null }))}><option value="">Sin rubro</option>{productCategories.map(c => <option key={c.id} value={c.id}>{c.nombre}</option>)}</select></label></div>
        </div></section>
<section className="product-panel" aria-labelledby="product-images-title"><h2 id="product-images-title" className="product-section-title">Imágenes</h2>
          {form.imagenes.length + files.length > 0 ? <div className="product-photos">
            {form.imagenes.map((image, index) => <div className="product-photo" key={`${image.id ?? image.url}-${index}`}><div className="product-photo-preview"><ProductPhoto src={image.url} alt={`Imagen ${index + 1} de ${form.nombre || "producto"}`} />{index === 0 && <span className="product-status">Portada</span>}</div><div className="product-photo-actions">{index > 0 && <button type="button" aria-label={`Usar imagen ${index + 1} como portada`} onClick={() => setForm(f => ({ ...f, imagenes: [f.imagenes[index], ...f.imagenes.filter((_, i) => i !== index)] }))}>Portada</button>}<button type="button" aria-label={`Quitar imagen ${index + 1}`} onClick={() => setForm(f => ({ ...f, imagenes: f.imagenes.filter((_, i) => i !== index) }))}>Quitar</button></div></div>)}
            {files.map((file, index) => <div className="product-photo" key={`${file.name}-${index}`}><div className="product-photo-preview"><ProductPhoto file={file} alt={`Vista previa de ${file.name}`} /><span className="product-status">{!form.imagenes.length && index === 0 ? "Portada · Nueva" : "Nueva"}</span></div><div className="product-photo-name">{file.name}</div><div className="product-photo-actions">{form.imagenes.length === 0 && index > 0 && <button type="button" aria-label={`Usar archivo ${index + 1} como portada`} onClick={() => { setFiles(old => [old[index], ...old.filter((_, i) => i !== index)]); setForm(f => ({ ...f, archivos: [f.archivos[index], ...f.archivos.filter((_, i) => i !== index)] })) }}>Portada</button>}<button type="button" aria-label={`Quitar archivo ${index + 1}`} onClick={() => removeFile(index)}>Quitar</button></div></div>)}
          </div> : <p className="product-help">Sin fotos</p>}
          <div className="product-upload"><button type="button" className="product-secondary" disabled={form.imagenes.length + files.length >= 20} onClick={() => fileInput.current?.click()}>Agregar fotos</button><input ref={fileInput} type="file" aria-label="Subir imágenes desde mi equipo" multiple accept="image/png,image/jpeg,image/webp" className="sr-only" tabIndex={-1} onChange={e => void selectFiles([...files, ...e.target.files ?? []])} /><span className="product-help">PNG, JPG o WebP · Hasta 5 MB por foto</span></div>
          <details className="mt-3"><summary className="product-link cursor-pointer">Agregar desde un enlace</summary><div className="product-fields mt-2"><label className="product-label">Enlace de la imagen<input type="url" maxLength={500} className={field} placeholder="https://…" value={link} onChange={e => { setLink(e.target.value); setLinkError("") }} /></label><button type="button" className="product-secondary justify-self-start" disabled={!link.trim() || form.imagenes.length + files.length >= 20} onClick={addLink}>Agregar imagen</button>{linkError && <p role="alert" className="product-error">{linkError}</p>}</div></details>
          <p role="status" className="product-help">{hashing ? "Preparando imágenes..." : files.length ? `${files.length} imagen(es) para guardar junto al producto.` : "Hasta 5 fotos nuevas por guardado · 19 MB en total · 20 imágenes por producto"}</p>
          {files.length > 0 && form.imagenes.length > 0 && <p className="product-help">Las fotos nuevas se agregan al final. Podés elegir una como portada después de guardar.</p>}
          {fileError && <div role="alert" className="product-error mt-3">{fileError}<button type="button" className="product-link ml-2" onClick={() => { setFileError(""); if (fileInput.current) fileInput.current.value = "" }}>Cerrar aviso</button></div>}
        </section></div>
<div className="product-editor-column"><section className="product-panel" aria-labelledby="product-price-title"><h2 id="product-price-title" className="product-section-title">Precio y envío</h2><div className="product-fields">
          <label className="product-label">Moneda del artículo<select aria-label="Moneda del artículo" required className={field} value={form.precio.moneda} onChange={e => setPrice("moneda", e.target.value)}><option value="">Elegir moneda</option><option value="ARS">Pesos argentinos (ARS)</option><option value="USD">Dólares (USD)</option></select></label>
          <label className="product-label">Precio de venta<input aria-label="Precio de venta" required inputMode="decimal" aria-describedby="catalogo-decimal-ayuda" className={field} placeholder="Ej. 12500,50" value={form.precio.precioOriginal} onChange={e => setPrice("precioOriginal", e.target.value)} />{form.precio.precioOriginal && !amount && <span role="alert" className="product-help text-red-700">Usá hasta dos decimales, sin separadores de miles.</span>}<span id="catalogo-decimal-ayuda" className="product-help">Sin separadores de miles. Ej.: 12500,50.</span></label>
          {form.precio.moneda === "USD" && <div className="product-help">Cotización de tu tienda: {config.cotizacionUsd == null ? "Sin configurar" : `${cotizacionDosDecimales(config.cotizacionUsd).replace(".", ",")} pesos por USD`}. <button type="button" className="product-link" onClick={() => configuration("catalogo-configuracion")}>Administrar cotización</button></div>}
          <label className="product-label">¿El precio ya incluye envío?<select required className={field} value={form.precio.incluyeEnvio === null ? "" : String(form.precio.incluyeEnvio)} onChange={e => setForm(f => ({ ...f, precio: { ...f.precio, incluyeEnvio: e.target.value === "" ? null : e.target.value === "true", categoriaEnvioId: null } }))}><option value="">Elegir una opción</option><option value="true">Sí, envío incluido</option><option value="false">No, sumar el costo de envío</option></select></label>
          {form.precio.incluyeEnvio === false && <div><label className="product-label">Categoría de envío de tu tienda<select required className={field} value={form.precio.categoriaEnvioId ?? ""} onChange={e => setPrice("categoriaEnvioId", e.target.value || null)}><option value="">Elegir categoría</option>{categories.map(c => <option key={c.id} value={c.id}>{c.nombre} · {dineroCatalogo(c.costoArs)}</option>)}</select></label><button type="button" className="product-link" onClick={() => configuration("catalogo-envios")}>{categories.length ? "Administrar categorías de envío" : "Crear mi primera categoría de envío"}</button></div>}
        </div><div aria-live="polite" className="product-price-summary">
          {!canPreview ? <p className="product-help">Completá nombre, SKU, moneda, precio y envío para revisar el producto.</p> : preview.isFetching ? <p role="status" className="product-help">Revisando precio y ficha...</p> : preview.isError ? <p role="alert" className="product-error">{catalogoError(preview.error)}</p> : calculated?.estado === "PENDIENTE" ? <ul className="list-disc pl-4 text-amber-800">{calculated.pendientes.map(p => <li key={p}>{p}</li>)}</ul> : calculated && <><p className="product-label">Precio público</p><p className="product-price-value">{dineroCatalogo(calculated.precioPublicoArs)}</p><p className="product-help">Pesos argentinos · {form.precio.incluyeEnvio ? "Envío incluido" : "Con envío sumado"}</p>{(form.precio.moneda === "USD" || form.precio.incluyeEnvio === false) && <dl className="product-price-breakdown"><div><dt>{form.precio.moneda === "USD" ? "Artículo convertido" : "Artículo"}</dt><dd>{dineroCatalogo(calculated.baseArs)}</dd></div>{form.precio.incluyeEnvio === false && <div><dt>Envío adicional</dt><dd>{dineroCatalogo(calculated.envioArs)}</dd></div>}</dl>}</>}
        </div></section>
<section className="product-panel" aria-labelledby="product-publish-title"><h2 id="product-publish-title" className="product-section-title">Publicación</h2><label className="product-check"><input type="checkbox" checked={form.activo} onChange={e => setForm(f => ({ ...f, activo: e.target.checked }))} />Producto activo</label><p className="product-help">{!form.activo ? "Se guardará inactivo y no aparecerá en el catálogo." : !modoCatalogo ? "La tienda aún está en preparación. Este precio se publicará cuando se habilite el catálogo." : calculated?.estado === "LISTO" && !preview.isFetching && !preview.isError ? "Aparecerá en tu catálogo al guardar." : "Completá el precio y el envío para poder publicarlo."}</p><p className="product-help">{canPreview && calculated?.estado === "LISTO" && !preview.isFetching && !preview.isError ? "Precio configurado" : "Precio pendiente de revisión"}</p><div className="mt-5 border-t border-slate-100 pt-4"><p className="product-label">Etiquetas</p>{([['promocion', 'Promoción'], ['novedad', 'Novedad']] as const).map(([key, label]) => <label key={key} className="product-check"><input type="checkbox" checked={form[key]} onChange={e => setForm(f => ({ ...f, [key]: e.target.checked }))} />{label}</label>)}</div></section></div>
</fieldset>
    {save.isError && <div ref={errorNotice} tabIndex={-1} role="alert" className="product-error mt-5">{catalogoError(save.error)} <button type="button" className="product-link ml-2" onClick={() => { save.reset(); setReload(n => n + 1) }}>Revisar nuevamente</button>{saved && <p>Si la ficha cambió en otra sesión, volvé a cargarla desde Productos. Tu borrador se conserva hasta que decidas descartarlo.</p>}</div>}
    {refreshError && <p role="alert" className="product-help">El producto quedó guardado, pero no pudimos actualizar la lista. <button type="button" className="product-link" onClick={() => { void refresh().then(() => setRefreshError(false)).catch(() => setRefreshError(true)) }}>Actualizar lista</button></p>}
  </form>
}

function ProductPhoto({ src, file, alt }: { src?: string; file?: File; alt: string }) {
  const [failed, setFailed] = useState(false)
  const image = useRef<HTMLImageElement>(null)
  useEffect(() => {
    if (!file) return
    const url = URL.createObjectURL(file)
    if (image.current) image.current.src = url
    return () => URL.revokeObjectURL(url)
  }, [file])
  return failed ? <span className="product-photo-fallback" role="img" aria-label={alt}>No se pudo cargar la imagen</span> : <img ref={image} src={src} alt={alt} onError={() => setFailed(true)} />
}
