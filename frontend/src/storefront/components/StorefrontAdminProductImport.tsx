import { useMemo, useRef, useState } from "react"
import { useMutation } from "@tanstack/react-query"
import axios from "axios"
import {
  importarProductosComoAdmin,
  type AdminImportImageMode,
  type AdminProductoImportRequest,
  type AdminProductoImportResult,
  type CategoriaProducto,
  type ProductoPropio,
} from "../api/storefrontOwnerApi"

type ImportStatus = "NUEVO" | "ACTUALIZAR" | "REVISAR"
type ImportFilter = "TODOS" | ImportStatus

type ImportDraft = {
  key: string
  fila: number
  incluir: boolean
  sku: string
  nombre: string
  descripcion: string
  precioVenta: number
  precioValido: boolean
  stock: number
  stockValido: boolean
  categoriaPaquete: string
  categoriaProductoId: string | null
  categoriaRaw: string
  activo: boolean
  promocion: boolean
  novedad: boolean
  imagenes: string[]
  modoImagenes: AdminImportImageMode
}

type UpdateOptions = Omit<AdminProductoImportRequest, "items">

const inputClass = "w-full rounded-xl border border-slate-200 bg-white px-3 py-2 text-sm outline-none focus:border-violet-400 focus:ring-2 focus:ring-violet-100"
const money = new Intl.NumberFormat("es-AR", { style: "currency", currency: "ARS" })
const MAX_ROWS = 2000
const MAX_FILE_SIZE = 5 * 1024 * 1024

const defaultUpdateOptions: UpdateOptions = {
  actualizarNombre: true,
  actualizarDescripcion: true,
  actualizarPrecio: true,
  actualizarStock: true,
  actualizarCategoria: true,
  actualizarCategoriaPaquete: true,
  actualizarEstado: true,
  actualizarDestacados: true,
  actualizarImagenes: true,
}

const updateOptionLabels: { key: keyof UpdateOptions; label: string }[] = [
  { key: "actualizarNombre", label: "Título" },
  { key: "actualizarDescripcion", label: "Descripción" },
  { key: "actualizarPrecio", label: "Importe" },
  { key: "actualizarStock", label: "Stock" },
  { key: "actualizarCategoria", label: "Categoría" },
  { key: "actualizarCategoriaPaquete", label: "Tipo de paquete" },
  { key: "actualizarEstado", label: "Estado activo" },
  { key: "actualizarDestacados", label: "Promoción y novedad" },
  { key: "actualizarImagenes", label: "Imágenes" },
]

function normalize(value: string) {
  return value.normalize("NFD").replace(/[\u0300-\u036f]/g, "").toLowerCase().replace(/[^a-z0-9]/g, "")
}

function findColumn(headers: string[], aliases: string[]) {
  const normalizedAliases = aliases.map(normalize)
  const index = headers.findIndex((header) => normalizedAliases.includes(normalize(header)))
  return index >= 0 ? index : null
}

function detectDelimiter(text: string) {
  const firstLine = text.replace(/^\uFEFF/, "").split(/\r?\n/, 1)[0] ?? ""
  const counts = [",", ";", "\t"].map((delimiter) => ({ delimiter, count: firstLine.split(delimiter).length - 1 }))
  return counts.sort((a, b) => b.count - a.count)[0]?.delimiter ?? ","
}

function parseCsv(text: string) {
  const source = text.replace(/^\uFEFF/, "")
  const delimiter = detectDelimiter(source)
  const rows: string[][] = []
  let row: string[] = []
  let cellValue = ""
  let quoted = false

  for (let index = 0; index < source.length; index += 1) {
    const char = source[index]
    const next = source[index + 1]
    if (char === '"') {
      if (quoted && next === '"') {
        cellValue += '"'
        index += 1
      } else {
        quoted = !quoted
      }
    } else if (char === delimiter && !quoted) {
      row.push(cellValue)
      cellValue = ""
    } else if ((char === "\n" || char === "\r") && !quoted) {
      if (char === "\r" && next === "\n") index += 1
      row.push(cellValue)
      if (row.some((value) => value.trim())) rows.push(row)
      row = []
      cellValue = ""
    } else {
      cellValue += char
    }
  }
  if (quoted) throw new Error("El CSV contiene una comilla sin cerrar.")
  if (cellValue.length || row.length) {
    row.push(cellValue)
    if (row.some((value) => value.trim())) rows.push(row)
  }
  return rows
}

function parseNumber(value: string | undefined) {
  const raw = value?.trim().replace(/\s/g, "").replace(/\$/g, "") ?? ""
  if (!raw) return { value: 0, valid: false }
  let normalized = raw
  if (raw.includes(",") && raw.includes(".")) {
    normalized = raw.lastIndexOf(",") > raw.lastIndexOf(".")
      ? raw.replace(/\./g, "").replace(",", ".")
      : raw.replace(/,/g, "")
  } else if (raw.includes(",")) {
    normalized = raw.replace(",", ".")
  }
  const parsed = Number(normalized)
  return { value: Number.isFinite(parsed) ? parsed : 0, valid: Number.isFinite(parsed) }
}

function parseBoolean(value: string | undefined, fallback: boolean) {
  if (value == null || !value.trim()) return fallback
  const normalized = normalize(value)
  if (["1", "true", "si", "yes", "activo", "x"].includes(normalized)) return true
  if (["0", "false", "no", "inactivo"].includes(normalized)) return false
  return fallback
}

function isValidImageUrl(value: string) {
  if (!value.trim()) return true
  try {
    const parsed = new URL(value.trim())
    return parsed.protocol === "http:" || parsed.protocol === "https:"
  } catch {
    return false
  }
}

function cell(row: string[], index: number | null) {
  return index == null ? undefined : row[index]?.trim()
}

function errorMessage(error: unknown) {
  if (axios.isAxiosError(error)) return error.response?.data?.message ?? "No se pudo completar la importación."
  return error instanceof Error ? error.message : "No se pudo completar la importación."
}

function ImagePreview({ url }: { url: string }) {
  const [state, setState] = useState<"loading" | "ok" | "error">("loading")
  if (!url.trim()) return <div className="flex h-20 w-20 items-center justify-center rounded-xl bg-slate-100 text-center text-[11px] text-slate-400">Sin imagen</div>
  return <div className="space-y-1">
    <a href={url} target="_blank" rel="noreferrer" className="block h-20 w-20 overflow-hidden rounded-xl border border-slate-200 bg-slate-50">
      <img key={url} src={url} alt="Vista previa" className="h-full w-full object-contain" onLoad={() => setState("ok")} onError={() => setState("error")} />
    </a>
    <p className={`text-[10px] font-semibold ${state === "error" ? "text-rose-600" : state === "ok" ? "text-emerald-600" : "text-slate-400"}`}>
      {state === "error" ? "No carga" : state === "ok" ? "Imagen válida" : "Validando..."}
    </p>
  </div>
}

export default function StorefrontAdminProductImport({
  revendedorId,
  products,
  categories,
  refresh,
  notify,
}: {
  revendedorId: string
  products: ProductoPropio[]
  categories: CategoriaProducto[]
  refresh: () => Promise<unknown>
  notify: (message: string) => void
}) {
  const fileInputRef = useRef<HTMLInputElement>(null)
  const [fileName, setFileName] = useState("")
  const [drafts, setDrafts] = useState<ImportDraft[]>([])
  const [filter, setFilter] = useState<ImportFilter>("TODOS")
  const [options, setOptions] = useState<UpdateOptions>(defaultUpdateOptions)
  const [parseError, setParseError] = useState<string | null>(null)
  const [result, setResult] = useState<AdminProductoImportResult | null>(null)

  const existingBySku = useMemo(() => new Map(products.map((product) => [product.sku.trim().toLowerCase(), product])), [products])
  const skuCounts = useMemo(() => {
    const counts = new Map<string, number>()
    drafts.forEach((draft) => {
      const sku = draft.sku.trim().toLowerCase()
      if (sku) counts.set(sku, (counts.get(sku) ?? 0) + 1)
    })
    return counts
  }, [drafts])

  const issuesFor = (draft: ImportDraft) => {
    const issues: string[] = []
    const sku = draft.sku.trim().toLowerCase()
    if (!sku) issues.push("Falta SKU")
    if (sku && (skuCounts.get(sku) ?? 0) > 1) issues.push("SKU repetido en el CSV")
    if (!draft.nombre.trim()) issues.push("Falta título")
    if (!draft.precioValido || draft.precioVenta < 0) issues.push("Importe inválido")
    if (!draft.stockValido || draft.stock < 0 || !Number.isInteger(draft.stock)) issues.push("Stock inválido")
    if (!["CHICO", "MEDIANO", "VOLUMINOSO"].includes(draft.categoriaPaquete)) issues.push("Tipo de paquete inválido")
    if (draft.categoriaRaw && !draft.categoriaProductoId) issues.push(`Categoría no encontrada: ${draft.categoriaRaw}`)
    if (draft.imagenes.some((url) => !isValidImageUrl(url))) issues.push("Hay una URL de imagen inválida")
    const existing = existingBySku.get(sku)
    if (existing && draft.modoImagenes !== "CONSERVAR") {
      const currentUrls = new Set(existing.media.filter((media) => media.tipoMedia === "IMAGEN").map((media) => media.url))
      const newUrls = new Set(draft.imagenes.map((url) => url.trim()).filter((url) => url && !currentUrls.has(url)))
      if (currentUrls.size + newUrls.size > 5) issues.push("El producto superaría el máximo de 5 imágenes")
    }
    return issues
  }
  const statusFor = (draft: ImportDraft): ImportStatus => {
    if (issuesFor(draft).length) return "REVISAR"
    return existingBySku.has(draft.sku.trim().toLowerCase()) ? "ACTUALIZAR" : "NUEVO"
  }
  const totals = drafts.reduce((acc, draft) => {
    acc[statusFor(draft)] += 1
    return acc
  }, { NUEVO: 0, ACTUALIZAR: 0, REVISAR: 0 })
  const visibleDrafts = drafts.filter((draft) => filter === "TODOS" || statusFor(draft) === filter)
  const readySelected = drafts.filter((draft) => draft.incluir && statusFor(draft) !== "REVISAR").length

  const mutation = useMutation({
    mutationFn: () => importarProductosComoAdmin(revendedorId, {
      ...options,
      items: drafts.map((draft) => ({
        fila: draft.fila,
        incluir: draft.incluir && statusFor(draft) !== "REVISAR",
        sku: draft.sku.trim(),
        nombre: draft.nombre.trim(),
        descripcion: draft.descripcion,
        precioVenta: draft.precioVenta,
        stock: draft.stock,
        categoriaPaquete: draft.categoriaPaquete,
        categoriaProductoId: draft.categoriaProductoId,
        activo: draft.activo,
        promocion: draft.promocion,
        novedad: draft.novedad,
        imagenes: draft.imagenes.map((url) => url.trim()).filter(Boolean),
        modoImagenes: draft.modoImagenes,
      })),
    }),
    onSuccess: async (response) => {
      setResult(response)
      await refresh()
      notify(`Importación terminada: ${response.creados} nuevos y ${response.actualizados} actualizados.`)
    },
  })

  const updateDraft = (key: string, patch: Partial<ImportDraft>) => {
    setDrafts((current) => current.map((draft) => draft.key === key ? { ...draft, ...patch } : draft))
  }

  const loadCsv = async (file: File) => {
    setParseError(null)
    setResult(null)
    if (!file.name.toLowerCase().endsWith(".csv")) {
      setParseError("Para esta prueba privada usá un archivo CSV.")
      return
    }
    if (file.size > MAX_FILE_SIZE) {
      setParseError("El CSV supera los 5 MB permitidos.")
      return
    }
    try {
      const rows = parseCsv(await file.text())
      if (rows.length < 2) throw new Error("El CSV no contiene productos.")
      if (rows.length - 1 > MAX_ROWS) throw new Error(`El CSV supera el límite de ${MAX_ROWS} productos.`)
      const headers = rows[0]
      const columns = {
        sku: findColumn(headers, ["sku", "codigo", "codigo producto"]),
        nombre: findColumn(headers, ["titulo", "nombre", "producto"]),
        descripcion: findColumn(headers, ["descripcion", "detalle"]),
        precio: findColumn(headers, ["importe", "precio", "precio venta", "precio final"]),
        stock: findColumn(headers, ["stock", "cantidad"]),
        categoria: findColumn(headers, ["categoria", "rubro"]),
        paquete: findColumn(headers, ["tipo paquete", "tipo_paquete", "categoria paquete", "paquete"]),
        activo: findColumn(headers, ["activo", "publicado"]),
        promocion: findColumn(headers, ["promocion", "es promocion"]),
        novedad: findColumn(headers, ["novedad", "es novedad"]),
        imagenes: Array.from({ length: 5 }, (_, index) => findColumn(headers, index === 0
          ? ["imagen", "imagen url", "url imagen", "imagen 1", "imagen_1"]
          : [`imagen ${index + 1}`, `imagen_${index + 1}`, `imagen url ${index + 1}`])),
      }
      const missing = [columns.sku == null ? "sku" : "", columns.nombre == null ? "titulo" : "", columns.precio == null ? "importe" : ""].filter(Boolean)
      if (missing.length) throw new Error(`Faltan columnas obligatorias: ${missing.join(", ")}.`)

      const categoryMap = new Map(categories.map((category) => [normalize(category.nombre), category.id]))
      const parsed = rows.slice(1).map((row, index): ImportDraft => {
        const sku = cell(row, columns.sku) ?? ""
        const existing = existingBySku.get(sku.trim().toLowerCase())
        const priceCell = cell(row, columns.precio)
        const stockCell = cell(row, columns.stock)
        const parsedPrice = priceCell == null ? { value: existing?.precioVenta ?? 0, valid: Boolean(existing) } : parseNumber(priceCell)
        const parsedStock = stockCell == null ? { value: existing?.stock ?? 0, valid: true } : parseNumber(stockCell)
        const categoryRaw = cell(row, columns.categoria) ?? ""
        const packageRaw = (cell(row, columns.paquete) || existing?.categoriaPaquete || "MEDIANO").trim().toUpperCase()
        return {
          key: `${index + 2}-${crypto.randomUUID()}`,
          fila: index + 2,
          incluir: true,
          sku,
          nombre: cell(row, columns.nombre) || existing?.nombre || "",
          descripcion: cell(row, columns.descripcion) ?? existing?.descripcion ?? "",
          precioVenta: parsedPrice.value,
          precioValido: parsedPrice.valid,
          stock: parsedStock.value,
          stockValido: parsedStock.valid,
          categoriaPaquete: packageRaw,
          categoriaProductoId: categoryRaw ? categoryMap.get(normalize(categoryRaw)) ?? null : existing?.categoriaProductoId ?? null,
          categoriaRaw: categoryRaw && !categoryMap.has(normalize(categoryRaw)) ? categoryRaw : "",
          activo: parseBoolean(cell(row, columns.activo), existing?.activo ?? true),
          promocion: parseBoolean(cell(row, columns.promocion), existing?.promocion ?? false),
          novedad: parseBoolean(cell(row, columns.novedad), existing?.novedad ?? false),
          imagenes: columns.imagenes.map((column) => cell(row, column) ?? ""),
          modoImagenes: existing ? "CONSERVAR" : "AGREGAR",
        }
      })
      setFileName(file.name)
      setDrafts(parsed)
      setFilter("TODOS")
    } catch (error) {
      setParseError(errorMessage(error))
      setDrafts([])
      setFileName("")
    }
  }

  const downloadTemplate = () => {
    const content = [
      "sku,titulo,descripcion,importe,stock,categoria,tipo_paquete,activo,promocion,novedad,imagen_1,imagen_2,imagen_3,imagen_4,imagen_5",
      'NB-CQ40,"Notebook Compaq CQ40 lista para trabajar","4GB de RAM, SSD de 128GB e Intel Celeron.",199900.00,3,Notebooks,MEDIANO,SI,NO,SI,https://ejemplo.com/compaq-cq40.jpg,,,,',
    ].join("\r\n")
    const url = URL.createObjectURL(new Blob([content], { type: "text/csv;charset=utf-8" }))
    const anchor = document.createElement("a")
    anchor.href = url
    anchor.download = "plantilla-productos-storefront.csv"
    anchor.click()
    URL.revokeObjectURL(url)
  }

  return <details className="overflow-hidden rounded-2xl border border-violet-200 bg-violet-50/50" open={drafts.length > 0}>
    <summary className="cursor-pointer list-none px-5 py-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div><p className="text-xs font-bold uppercase tracking-[.2em] text-violet-700">Solo administrador MD</p><h2 className="mt-1 font-semibold text-slate-950">Importación masiva de productos</h2></div>
        <span className="rounded-full bg-violet-700 px-3 py-1 text-[10px] font-bold uppercase tracking-wider text-white">Prueba privada</span>
      </div>
    </summary>
    <div className="space-y-5 border-t border-violet-200 bg-white p-5">
      <div className="flex flex-wrap items-center gap-3">
        <input ref={fileInputRef} type="file" accept=".csv,text/csv" className="hidden" onChange={(event) => { const file = event.target.files?.[0]; if (file) void loadCsv(file); event.target.value = "" }} />
        <button type="button" onClick={() => fileInputRef.current?.click()} className="rounded-xl bg-slate-950 px-4 py-2.5 text-sm font-semibold text-white hover:bg-slate-800">Seleccionar CSV</button>
        <button type="button" onClick={downloadTemplate} className="rounded-xl border border-slate-200 px-4 py-2.5 text-sm font-semibold text-slate-700 hover:border-slate-400">Descargar plantilla</button>
        <span className="text-sm text-slate-500">{fileName || "Todavía no seleccionaste un archivo"}</span>
      </div>
      {parseError ? <div className="rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">{parseError}</div> : null}
      {mutation.isError ? <div className="rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">{errorMessage(mutation.error)}</div> : null}
      {result ? <div className={`rounded-xl border px-4 py-3 text-sm ${result.rechazados ? "border-amber-200 bg-amber-50 text-amber-800" : "border-emerald-200 bg-emerald-50 text-emerald-800"}`}>
        Creados: <strong>{result.creados}</strong> · Actualizados: <strong>{result.actualizados}</strong> · Omitidos: <strong>{result.omitidos}</strong> · Rechazados: <strong>{result.rechazados}</strong>
        {result.errores.length ? <ul className="mt-2 list-disc pl-5">{result.errores.slice(0, 10).map((error) => <li key={`${error.fila}-${error.sku}`}>Fila {error.fila} ({error.sku || "sin SKU"}): {error.mensaje}</li>)}</ul> : null}
      </div> : null}

      {drafts.length ? <>
        <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
          <p className="text-xs font-bold uppercase tracking-wider text-slate-500">En productos existentes, elegí qué campos actualizar</p>
          <div className="mt-3 flex flex-wrap gap-3">{updateOptionLabels.map((option) => <label key={option.key} className="flex items-center gap-2 text-sm text-slate-700"><input type="checkbox" checked={options[option.key]} onChange={(event) => setOptions((current) => ({ ...current, [option.key]: event.target.checked }))} />{option.label}</label>)}</div>
        </div>
        <div className="flex flex-wrap items-center justify-between gap-3">
          <div className="flex flex-wrap gap-2">{([
            ["TODOS", `Todos (${drafts.length})`], ["NUEVO", `Nuevos (${totals.NUEVO})`], ["ACTUALIZAR", `Actualizar (${totals.ACTUALIZAR})`], ["REVISAR", `Revisar (${totals.REVISAR})`],
          ] as [ImportFilter, string][]).map(([value, label]) => <button type="button" key={value} onClick={() => setFilter(value)} className={`rounded-full px-3 py-2 text-xs font-bold ${filter === value ? "bg-slate-950 text-white" : "bg-slate-100 text-slate-600 hover:bg-slate-200"}`}>{label}</button>)}</div>
          <button type="button" disabled={!readySelected || mutation.isPending} onClick={() => mutation.mutate()} className="rounded-xl bg-violet-700 px-5 py-2.5 text-sm font-semibold text-white hover:bg-violet-800 disabled:cursor-not-allowed disabled:opacity-50">{mutation.isPending ? "Importando..." : `Confirmar ${readySelected} producto${readySelected === 1 ? "" : "s"}`}</button>
        </div>

        <div className="overflow-x-auto rounded-2xl border border-slate-200">
          <table className="min-w-[1320px] w-full text-left text-sm">
            <thead className="bg-slate-50 text-xs uppercase tracking-wider text-slate-500"><tr><th className="px-3 py-3">Importar</th><th className="px-3 py-3">Estado / SKU</th><th className="px-3 py-3">Catálogo actual</th><th className="px-3 py-3">Datos nuevos editables</th><th className="px-3 py-3">Clasificación</th><th className="px-3 py-3">Imágenes</th></tr></thead>
            <tbody className="divide-y divide-slate-100">{visibleDrafts.map((draft) => {
              const status = statusFor(draft)
              const issues = issuesFor(draft)
              const existing = existingBySku.get(draft.sku.trim().toLowerCase())
              return <tr key={draft.key} className="align-top">
                <td className="px-3 py-4"><input type="checkbox" checked={draft.incluir} disabled={status === "REVISAR"} onChange={(event) => updateDraft(draft.key, { incluir: event.target.checked })} /></td>
                <td className="w-48 space-y-2 px-3 py-4"><span className={`inline-flex rounded-full px-2 py-1 text-[10px] font-bold ${status === "NUEVO" ? "bg-emerald-50 text-emerald-700" : status === "ACTUALIZAR" ? "bg-blue-50 text-blue-700" : "bg-amber-50 text-amber-700"}`}>{status}</span><input className={inputClass} value={draft.sku} onChange={(event) => updateDraft(draft.key, { sku: event.target.value })} />{issues.map((issue) => <p key={issue} className="text-xs text-amber-700">{issue}</p>)}</td>
                <td className="w-64 px-3 py-4">{existing ? <div className="space-y-2 text-slate-600"><p className="font-semibold text-slate-900">{existing.nombre}</p><p className="line-clamp-3 text-xs">{existing.descripcion || "Sin descripción"}</p><p className="font-semibold">{existing.precioVenta === null ? "Precio no disponible" : money.format(existing.precioVenta)}</p><p className="text-xs">Stock {existing.stock} · {existing.categoriaProductoNombre || "Sin categoría"}</p></div> : <span className="text-slate-400">Producto nuevo</span>}</td>
                <td className="w-[360px] space-y-2 px-3 py-4"><input className={inputClass} value={draft.nombre} onChange={(event) => updateDraft(draft.key, { nombre: event.target.value })} placeholder="Título" /><textarea rows={3} className={`${inputClass} resize-y`} value={draft.descripcion} onChange={(event) => updateDraft(draft.key, { descripcion: event.target.value })} placeholder="Descripción" /><div className="grid grid-cols-2 gap-2"><label className="text-xs text-slate-500">Importe<input type="number" min={0} step="0.01" className={`${inputClass} mt-1`} value={draft.precioVenta} onChange={(event) => updateDraft(draft.key, { precioVenta: Number(event.target.value), precioValido: event.target.value !== "" && Number.isFinite(Number(event.target.value)) })} /></label><label className="text-xs text-slate-500">Stock<input type="number" min={0} step="1" className={`${inputClass} mt-1`} value={draft.stock} onChange={(event) => updateDraft(draft.key, { stock: Number(event.target.value), stockValido: event.target.value !== "" && Number.isInteger(Number(event.target.value)) })} /></label></div></td>
                <td className="w-56 space-y-2 px-3 py-4"><select className={inputClass} value={draft.categoriaProductoId ?? ""} onChange={(event) => updateDraft(draft.key, { categoriaProductoId: event.target.value || null, categoriaRaw: "" })}><option value="">Sin categoría</option>{categories.map((category) => <option key={category.id} value={category.id}>{category.nombre}</option>)}</select><select className={inputClass} value={draft.categoriaPaquete} onChange={(event) => updateDraft(draft.key, { categoriaPaquete: event.target.value })}><option value="CHICO">Chico</option><option value="MEDIANO">Mediano</option><option value="VOLUMINOSO">Voluminoso</option></select><div className="grid grid-cols-3 gap-1 text-[11px] text-slate-600"><label><input type="checkbox" checked={draft.activo} onChange={(event) => updateDraft(draft.key, { activo: event.target.checked })} /> Activo</label><label><input type="checkbox" checked={draft.promocion} onChange={(event) => updateDraft(draft.key, { promocion: event.target.checked })} /> Promo</label><label><input type="checkbox" checked={draft.novedad} onChange={(event) => updateDraft(draft.key, { novedad: event.target.checked })} /> Nuevo</label></div></td>
                <td className="w-[330px] space-y-3 px-3 py-4"><div className="flex flex-wrap gap-2">{draft.imagenes.filter(Boolean).map((url, index) => <ImagePreview key={`${index}-${url}`} url={url} />)}{!draft.imagenes.some(Boolean) ? <ImagePreview url="" /> : null}</div><div className="space-y-1">{draft.imagenes.map((url, index) => <input key={index} type="url" className={inputClass} value={url} onChange={(event) => { const next = [...draft.imagenes]; next[index] = event.target.value; updateDraft(draft.key, { imagenes: next }) }} placeholder={`URL imagen ${index + 1}`} />)}</div>{existing ? <select className={inputClass} value={draft.modoImagenes} onChange={(event) => updateDraft(draft.key, { modoImagenes: event.target.value as AdminImportImageMode })}><option value="CONSERVAR">Conservar imágenes actuales</option><option value="AGREGAR">Agregar como nuevas</option><option value="REEMPLAZAR_PORTADA">Usar la primera como portada</option></select> : null}</td>
              </tr>
            })}</tbody>
          </table>
        </div>
      </> : null}
    </div>
  </details>
}
