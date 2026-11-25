import { useEffect, useMemo, useState } from "react"
import FeedbackAlert from "../components/ui/FeedbackAlert"
import { useAuth } from "../auth/useAuth"
import { LOCAL_STOREFRONT_OVERRIDE_KEY } from "../tenant/TenantContext"
import { useRevendedorPricingPreview } from "../hooks/useRevendedorPricing"
import { useCreateRevendedorAsesor, useRevendedorAsesores, useRevendedorStorefrontHost, useUpdateRevendedorAsesor } from "../hooks/useRevendedorAsesores"
import type { RevendedorPricingPreviewItemResponse } from "../types/pricing"
import type { RevendedorAsesorResponse } from "../types/revendedor"

const currencyFormatter = new Intl.NumberFormat("es-AR", {
  style: "currency",
  currency: "ARS",
  maximumFractionDigits: 2,
})

type SectionKey = "presupuestos" | "redes"

type ShareFeedback =
  | { variant: "success" | "error" | "warning" | "info"; message: string }
  | null

function normalizeText(value: string | null | undefined) {
  return (value ?? "")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase()
    .trim()
}

function normalizeHost(value: string) {
  return value
    .trim()
    .toLowerCase()
    .replace(/^https?:\/\//, "")
    .replace(/\/.*$/, "")
    .replace(/:\d+$/, "")
}

function getStorefrontHostStorageKey(revendedorId: string | null) {
  return revendedorId ? `md:revendedor-storefront-host:${revendedorId}` : "md:revendedor-storefront-host"
}

function resolveDefaultStorefrontHost(revendedorId: string | null) {
  if (typeof window === "undefined") {
    return ""
  }

  const storageKey = getStorefrontHostStorageKey(revendedorId)
  const stored = normalizeHost(window.localStorage.getItem(storageKey) ?? "")
  if (stored) {
    return stored
  }

  const sessionOverride = normalizeHost(window.sessionStorage.getItem(LOCAL_STOREFRONT_OVERRIDE_KEY) ?? "")
  if (sessionOverride) {
    return sessionOverride
  }

  return normalizeHost(import.meta.env.VITE_STOREFRONT_HOST ?? "")
}

function buildPublicLink(path: string, storefrontHostRaw: string, selectedAsesor: RevendedorAsesorResponse | null, extraParams: Record<string, string> = {}) {
  const storefrontHost = normalizeHost(storefrontHostRaw)
  const hasRemoteStorefrontHost = storefrontHost && storefrontHost !== "localhost" && storefrontHost !== "127.0.0.1"
  const params: Record<string, string> = { ...extraParams }

  if (selectedAsesor) {
    params.asesorId = selectedAsesor.id
    params.asesor = selectedAsesor.nombre
  }

  if (hasRemoteStorefrontHost) {
    const url = new URL(`https://${storefrontHost}${path}`)
    Object.entries(params).forEach(([key, value]) => {
      if (value.trim()) {
        url.searchParams.set(key, value.trim())
      }
    })
    return url.toString()
  }

  const url = new URL(path, window.location.origin)
  if (storefrontHost) {
    url.searchParams.set("storefrontHost", storefrontHost)
  }
  Object.entries(params).forEach(([key, value]) => {
    if (value.trim()) {
      url.searchParams.set(key, value.trim())
    }
  })
  return url.toString()
}

function buildStorefrontOgShareLink(path: string, storefrontHostRaw: string, params: Record<string, string> = {}) {
  const storefrontHost = normalizeHost(storefrontHostRaw)
  const hasRemoteStorefrontHost = storefrontHost && storefrontHost !== "localhost" && storefrontHost !== "127.0.0.1"
  const baseUrl = hasRemoteStorefrontHost ? `https://${storefrontHost}/api/` : window.location.origin
  const url = new URL(path, baseUrl)
  Object.entries(params).forEach(([key, value]) => {
    const trimmed = value.trim()
    if (trimmed) {
      url.searchParams.set(key, trimmed)
    }
  })
  return url.toString()
}

async function shareViaWebOrWhatsApp(title: string, text: string, url: string) {
  if (typeof navigator !== "undefined" && typeof navigator.share === "function") {
    await navigator.share({ title, text, url })
    return
  }

  const waUrl = `https://wa.me/?text=${encodeURIComponent(`${text}\n${url}`)}`
  window.open(waUrl, "_blank", "noopener,noreferrer")
}

function buildShareText(producto: RevendedorPricingPreviewItemResponse) {
  const entregaDesde = Math.max(producto.demoraEstimadaDias - 2, 1)
  const entregaHasta = producto.demoraEstimadaDias + 2
  return [
    `Producto: ${producto.nombrePublico}`,
    `Precio: ${currencyFormatter.format(producto.precioFinalEstimado)}`,
    `Entrega estimada: ${entregaDesde} a ${entregaHasta} dias`,
    "Compra online segura.",
    "Ver detalles:",
  ].join("\n")
}

function buildDailySuggestions(items: RevendedorPricingPreviewItemResponse[], size: number) {
  const daySeed = new Date().toISOString().slice(0, 10)
  return [...items]
    .sort((a, b) => `${daySeed}:${a.productoCanonicoId}`.localeCompare(`${daySeed}:${b.productoCanonicoId}`))
    .slice(0, size)
}

export default function RevendedorRedComercialPage() {
  const { session } = useAuth()
  const revendedorId = session?.revendedorId || localStorage.getItem("revendedorId") || import.meta.env.VITE_REVENDEDOR_ID || null
  const previewQuery = useRevendedorPricingPreview(revendedorId)
  const asesoresQuery = useRevendedorAsesores(revendedorId)
  const storefrontHostQuery = useRevendedorStorefrontHost(revendedorId)
  const createAsesor = useCreateRevendedorAsesor(revendedorId)
  const updateAsesor = useUpdateRevendedorAsesor(revendedorId)

  const [storefrontHost, setStorefrontHost] = useState(() => resolveDefaultStorefrontHost(revendedorId))
  const [section, setSection] = useState<SectionKey>("presupuestos")
  const [search, setSearch] = useState("")
  const [selectedIds, setSelectedIds] = useState<string[]>([])
  const [selectedAsesorId, setSelectedAsesorId] = useState<string>("")
  const [nuevoAsesorNombre, setNuevoAsesorNombre] = useState("")
  const [feedback, setFeedback] = useState<ShareFeedback>(null)

  useEffect(() => {
    setStorefrontHost(resolveDefaultStorefrontHost(revendedorId))
  }, [revendedorId])

  useEffect(() => {
    const hostFromBackend = normalizeHost(storefrontHostQuery.data?.storefrontHost ?? "")
    if (!hostFromBackend) {
      return
    }
    setStorefrontHost(hostFromBackend)
    if (typeof window !== "undefined") {
      window.localStorage.setItem(getStorefrontHostStorageKey(revendedorId), hostFromBackend)
    }
  }, [revendedorId, storefrontHostQuery.data?.storefrontHost])

  const productosOperables = useMemo(() => {
    const base = previewQuery.data ?? []
    return base.filter((item) => item.stockDisponible > 0)
  }, [previewQuery.data])

  const asesores = asesoresQuery.data ?? []
  const selectedAsesor = useMemo(
    () => asesores.find((item) => item.id === selectedAsesorId && item.activo) ?? null,
    [asesores, selectedAsesorId],
  )

  const productosSugeridosHoy = useMemo(() => buildDailySuggestions(productosOperables, 6), [productosOperables])

  const filteredProducts = useMemo(() => {
    const query = normalizeText(search)
    return productosOperables.filter((item) => normalizeText(item.nombrePublico).includes(query))
  }, [productosOperables, search])

  const selectedProducts = useMemo(
    () => productosOperables.filter((item) => selectedIds.includes(item.productoCanonicoId)),
    [productosOperables, selectedIds],
  )

  const listaCompartibleLink = useMemo(() => {
    if (selectedIds.length === 0) {
      return null
    }
    const params: Record<string, string> = { lista: selectedIds.join(",") }
    return buildPublicLink("/catalogo", storefrontHost, selectedAsesor, params)
  }, [selectedAsesor, selectedIds, storefrontHost])

  const listaResumen = useMemo(() => {
    const total = selectedProducts.reduce((sum, item) => sum + item.precioFinalEstimado, 0)
    return {
      cantidad: selectedProducts.length,
      totalEstimado: total,
    }
  }, [selectedProducts])

  const toggleSelect = (productoCanonicoId: string) => {
    setSelectedIds((current) =>
      current.includes(productoCanonicoId)
        ? current.filter((id) => id !== productoCanonicoId)
        : [...current, productoCanonicoId],
    )
  }

  const shareProduct = async (producto: RevendedorPricingPreviewItemResponse) => {
    const shareLink = buildStorefrontOgShareLink(`public/share/producto/${producto.productoCanonicoId}`, storefrontHost, {
      storefrontHost: normalizeHost(storefrontHost),
      asesorId: selectedAsesor?.id ?? "",
      asesor: selectedAsesor?.nombre ?? "",
      v: `${Date.now()}`,
    })
    const text = buildShareText(producto)
    setFeedback(null)

    try {
      await shareViaWebOrWhatsApp(producto.nombrePublico, text, shareLink)
      setFeedback({ variant: "success", message: `Producto compartido: ${producto.nombrePublico}` })
    } catch {
      setFeedback({ variant: "error", message: "No se pudo abrir el flujo de compartir en este dispositivo." })
    }
  }

  const shareList = async () => {
    if (!listaCompartibleLink || selectedProducts.length === 0) {
      setFeedback({ variant: "warning", message: "Selecciona al menos un producto para generar el presupuesto compartible." })
      return
    }

    const shareListLink = buildPublicLink("/catalogo", storefrontHost, selectedAsesor, {
      lista: selectedIds.join(","),
    })
    const lines = [
      "Te comparto una seleccion de productos:",
      ...selectedProducts.slice(0, 8).map((item) => `- ${item.nombrePublico} (${currencyFormatter.format(item.precioFinalEstimado)})`),
      "",
      "Abrir lista y comprar online:",
    ]

    setFeedback(null)

    try {
      await shareViaWebOrWhatsApp("Presupuesto sugerido", lines.join("\n"), shareListLink)
      setFeedback({ variant: "success", message: "Lista compartida correctamente." })
    } catch {
      setFeedback({ variant: "error", message: "No se pudo compartir la lista en este dispositivo." })
    }
  }

  const createAsesorHandler = () => {
    const nombre = nuevoAsesorNombre.trim()
    if (!nombre) {
      setFeedback({ variant: "warning", message: "Ingresa un nombre para crear el asesor." })
      return
    }

    createAsesor.mutate(
      { nombre, activo: true },
      {
        onSuccess: (asesorCreado) => {
          setSelectedAsesorId(asesorCreado.id)
          setNuevoAsesorNombre("")
          setFeedback({ variant: "success", message: `Asesor creado: ${asesorCreado.nombre}` })
        },
        onError: () => {
          setFeedback({ variant: "error", message: "No se pudo crear el asesor de venta." })
        },
      },
    )
  }

  const toggleAsesorActivo = (asesor: RevendedorAsesorResponse) => {
    updateAsesor.mutate(
      {
        asesorId: asesor.id,
        payload: {
          nombre: asesor.nombre,
          fotoUrl: asesor.fotoUrl,
          activo: !asesor.activo,
        },
      },
      {
        onSuccess: () => {
          if (asesor.activo && selectedAsesorId === asesor.id) {
            setSelectedAsesorId("")
          }
          setFeedback({
            variant: "success",
            message: !asesor.activo ? `Asesor activado: ${asesor.nombre}` : `Asesor desactivado: ${asesor.nombre}`,
          })
        },
        onError: () => {
          setFeedback({ variant: "error", message: "No se pudo actualizar el estado del asesor." })
        },
      },
    )
  }

  if (!revendedorId) {
    return (
      <section className="space-y-4">
        <header className="rounded-[2rem] border border-slate-200 bg-white p-8 shadow-sm">
          <h2 className="text-3xl font-semibold tracking-tight text-slate-950">Red comercial</h2>
          <p className="mt-3 text-sm text-slate-600">Esta pantalla necesita un `revendedorId` operativo en sesion.</p>
        </header>
        <FeedbackAlert variant="warning" title="Revendedor no identificado">
          Primero necesitamos conocer que revendedor esta operando para poder gestionar asesores y compartir productos.
        </FeedbackAlert>
      </section>
    )
  }

  return (
    <section className="space-y-6">
      <header className="rounded-[2rem] border border-slate-200 bg-white p-8 shadow-sm">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div className="max-w-3xl">
            <p className="text-xs font-semibold uppercase tracking-[0.35em] text-slate-400">Revendedor</p>
            <h2 className="mt-3 text-3xl font-semibold tracking-tight text-slate-950">Red comercial</h2>
            <p className="mt-3 text-sm leading-6 text-slate-600">
              Gestiona asesores ligados a tu storefront y opera dos flujos comerciales: presupuestos compartibles y
              difusion en redes sociales (WhatsApp sin API paga).
            </p>
          </div>
          <span className="rounded-full border border-emerald-200 bg-emerald-50 px-3 py-1 text-xs font-semibold uppercase tracking-[0.2em] text-emerald-700">
            conectable
          </span>
        </div>
      </header>

      {feedback ? <FeedbackAlert variant={feedback.variant}>{feedback.message}</FeedbackAlert> : null}

      <section className="rounded-[2rem] border border-slate-200 bg-white p-6 shadow-sm">
        <div className="flex flex-wrap items-end justify-between gap-4">
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.3em] text-slate-400">Asesores</p>
            <h3 className="mt-2 text-xl font-semibold tracking-tight text-slate-950">Equipo de ventas del storefront</h3>
          </div>
          <div className="flex w-full flex-col gap-2 sm:w-auto sm:flex-row sm:items-end">
            <input
              value={nuevoAsesorNombre}
              onChange={(event) => setNuevoAsesorNombre(event.target.value)}
              placeholder="Nombre del asesor"
              className="w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm outline-none transition focus:border-blue-300 focus:bg-white sm:w-64"
            />
            <button
              type="button"
              className="rounded-full bg-slate-950 px-4 py-2 text-sm font-semibold text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-60"
              disabled={createAsesor.isPending}
              onClick={createAsesorHandler}
            >
              {createAsesor.isPending ? "Creando..." : "Crear asesor"}
            </button>
          </div>
        </div>

        <div className="mt-4 grid gap-3 md:grid-cols-3">
          <SummaryCard label="Asesores totales" value={String(asesores.length)} detail="Asignados al revendedor actual." />
          <SummaryCard
            label="Activos"
            value={String(asesores.filter((item) => item.activo).length)}
            detail="Disponibles para atribuir ventas."
          />
          <SummaryCard
            label="Asesor seleccionado"
            value={selectedAsesor?.nombre || "Sin seleccionar"}
            detail="Se usa en links de presupuesto y redes."
          />
        </div>

        <div className="mt-4 grid gap-3 md:grid-cols-2">
          <label className="space-y-2">
            <span className="text-sm font-medium text-slate-700">Elegir asesor activo</span>
            <select
              value={selectedAsesorId}
              onChange={(event) => setSelectedAsesorId(event.target.value)}
              className="w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm outline-none transition focus:border-blue-300 focus:bg-white"
            >
              <option value="">Sin asesor</option>
              {asesores
                .filter((item) => item.activo)
                .map((item) => (
                  <option key={item.id} value={item.id}>
                    {item.nombre}
                  </option>
                ))}
            </select>
          </label>

          <label className="space-y-2">
            <span className="text-sm font-medium text-slate-700">Dominio storefront</span>
            <input
              value={storefrontHost}
              onChange={(event) => {
                setStorefrontHost(event.target.value)
                window.localStorage.setItem(getStorefrontHostStorageKey(revendedorId), normalizeHost(event.target.value))
              }}
              placeholder="ej: navarroventas.com"
              className="w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm outline-none transition focus:border-blue-300 focus:bg-white"
            />
          </label>
        </div>

        {asesoresQuery.isLoading ? (
          <div className="mt-4 space-y-2">
            {Array.from({ length: 3 }).map((_, index) => (
              <div key={index} className="h-12 animate-pulse rounded-xl bg-slate-100" />
            ))}
          </div>
        ) : null}

        {!asesoresQuery.isLoading && asesores.length > 0 ? (
          <div className="mt-4 space-y-2">
            {asesores.map((asesor) => (
              <div key={asesor.id} className="flex flex-wrap items-center justify-between gap-2 rounded-xl border border-slate-200 bg-slate-50 px-3 py-2">
                <div>
                  <p className="text-sm font-semibold text-slate-900">{asesor.nombre}</p>
                  <p className="text-xs text-slate-500">{asesor.activo ? "Activo" : "Inactivo"}</p>
                </div>
                <button
                  type="button"
                  className={`rounded-full px-3 py-1 text-xs font-semibold ${
                    asesor.activo ? "bg-amber-100 text-amber-700 hover:bg-amber-200" : "bg-emerald-100 text-emerald-700 hover:bg-emerald-200"
                  }`}
                  onClick={() => toggleAsesorActivo(asesor)}
                >
                  {asesor.activo ? "Desactivar" : "Activar"}
                </button>
              </div>
            ))}
          </div>
        ) : null}
      </section>

      <section className="rounded-[2rem] border border-slate-200 bg-white p-6 shadow-sm">
        <div className="flex flex-wrap gap-2">
          <button
            type="button"
            onClick={() => setSection("presupuestos")}
            className={`rounded-full px-4 py-2 text-sm font-semibold ${
              section === "presupuestos" ? "bg-slate-900 text-white" : "border border-slate-300 bg-white text-slate-700"
            }`}
          >
            Presupuestos
          </button>
          <button
            type="button"
            onClick={() => setSection("redes")}
            className={`rounded-full px-4 py-2 text-sm font-semibold ${
              section === "redes" ? "bg-slate-900 text-white" : "border border-slate-300 bg-white text-slate-700"
            }`}
          >
            Redes sociales
          </button>
        </div>

        <div className="mt-4 flex flex-col gap-3 md:flex-row md:items-end md:justify-between">
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.3em] text-slate-400">Productos</p>
            <h3 className="mt-2 text-xl font-semibold tracking-tight text-slate-950">
              {section === "presupuestos" ? "Armar presupuesto comercial" : "Compartir en redes"}
            </h3>
          </div>
          <input
            type="search"
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            placeholder="Buscar producto"
            className="w-full rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm outline-none transition focus:border-blue-300 focus:bg-white md:max-w-xs"
          />
        </div>

        {previewQuery.isLoading ? (
          <div className="mt-5 space-y-3">
            {Array.from({ length: 4 }).map((_, index) => (
              <div key={index} className="h-20 animate-pulse rounded-2xl bg-slate-100" />
            ))}
          </div>
        ) : null}

        {!previewQuery.isLoading ? (
          <div className="mt-5 grid gap-4 md:grid-cols-2">
            {(section === "redes" ? filteredProducts : buildDailySuggestions(filteredProducts, 12)).map((producto) => {
              const isSelected = selectedIds.includes(producto.productoCanonicoId)
              const detalleLink = buildPublicLink(`/producto/${producto.productoCanonicoId}`, storefrontHost, selectedAsesor)
              return (
                <article key={producto.productoCanonicoId} className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
                  <div className="flex items-start gap-3">
                    <div className="h-20 w-20 overflow-hidden rounded-xl border border-slate-200 bg-white">
                      {producto.imagenPrincipal ? (
                        <img src={producto.imagenPrincipal} alt={producto.nombrePublico} className="h-full w-full object-contain" />
                      ) : (
                        <div className="flex h-full w-full items-center justify-center text-xs text-slate-400">Sin imagen</div>
                      )}
                    </div>
                    <div className="min-w-0 flex-1">
                      <p className="truncate text-sm font-semibold text-slate-900">{producto.nombrePublico}</p>
                      <p className="mt-1 text-xs text-slate-500">Stock: {producto.stockDisponible}</p>
                      <p className="mt-2 text-base font-semibold text-slate-950">
                        {currencyFormatter.format(producto.precioFinalEstimado)}
                      </p>
                    </div>
                  </div>

                  <div className="mt-4 flex flex-wrap gap-2">
                    {section === "redes" ? (
                      <>
                        <a
                          href={detalleLink}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="rounded-full border border-slate-300 bg-white px-3 py-1.5 text-xs font-semibold text-slate-700 transition hover:border-slate-400 hover:text-slate-900"
                        >
                          Ver detalle
                        </a>
                        <button
                          type="button"
                          className="rounded-full border border-emerald-300 bg-emerald-50 px-3 py-1.5 text-xs font-semibold text-emerald-700 transition hover:border-emerald-400 hover:bg-emerald-100"
                          onClick={() => {
                            void shareProduct(producto)
                          }}
                        >
                          Compartir producto
                        </button>
                      </>
                    ) : (
                      <button
                        type="button"
                        className={`rounded-full border px-3 py-1.5 text-xs font-semibold transition ${
                          isSelected
                            ? "border-slate-900 bg-slate-900 text-white hover:bg-slate-800"
                            : "border-slate-300 bg-white text-slate-700 hover:border-slate-400"
                        }`}
                        onClick={() => toggleSelect(producto.productoCanonicoId)}
                      >
                        {isSelected ? "Quitar de presupuesto" : "Agregar a presupuesto"}
                      </button>
                    )}
                  </div>
                </article>
              )
            })}
          </div>
        ) : null}
      </section>

      {section === "presupuestos" ? (
        <section className="rounded-[2rem] border border-slate-200 bg-white p-6 shadow-sm">
          <div className="flex flex-wrap items-end justify-between gap-3">
            <div>
              <p className="text-xs font-semibold uppercase tracking-[0.3em] text-slate-400">Presupuesto</p>
              <h3 className="mt-2 text-xl font-semibold tracking-tight text-slate-950">Lista compartible</h3>
            </div>
            <button
              type="button"
              className="rounded-full bg-slate-950 px-4 py-2 text-sm font-semibold text-white transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-60"
              disabled={!listaCompartibleLink}
              onClick={() => {
                void shareList()
              }}
            >
              Compartir lista
            </button>
          </div>

          <div className="mt-4 grid gap-4 md:grid-cols-3">
            <SummaryCard label="Seleccionados" value={String(listaResumen.cantidad)} detail="Productos en la lista actual." />
            <SummaryCard
              label="Total estimado"
              value={currencyFormatter.format(listaResumen.totalEstimado)}
              detail="Suma referencial de precios finales."
            />
            <SummaryCard
              label="Asesor aplicado"
              value={selectedAsesor?.nombre || "Sin asesor"}
              detail="Se agrega en el link compartido."
            />
          </div>

          {listaCompartibleLink ? (
            <div className="mt-4 rounded-2xl border border-slate-200 bg-slate-50 p-4">
              <p className="text-xs font-semibold uppercase tracking-[0.2em] text-slate-500">Link de presupuesto</p>
              <p className="mt-2 break-all text-sm text-slate-700">{listaCompartibleLink}</p>
            </div>
          ) : null}
        </section>
      ) : null}

      <section className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        <SummaryCard label="Productos operables" value={String(productosOperables.length)} detail="Canonizados + stock disponible." />
        <SummaryCard
          label="Sugeridos de hoy"
          value={String(productosSugeridosHoy.length)}
          detail="Recorte diario para foco comercial."
        />
        <SummaryCard
          label="Asesor en links"
          value={selectedAsesor ? "Si" : "No"}
          detail="Atribucion comercial opcional."
        />
        <SummaryCard
          label="Storefront configurado"
          value={normalizeHost(storefrontHost) || "Pendiente"}
          detail="Dominio usado para compartir."
        />
      </section>
    </section>
  )
}

function SummaryCard({ label, value, detail }: { label: string; value: string; detail: string }) {
  return (
    <article className="rounded-[2rem] border border-slate-200 bg-white p-6 shadow-sm">
      <p className="text-[11px] font-semibold uppercase tracking-[0.28em] text-slate-400">{label}</p>
      <p className="mt-4 text-2xl font-semibold tracking-tight text-slate-950">{value}</p>
      <p className="mt-2 text-sm text-slate-600">{detail}</p>
    </article>
  )
}

