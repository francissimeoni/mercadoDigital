import axios from "axios"
import { useEffect, useMemo, useState, type ChangeEvent, type ReactNode } from "react"
import { Link } from "react-router-dom"
import ProveedorMetricCard from "../../components/proveedor/ProveedorMetricCard"
import ProveedorModuleHeader from "../../components/proveedor/ProveedorModuleHeader"
import FeedbackAlert from "../../components/ui/FeedbackAlert"
import { useProveedorContext } from "../../hooks/useProveedorContext"
import { useImportarProveedorProductos } from "../../hooks/useProveedorProductos"
import type { ProveedorProductoImportacionResponse } from "../../types/proveedor"

const inputClassName =
  "mt-2 block w-full rounded-2xl border border-slate-300 bg-white px-4 py-3 text-sm text-slate-900 outline-none transition file:mr-4 file:rounded-full file:border-0 file:bg-slate-950 file:px-4 file:py-2 file:text-sm file:font-semibold file:text-white hover:file:bg-slate-800 focus:border-slate-400 focus:ring-2 focus:ring-slate-200"

const acceptedExtensions = [".csv", ".xlsx"]
const requiredImportFields = [
  "Codigo",
  "Descripcion",
  "precio",
  "Tipo",
  "IVA",
  "ROS",
  "MZA",
  "CBA",
  "LUG",
  "Grupo",
  "Rubro",
  "Part Number",
] as const

const fieldMeta: Record<RequiredImportField, { title: string; helper: string }> = {
  Codigo: {
    title: "SKU / Codigo",
    helper: "Elegi la columna origen que la plataforma debe usar como codigo del producto.",
  },
  Descripcion: {
    title: "Descripcion",
    helper: "Elegi la columna origen que contiene la descripcion o nombre comercial del producto.",
  },
  precio: {
    title: "Precio",
    helper: "Elegi la columna origen que contiene el precio de costo o precio base a importar.",
  },
  Tipo: {
    title: "Tipo",
    helper: "Elegi la columna origen que representa el tipo de articulo que llega en el archivo.",
  },
  IVA: {
    title: "IVA",
    helper: "Elegi la columna origen con la alicuota o valor de IVA.",
  },
  ROS: {
    title: "Stock ROS",
    helper: "Elegi la columna origen con stock o unidades de Rosario.",
  },
  MZA: {
    title: "Stock MZA",
    helper: "Elegi la columna origen con stock o unidades de Mendoza.",
  },
  CBA: {
    title: "Stock CBA",
    helper: "Elegi la columna origen con stock o unidades de Cordoba.",
  },
  LUG: {
    title: "Stock LUG",
    helper: "Elegi la columna origen con stock o unidades de la columna LUG.",
  },
  Grupo: {
    title: "Grupo",
    helper: "Elegi la columna origen con el grupo comercial del producto.",
  },
  Rubro: {
    title: "Rubro",
    helper: "Elegi la columna origen con el rubro o categoria del proveedor.",
  },
  "Part Number": {
    title: "Part Number",
    helper: "Elegi la columna origen que contiene el part number o identificador tecnico.",
  },
}

const destinationGroups = [
  {
    key: "sku",
    title: "SKU",
    apiField: "sku",
    helper: "Dato destino que identifica univocamente al producto del proveedor.",
    fields: ["Codigo"] as RequiredImportField[],
    required: true,
  },
  {
    key: "descripcion",
    title: "Descripcion",
    apiField: "nombre / descripcion",
    helper: "Dato destino que la plataforma usa para nombre y descripcion del producto importado.",
    fields: ["Descripcion"] as RequiredImportField[],
    required: true,
  },
  {
    key: "precio",
    title: "Precio costo",
    apiField: "precioCosto",
    helper: "Dato destino que la plataforma usa como precio de costo del producto.",
    fields: ["precio"] as RequiredImportField[],
    required: true,
  },
  {
    key: "stock",
    title: "Stock total",
    apiField: "stock",
    helper: "Dato destino que la plataforma calcula sumando las columnas de stock del archivo origen.",
    fields: ["ROS", "MZA", "CBA", "LUG"] as RequiredImportField[],
    required: true,
  },
  {
    key: "tipo",
    title: "Tipo",
    apiField: "metadata auxiliar",
    helper: "Dato auxiliar disponible para acompanar la importacion. Hoy no bloquea el alta si falta.",
    fields: ["Tipo"] as RequiredImportField[],
    required: false,
  },
  {
    key: "iva",
    title: "IVA",
    apiField: "metadata auxiliar",
    helper: "Dato auxiliar disponible para acompanar la importacion. Hoy no bloquea el alta si falta.",
    fields: ["IVA"] as RequiredImportField[],
    required: false,
  },
  {
    key: "grupo",
    title: "Grupo",
    apiField: "metadata auxiliar",
    helper: "Dato auxiliar disponible para acompanar la importacion. Hoy no bloquea el alta si falta.",
    fields: ["Grupo"] as RequiredImportField[],
    required: false,
  },
  {
    key: "rubro",
    title: "Rubro",
    apiField: "metadata auxiliar",
    helper: "Dato auxiliar disponible para acompanar la importacion. Hoy no bloquea el alta si falta.",
    fields: ["Rubro"] as RequiredImportField[],
    required: false,
  },
  {
    key: "partNumber",
    title: "Part Number",
    apiField: "metadata auxiliar",
    helper: "Dato auxiliar disponible para acompanar la importacion. Hoy no bloquea el alta si falta.",
    fields: ["Part Number"] as RequiredImportField[],
    required: false,
  },
] as const

type RequiredImportField = (typeof requiredImportFields)[number]

type FeedbackState = {
  variant: "error" | "success" | "info"
  title: string
  message: string
}

type CsvPreview = {
  headers: CsvHeaderOption[]
  suggestedMapping: Record<RequiredImportField, string>
  sampleRows: string[][]
  allRows: string[][]
}

type CsvHeaderOption = {
  value: string
  label: string
  raw: string
  index: number
}

export default function ProveedorCargaExcelPage() {
  const { proveedorId } = useProveedorContext()
  const importacionMutation = useImportarProveedorProductos(proveedorId)
  const [archivo, setArchivo] = useState<File | null>(null)
  const [csvText, setCsvText] = useState<string | null>(null)
  const [feedback, setFeedback] = useState<FeedbackState | null>(null)
  const [resultado, setResultado] = useState<ProveedorProductoImportacionResponse | null>(null)
  const [csvPreview, setCsvPreview] = useState<CsvPreview | null>(null)
  const [mapping, setMapping] = useState<Record<RequiredImportField, string>>(createEmptyMapping())
  const [isPreparingPreview, setIsPreparingPreview] = useState(false)

  const extensionArchivo = archivo ? getFileExtension(archivo.name) : ""
  const archivoValido = archivo ? acceptedExtensions.includes(extensionArchivo) : false
  const isCsv = extensionArchivo === ".csv"

  const totalProcesados = useMemo(() => {
    if (!resultado) return 0
    return resultado.creados + resultado.actualizados + resultado.rechazados
  }, [resultado])

  const missingMappings = useMemo(
    () => requiredImportFields.filter((field) => !mapping[field]),
    [mapping],
  )

  const duplicateMappedHeaders = useMemo(() => {
    const counts = new Map<string, number>()
    for (const header of Object.values(mapping)) {
      if (!header) continue
      counts.set(header, (counts.get(header) ?? 0) + 1)
    }
    return Array.from(counts.entries())
      .filter(([, count]) => count > 1)
      .map(([header]) => header)
  }, [mapping])

  const canSubmit = Boolean(
    proveedorId &&
      archivo &&
      archivoValido &&
      !importacionMutation.isPending &&
      (!isCsv || (csvPreview && csvText !== null && missingMappings.length === 0 && duplicateMappedHeaders.length === 0)),
  )

  useEffect(() => {
    if (!archivo || getFileExtension(archivo.name) !== ".csv") {
      setCsvText(null)
      setCsvPreview(null)
      setMapping(createEmptyMapping())
      return
    }

    const currentArchivo = archivo
    let cancelled = false

    async function loadPreview() {
      setIsPreparingPreview(true)
      try {
        const text = await currentArchivo.text()
        if (cancelled) return

        const preview = buildCsvPreview(text)
        setCsvText(text)
        setCsvPreview(preview)
        setMapping(preview.suggestedMapping)
        setFeedback({
          variant: "info",
          title: "Preview lista",
          message: "Revisa el mapeo detectado y la muestra antes de confirmar la importacion.",
        })
      } catch (error) {
        if (cancelled) return
        setCsvText(null)
        setCsvPreview(null)
        setMapping(createEmptyMapping())
        setFeedback({
          variant: "error",
          title: "No se pudo leer el CSV",
          message: getCsvPreviewErrorMessage(error),
        })
      } finally {
        if (!cancelled) {
          setIsPreparingPreview(false)
        }
      }
    }

    void loadPreview()

    return () => {
      cancelled = true
    }
  }, [archivo])

  async function onFileChange(event: ChangeEvent<HTMLInputElement>) {
    const nextFile = event.target.files?.[0] ?? null
    setArchivo(nextFile)
    setResultado(null)

    if (!nextFile) {
      setFeedback(null)
      return
    }

    if (!acceptedExtensions.includes(getFileExtension(nextFile.name))) {
      setFeedback({
        variant: "error",
        title: "Archivo no admitido",
        message: "Selecciona un archivo con extension .csv o .xlsx para continuar.",
      })
      return
    }

    if (getFileExtension(nextFile.name) === ".xlsx") {
      setFeedback({
        variant: "info",
        title: "Archivo XLSX seleccionado",
        message: "La importacion XLSX sigue disponible, pero la previsualizacion y el mapeo local v1 solo se aplican a CSV.",
      })
      return
    }

    setFeedback(null)
  }

  async function onSubmit() {
    if (!proveedorId) {
      setFeedback({
        variant: "error",
        title: "Proveedor no resuelto",
        message: "No se puede ejecutar la importacion sin `proveedorId`.",
      })
      return
    }

    if (!archivo) {
      setFeedback({
        variant: "error",
        title: "Archivo requerido",
        message: "Selecciona un archivo .csv o .xlsx antes de subirlo.",
      })
      return
    }

    if (!archivoValido) {
      setFeedback({
        variant: "error",
        title: "Extension invalida",
        message: "La validacion minima del frontend solo acepta archivos .csv o .xlsx.",
      })
      return
    }

    if (isCsv) {
      if (!csvPreview || csvText === null) {
        setFeedback({
          variant: "error",
          title: "Preview pendiente",
          message: "Espera a que el sistema termine de leer el CSV antes de importarlo.",
        })
        return
      }

      if (missingMappings.length > 0) {
        setFeedback({
          variant: "error",
          title: "Mapeo incompleto",
          message: `Faltan columnas por asignar: ${missingMappings.join(", ")}.`,
        })
        return
      }

      if (duplicateMappedHeaders.length > 0) {
        setFeedback({
          variant: "error",
          title: "Mapeo duplicado",
          message: `Una misma columna fuente no puede alimentar mas de un campo requerido: ${duplicateMappedHeaders.join(", ")}.`,
        })
        return
      }
    }

    setFeedback(null)

    try {
      const archivoAEnviar =
        isCsv && csvPreview && csvText !== null
          ? buildNormalizedCsvFile(archivo, csvText, csvPreview, mapping)
          : archivo

      const response = await importacionMutation.mutateAsync(archivoAEnviar)
      setResultado(response)
      setFeedback({
        variant: "success",
        title: "Importacion finalizada",
        message: "El backend proceso el archivo y devolvio el resumen operativo de la carga.",
      })
    } catch (error) {
      setResultado(null)
      setFeedback({
        variant: "error",
        title: "No se pudo importar el archivo",
        message: getErrorMessage(error),
      })
    }
  }

  function onReset() {
    setArchivo(null)
    setCsvText(null)
    setResultado(null)
    setCsvPreview(null)
    setMapping(createEmptyMapping())
    setFeedback(null)
  }

  function onDownloadTemplate() {
    const csv = [requiredImportFields.join(","), "SKU-001,Producto ejemplo,150000,GENERAL,21,10,0,5,2,Accesorios,Computacion,PN-001"].join("\n")
    downloadTextFile("plantilla_importacion_productos_proveedor.csv", csv, "text/csv;charset=utf-8;")
  }

  return (
    <section className="space-y-6">
      <ProveedorModuleHeader
        eyebrow="Proveedor"
        title="Importación Masiva"
        description="Actualice SKU, descripción, precios y disponibilidad mediante archivos de datos. El proceso valida la estructura antes de impactar en el catálogo."
        proveedorId={proveedorId}
        aside={
          <>
            <Link
              to="/proveedor/productos"
              className="rounded-full border border-slate-200 px-4 py-2 text-xs font-bold text-slate-600 transition hover:border-slate-300 hover:bg-slate-50"
            >
              Mi Catálogo
            </Link>
            <button
              type="button"
              className="rounded-full border border-slate-200 px-4 py-2 text-xs font-bold text-slate-600 transition hover:border-slate-300 hover:bg-slate-50"
              onClick={onDownloadTemplate}
            >
              Obtener Plantilla
            </button>
            <button
              type="button"
              className="rounded-full bg-slate-950 px-6 py-2.5 text-xs font-bold text-white shadow-sm transition hover:bg-slate-800 disabled:opacity-50"
              onClick={onReset}
              disabled={importacionMutation.isPending}
            >
              Limpiar Proceso
            </button>
          </>
        }
      />

      {!proveedorId ? (
        <FeedbackAlert variant="warning" title="Proveedor no resuelto">
          Falta `proveedorId` para habilitar la importacion del proveedor autenticado.
        </FeedbackAlert>
      ) : null}

      {feedback ? (
        <FeedbackAlert variant={feedback.variant} title={feedback.title}>
          {feedback.message}
        </FeedbackAlert>
      ) : null}

      <div className="grid gap-6 xl:grid-cols-[minmax(0,1.15fr)_minmax(0,0.85fr)]">
        <section className="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm">
          <p className="text-xs font-semibold uppercase tracking-[0.22em] text-slate-500">Archivo</p>
          <h3 className="mt-2 text-2xl font-semibold text-slate-950">Subir importacion proveedor</h3>
          <p className="mt-2 max-w-3xl text-sm leading-6 text-slate-600">
            Para archivos CSV, la pantalla detecta encabezados, te deja mapear columnas y genera un CSV normalizado antes de enviarlo. En esta v1 el foco esta en la oferta del proveedor: SKU, descripcion, precio y stock.
          </p>

          <div className="mt-5 grid gap-4 md:grid-cols-3">
            <InfoChip label="Formatos" value=".csv / .xlsx" />
            <InfoChip label="Campo multipart" value="archivo" />
            <InfoChip label="Alcance v1" value="oferta proveedor" />
          </div>

          <div className="mt-6 rounded-3xl border border-dashed border-slate-300 bg-slate-50 p-5">
            <label className="block">
              <span className="text-xs font-semibold uppercase tracking-[0.22em] text-slate-500">Seleccion de archivo</span>
              <input
                type="file"
                accept=".csv,.xlsx"
                onChange={onFileChange}
                className={inputClassName}
                disabled={importacionMutation.isPending}
              />
            </label>

            <div className="mt-5 grid gap-3 md:grid-cols-2">
              <StatusCard
                label="Archivo actual"
                value={archivo?.name || "Ninguno seleccionado"}
                helper={
                  archivo
                    ? `${formatBytes(archivo.size)}${archivoValido ? "" : " · extension no valida"}`
                    : "Selecciona un CSV o XLSX para comenzar."
                }
              />
              <StatusCard
                label="Estado de carga"
                value={
                  importacionMutation.isPending
                    ? "Subiendo..."
                    : isPreparingPreview
                      ? "Preparando preview..."
                      : resultado
                        ? "Finalizada"
                        : "Pendiente"
                }
                helper={
                  importacionMutation.isPending
                    ? "Se bloqueo el submit para evitar dobles envios."
                    : isPreparingPreview
                      ? "Leyendo encabezados y filas del CSV."
                      : "Cuando termine podras subir otro archivo sin recargar la pagina."
                }
              />
            </div>

            <div className="mt-6 flex flex-wrap gap-3">
              <button
                type="button"
                className="rounded-full bg-slate-950 px-5 py-3 text-sm font-semibold text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:bg-slate-400"
                onClick={() => void onSubmit()}
                disabled={!canSubmit}
              >
                {importacionMutation.isPending ? "Subiendo archivo..." : "Importar archivo"}
              </button>
              <Link
                to="/proveedor/productos"
                className="rounded-full border border-slate-300 px-5 py-3 text-sm font-semibold text-slate-700 transition hover:border-slate-400 hover:text-slate-900"
              >
                Ir al listado de productos
              </Link>
            </div>
          </div>
        </section>

        <section className="space-y-4">
          <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-1">
            <ProveedorMetricCard
              label="Filas leidas"
              value={String(resultado?.filasLeidas ?? 0)}
              helper="Cantidad reportada por backend para el archivo procesado."
            />
            <ProveedorMetricCard
              label="Procesadas"
              value={String(totalProcesados)}
              helper="Suma visible de creados, actualizados y rechazados."
              tone="blue"
            />
          </div>
          <div className="grid gap-4 md:grid-cols-3 xl:grid-cols-1">
            <ProveedorMetricCard
              label="Headers detectados"
              value={String(csvPreview?.headers.length ?? 0)}
              helper="Solo para preview local de CSV."
              tone="blue"
            />
            <ProveedorMetricCard
              label="Campos pendientes"
              value={String(missingMappings.length)}
              helper="Columnas requeridas aun no asignadas."
              tone="amber"
            />
            <ProveedorMetricCard
              label="Duplicados"
              value={String(duplicateMappedHeaders.length)}
              helper="Columnas fuente repetidas en el mapeo."
              tone="amber"
            />
          </div>
        </section>
      </div>

      {isCsv ? (
        <section className="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm">
          <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
            <div>
              <p className="text-xs font-semibold uppercase tracking-[0.22em] text-slate-500">Preview CSV</p>
              <h3 className="mt-2 text-2xl font-semibold text-slate-950">Mapeo de importacion</h3>
              <p className="mt-2 text-sm leading-6 text-slate-600">
                Los bloques de abajo representan datos destino del API para cargar la oferta del proveedor. En cada bloque elegis que columna origen de tu archivo se usa para completar ese dato en la plataforma.
              </p>
            </div>
            {csvPreview ? <ResultPill>{csvPreview.sampleRows.length} fila{csvPreview.sampleRows.length === 1 ? "" : "s"} de muestra</ResultPill> : null}
          </div>

          {!archivo ? (
            <EmptyState>Selecciona un archivo CSV para ver encabezados detectados, mapeo sugerido y una muestra de filas.</EmptyState>
          ) : isPreparingPreview ? (
            <div className="mt-5 grid gap-4 md:grid-cols-2 xl:grid-cols-3">
              {Array.from({ length: 6 }).map((_, index) => (
                <div key={index} className="h-28 animate-pulse rounded-2xl border border-slate-200 bg-slate-200" />
              ))}
            </div>
          ) : !csvPreview ? (
            <EmptyState>No se pudo generar la preview del CSV. Revisa la estructura del archivo y vuelve a intentarlo.</EmptyState>
          ) : (
            <div className="mt-5 space-y-6">
              <div className="grid gap-4 xl:grid-cols-2">
                {destinationGroups.filter((group) => group.required).map((group) => (
                  <DestinationGroupCard
                    key={group.key}
                    group={group}
                    mapping={mapping}
                    headers={csvPreview.headers}
                    onChange={(field, value) =>
                      setMapping((current) => ({
                        ...current,
                        [field]: value,
                      }))
                    }
                  />
                ))}
              </div>

              <FeedbackAlert variant="info" title="Alcance real de esta importacion">
                Esta carga actualiza la oferta basica del proveedor. No crea ni asocia automaticamente `ProductoCanonico`, y no interpreta categorias externas del archivo como taxonomia interna de Mercado Digital.
              </FeedbackAlert>

              <div className="rounded-3xl border border-slate-200 bg-slate-50 p-4">
                <p className="text-sm font-semibold text-slate-900">Metadata externa del archivo</p>
                <p className="mt-1 text-sm text-slate-600">
                  Estos datos no forman parte del nucleo minimo que hoy impacta la oferta del proveedor. Se muestran como referencia externa y no deben leerse como vinculacion automatica al dominio interno MD.
                </p>
                <div className="mt-4 grid gap-4 md:grid-cols-2 xl:grid-cols-3">
                  {destinationGroups.filter((group) => !group.required).map((group) => (
                    <DestinationGroupCard
                      key={group.key}
                    group={group}
                    mapping={mapping}
                    headers={csvPreview.headers}
                    onChange={(field, value) =>
                        setMapping((current) => ({
                          ...current,
                          [field]: value,
                        }))
                      }
                    />
                  ))}
                </div>
              </div>

              {missingMappings.length > 0 ? (
                <FeedbackAlert variant="warning" title="Faltan asignaciones">
                  Completa todas las asignaciones requeridas para `sku`, `nombre/descripcion`, `precioCosto` y `stock`.
                </FeedbackAlert>
              ) : null}

              {duplicateMappedHeaders.length > 0 ? (
                <FeedbackAlert variant="warning" title="Hay columnas repetidas en el mapeo">
                  Ajusta las selecciones para que una columna fuente no se use mas de una vez: {duplicateMappedHeaders.join(", ")}.
                </FeedbackAlert>
              ) : null}

              <div className="rounded-3xl border border-slate-200 bg-slate-50 p-4">
                <p className="text-sm font-semibold text-slate-900">Encabezados detectados</p>
                  <p className="mt-1 text-sm text-slate-600">Estas son las columnas origen detectadas en el archivo que podes asignar a cada campo destino.</p>
                <div className="mt-3 flex flex-wrap gap-2">
                  {csvPreview.headers.map((header) => (
                    <ResultPill key={header.value}>{header.label}</ResultPill>
                  ))}
                </div>
              </div>

              <div className="overflow-x-auto">
                <table className="min-w-full divide-y divide-slate-200 overflow-hidden rounded-2xl border border-slate-200 bg-white text-sm">
                  <thead className="bg-slate-100 text-left text-xs font-semibold uppercase tracking-[0.18em] text-slate-500">
                    <tr>
                      <th className="px-4 py-3">SKU</th>
                      <th className="px-4 py-3">Descripcion</th>
                      <th className="px-4 py-3">Precio costo</th>
                      <th className="px-4 py-3">Stock total</th>
                      <th className="px-4 py-3">Tipo</th>
                      <th className="px-4 py-3">IVA</th>
                      <th className="px-4 py-3">Grupo</th>
                      <th className="px-4 py-3">Rubro</th>
                      <th className="px-4 py-3">Part Number</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-200">
                    {csvPreview.sampleRows.length > 0 ? (
                      csvPreview.sampleRows.map((row, rowIndex) => (
                        <tr key={rowIndex} className="align-top">
                          <td className="px-4 py-3 text-slate-700">{getMappedCellValue(csvPreview.headers, row, mapping.Codigo) || "-"}</td>
                          <td className="px-4 py-3 text-slate-700">{getMappedCellValue(csvPreview.headers, row, mapping.Descripcion) || "-"}</td>
                          <td className="px-4 py-3 text-slate-700">{getMappedCellValue(csvPreview.headers, row, mapping.precio) || "-"}</td>
                          <td className="px-4 py-3 text-slate-700">{formatStockPreview(row, csvPreview.headers, mapping)}</td>
                          <td className="px-4 py-3 text-slate-700">{getMappedCellValue(csvPreview.headers, row, mapping.Tipo) || "-"}</td>
                          <td className="px-4 py-3 text-slate-700">{getMappedCellValue(csvPreview.headers, row, mapping.IVA) || "-"}</td>
                          <td className="px-4 py-3 text-slate-700">{getMappedCellValue(csvPreview.headers, row, mapping.Grupo) || "-"}</td>
                          <td className="px-4 py-3 text-slate-700">{getMappedCellValue(csvPreview.headers, row, mapping.Rubro) || "-"}</td>
                          <td className="px-4 py-3 text-slate-700">{getMappedCellValue(csvPreview.headers, row, mapping["Part Number"]) || "-"}</td>
                        </tr>
                      ))
                    ) : (
                      <tr>
                        <td colSpan={9} className="px-4 py-6 text-center text-slate-600">
                          El CSV no tiene filas de datos para previsualizar.
                        </td>
                      </tr>
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </section>
      ) : archivo && archivoValido ? (
        <FeedbackAlert variant="info" title="Preview local no disponible para XLSX">
          Esta v1 mantiene la subida directa para `.xlsx`. Si queres inspeccionarlo y mapear columnas antes de importar, conviertelo a `.csv`.
        </FeedbackAlert>
      ) : null}

      <section className="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-end sm:justify-between">
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.22em] text-slate-500">Resultado</p>
            <h3 className="mt-2 text-2xl font-semibold text-slate-950">Resumen de importacion</h3>
          </div>
          {resultado ? <ResultPill>{resultado.errores.length > 0 ? "Con observaciones" : "Sin errores reportados"}</ResultPill> : null}
        </div>

        {!resultado ? (
          <EmptyState>
            Todavia no hay un resultado de importacion para mostrar. Revisa el archivo, completa el mapeo si es CSV y ejecuta la carga para ver el resumen real devuelto por backend.
          </EmptyState>
        ) : (
          <div className="mt-5 space-y-6">
            <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
              <SummaryCard label="Filas leidas" value={resultado.filasLeidas} />
              <SummaryCard label="Creados" value={resultado.creados} />
              <SummaryCard label="Actualizados" value={resultado.actualizados} />
              <SummaryCard label="Rechazados" value={resultado.rechazados} />
            </div>

            <div className="flex flex-wrap gap-3">
              <Link
                to="/proveedor/productos"
                className="rounded-full bg-slate-950 px-5 py-3 text-sm font-semibold text-white transition hover:bg-slate-800"
              >
                Revisar productos importados
              </Link>
              <button
                type="button"
                className="rounded-full border border-slate-300 px-5 py-3 text-sm font-semibold text-slate-700 transition hover:border-slate-400 hover:text-slate-950"
                onClick={onReset}
              >
                Preparar otra carga
              </button>
            </div>

            <div className="rounded-3xl border border-slate-200 bg-slate-50 p-4">
              <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                <div>
                  <p className="text-sm font-semibold text-slate-900">Errores por fila</p>
                  <p className="mt-1 text-sm text-slate-600">
                    {resultado.errores.length > 0
                      ? "Detalle devuelto por backend para revisar filas rechazadas."
                      : "El backend no reporto errores por fila para esta importacion."}
                  </p>
                </div>
                <ResultPill>{resultado.errores.length} error{resultado.errores.length === 1 ? "" : "es"}</ResultPill>
              </div>

              {resultado.errores.length > 0 ? (
                <div className="mt-4 overflow-x-auto">
                  <table className="min-w-full divide-y divide-slate-200 overflow-hidden rounded-2xl border border-slate-200 bg-white text-sm">
                    <thead className="bg-slate-100 text-left text-xs font-semibold uppercase tracking-[0.18em] text-slate-500">
                      <tr>
                        <th className="px-4 py-3">Fila</th>
                        <th className="px-4 py-3">SKU</th>
                        <th className="px-4 py-3">Mensaje</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-200">
                      {resultado.errores.map((error, index) => (
                        <tr key={`${error.fila ?? "sin-fila"}-${error.sku ?? "sin-sku"}-${index}`} className="align-top">
                          <td className="px-4 py-3 font-semibold text-slate-900">{error.fila ?? "-"}</td>
                          <td className="px-4 py-3 text-slate-700">{error.sku || "-"}</td>
                          <td className="px-4 py-3 text-slate-600">{error.mensaje}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : null}
            </div>
          </div>
        )}
      </section>
    </section>
  )
}

function SummaryCard({ label, value }: { label: string; value: number }) {
  return (
    <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
      <p className="text-xs font-semibold uppercase tracking-[0.18em] text-slate-500">{label}</p>
      <p className="mt-2 text-3xl font-semibold text-slate-950">{value}</p>
    </div>
  )
}

function StatusCard({ label, value, helper }: { label: string; value: string; helper: string }) {
  return (
    <div className="rounded-2xl border border-slate-200 bg-white p-4">
      <p className="text-xs font-semibold uppercase tracking-[0.18em] text-slate-500">{label}</p>
      <p className="mt-2 break-all text-base font-semibold text-slate-950">{value}</p>
      <p className="mt-2 text-sm leading-6 text-slate-600">{helper}</p>
    </div>
  )
}

function InfoChip({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-2xl border border-slate-200 bg-white p-4">
      <p className="text-xs font-semibold uppercase tracking-[0.18em] text-slate-500">{label}</p>
      <p className="mt-2 text-sm font-semibold text-slate-900">{value}</p>
    </div>
  )
}

function ResultPill({ children }: { children: ReactNode }) {
  return <span className="rounded-full bg-slate-100 px-3 py-1 text-xs font-semibold text-slate-700">{children}</span>
}

function EmptyState({ children }: { children: ReactNode }) {
  return <div className="mt-5 rounded-2xl border border-dashed border-slate-300 bg-slate-50 p-6 text-sm leading-6 text-slate-600">{children}</div>
}

function DestinationGroupCard({
  group,
  mapping,
  headers,
  onChange,
}: {
  group: (typeof destinationGroups)[number]
  mapping: Record<RequiredImportField, string>
  headers: CsvHeaderOption[]
  onChange: (field: RequiredImportField, value: string) => void
}) {
  return (
    <section className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
      <p className="text-xs font-semibold uppercase tracking-[0.18em] text-slate-500">Campo destino API</p>
      <div className="mt-2 flex flex-wrap items-center gap-2">
        <p className="text-base font-semibold text-slate-950">{group.title}</p>
        <span className="rounded-full bg-white px-3 py-1 text-xs font-semibold text-slate-700">{group.apiField}</span>
      </div>
      <p className="mt-2 text-sm leading-6 text-slate-600">{group.helper}</p>

      <div className={`mt-4 grid gap-4 ${group.fields.length > 1 ? "md:grid-cols-2" : ""}`}>
        {group.fields.map((field) => (
          <label key={field} className="block">
            <span className="text-xs font-semibold uppercase tracking-[0.18em] text-slate-500">
              Columna origen para {fieldMeta[field].title}
            </span>
            <select
              value={mapping[field]}
              onChange={(event) => onChange(field, event.target.value)}
              className="mt-2 w-full rounded-2xl border border-slate-300 bg-white px-4 py-3 text-sm text-slate-900 outline-none transition focus:border-slate-400 focus:ring-2 focus:ring-slate-200"
            >
              <option value="">Seleccionar columna origen</option>
              {headers.map((header) => (
                <option key={`${field}-${header.value}`} value={header.value}>
                  {header.label}
                </option>
              ))}
            </select>
          </label>
        ))}
      </div>
    </section>
  )
}

function createEmptyMapping(): Record<RequiredImportField, string> {
  return requiredImportFields.reduce(
    (acc, field) => {
      acc[field] = ""
      return acc
    },
    {} as Record<RequiredImportField, string>,
  )
}

function buildCsvPreview(text: string): CsvPreview {
  const rows = parseCsv(text)
  if (rows.length === 0) throw new Error("El archivo CSV esta vacio.")

  const headers = rows[0].map((header, index) => buildHeaderOption(header.trim(), index))
  if (headers.length === 0) throw new Error("No se detectaron encabezados en la primera fila.")

  const allRows = rows.slice(1).filter((row) => row.some((cell) => cell.trim().length > 0))
  const sampleRows = allRows.slice(0, 10)
  const suggestedMapping = createEmptyMapping()

  for (const field of requiredImportFields) {
    const candidate = headers.find((header) => normalizeHeader(header.raw) === normalizeHeader(field))
    suggestedMapping[field] = candidate?.value ?? ""
  }

  return {
    headers,
    suggestedMapping,
    sampleRows,
    allRows,
  }
}

function parseCsv(text: string) {
  const normalizedText = text.replace(/^\uFEFF/, "")
  const rows: string[][] = []
  let currentCell = ""
  let currentRow: string[] = []
  let inQuotes = false

  for (let index = 0; index < normalizedText.length; index += 1) {
    const char = normalizedText[index]
    const nextChar = normalizedText[index + 1]

    if (char === '"') {
      if (inQuotes && nextChar === '"') {
        currentCell += '"'
        index += 1
      } else {
        inQuotes = !inQuotes
      }
      continue
    }

    if (char === "," && !inQuotes) {
      currentRow.push(currentCell)
      currentCell = ""
      continue
    }

    if ((char === "\n" || char === "\r") && !inQuotes) {
      if (char === "\r" && nextChar === "\n") {
        index += 1
      }
      currentRow.push(currentCell)
      rows.push(currentRow)
      currentRow = []
      currentCell = ""
      continue
    }

    currentCell += char
  }

  if (currentCell.length > 0 || currentRow.length > 0) {
    currentRow.push(currentCell)
    rows.push(currentRow)
  }

  return rows
}

function buildHeaderOption(rawHeader: string, index: number): CsvHeaderOption {
  const cleaned = rawHeader.trim()
  const label = cleaned ? cleaned : `Columna sin encabezado #${index + 1}`
  return {
    value: `col_${index}`,
    label,
    raw: cleaned,
    index,
  }
}

function buildNormalizedCsvFile(
  archivoOriginal: File,
  csvText: string,
  preview: CsvPreview,
  mapping: Record<RequiredImportField, string>,
) {
  const headerIndexes = preview.headers.reduce(
    (acc, header) => {
      acc[header.value] = header.index
      return acc
    },
    {} as Record<string, number>,
  )

  const normalizedLines = [
    requiredImportFields.join(","),
    ...preview.allRows.map((row) =>
      requiredImportFields
        .map((field) => {
          const sourceHeader = mapping[field]
          const sourceIndex = headerIndexes[sourceHeader]
          const value = sourceIndex !== undefined ? row[sourceIndex] ?? "" : ""
          return escapeCsvCell(value)
        })
        .join(","),
    ),
  ]

  const lineBreak = csvText.includes("\r\n") ? "\r\n" : "\n"

  return new File([normalizedLines.join(lineBreak)], replaceFileExtension(archivoOriginal.name, ".csv"), {
    type: "text/csv;charset=utf-8;",
  })
}

function escapeCsvCell(value: string) {
  if (value.includes(",") || value.includes('"') || value.includes("\n") || value.includes("\r")) {
    return `"${value.replace(/"/g, '""')}"`
  }
  return value
}

function getMappedCellValue(headers: CsvHeaderOption[], row: string[], mappedHeader: string) {
  const header = headers.find((item) => item.value === mappedHeader)
  if (!header) return ""
  return row[header.index] ?? ""
}

function formatStockPreview(
  row: string[],
  headers: CsvHeaderOption[],
  mapping: Record<RequiredImportField, string>,
) {
  const total = ["ROS", "MZA", "CBA", "LUG"].reduce((sum, field) => {
    const raw = getMappedCellValue(headers, row, mapping[field as RequiredImportField]).trim()
    const normalized = raw.replace(",", ".")
    const value = Number(normalized)
    return Number.isFinite(value) ? sum + value : sum
  }, 0)

  return String(total)
}

function normalizeHeader(value: string) {
  return value.normalize("NFD").replace(/[\u0300-\u036f]/g, "").toLowerCase().trim()
}

function replaceFileExtension(fileName: string, nextExtension: string) {
  const dotIndex = fileName.lastIndexOf(".")
  if (dotIndex < 0) return `${fileName}${nextExtension}`
  return `${fileName.slice(0, dotIndex)}${nextExtension}`
}

function getFileExtension(fileName: string) {
  const normalized = fileName.trim().toLowerCase()
  return normalized.includes(".") ? `.${normalized.split(".").pop()}` : ""
}

function formatBytes(size: number) {
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / (1024 * 1024)).toFixed(1)} MB`
}

function getCsvPreviewErrorMessage(error: unknown) {
  if (error instanceof Error && error.message.trim()) {
    return error.message
  }
  return "El archivo no se pudo interpretar como CSV."
}

function getErrorMessage(error: unknown) {
  if (axios.isAxiosError(error)) {
    const data = error.response?.data

    if (typeof data === "string" && data.trim()) {
      return data
    }

    if (data && typeof data === "object") {
      const message =
        ("message" in data && typeof data.message === "string" && data.message) ||
        ("mensaje" in data && typeof data.mensaje === "string" && data.mensaje) ||
        ("error" in data && typeof data.error === "string" && data.error)

      if (message) return message
    }

    if (error.response?.status) {
      return `Backend respondio ${error.response.status}. Revisa el archivo e intenta nuevamente.`
    }

    return "Ocurrio un error de red al intentar subir el archivo."
  }

  if (error instanceof Error && error.message.trim()) {
    return error.message
  }

  return "No fue posible completar la importacion."
}

function downloadTextFile(fileName: string, content: string, type: string) {
  const blob = new Blob([content], { type })
  const url = URL.createObjectURL(blob)
  const anchor = document.createElement("a")
  anchor.href = url
  anchor.download = fileName
  anchor.click()
  URL.revokeObjectURL(url)
}
