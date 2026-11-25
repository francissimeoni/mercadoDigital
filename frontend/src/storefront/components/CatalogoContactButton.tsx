import { useEffect, useId, useRef, useState, type KeyboardEvent } from "react"
import { useQueryClient } from "@tanstack/react-query"
import { getCatalogoContacto, type CatalogoContactoProducto } from "../api/storefrontCatalogoApi"
import { buildCatalogoWhatsAppUrl, abrirCatalogoWhatsApp } from "../utils/catalogoWhatsApp"
import { formatPrecioArs } from "../utils/catalogoPublico"
import { trackStorefrontEvent } from "../api/storefrontMetricsApi"
import { WhatsAppIcon } from "./StorefrontWhatsAppFab"
import ContactAvatar from "./ContactAvatar"

type Props = { productoId: string; host: string; precioMostrado: string; compacto?: boolean }
const action = "inline-flex min-h-12 items-center justify-center gap-2 rounded-full bg-[#25D366] px-5 py-3 text-sm font-semibold text-slate-950 transition hover:bg-[#20bd5a] focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-emerald-700 disabled:opacity-50"

export default function CatalogoContactButton(props: Props) {
  const [open, setOpen] = useState(false)
  const trigger = useRef<HTMLButtonElement>(null)
  return <>
    <button ref={trigger} type="button" className={action} aria-haspopup="dialog" onClick={e => { e.stopPropagation(); setOpen(true) }}>
      <WhatsAppIcon className="h-5 w-5" />{props.compacto ? "Comprar" : "Comprar por WhatsApp"}
    </button>
    {open && <ContactDialog {...props} close={() => { setOpen(false); trigger.current?.focus() }} />}
  </>
}

function ContactDialog({ productoId, host, precioMostrado, close }: Props & { close: () => void }) {
  const dialog = useRef<HTMLDialogElement>(null)
  const controller = useRef<AbortController | null>(null)
  const opening = useRef(false)
  const client = useQueryClient()
  const titleId = useId()
  const descriptionId = useId()
  const [data, setData] = useState<CatalogoContactoProducto | null>(null)
  const [selected, setSelected] = useState<string | null>(null)
  const [busy, setBusy] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(null)

  const refreshPublicPrices = () => {
    void client.invalidateQueries({ queryKey: ["storefront-catalogo"] })
    void client.invalidateQueries({ queryKey: ["storefront-producto-detalle", productoId] })
  }

  useEffect(() => {
    dialog.current?.showModal()
    const previousOverflow = document.body.style.overflow
    document.body.style.overflow = "hidden"
    const abort = new AbortController()
    controller.current = abort
    getCatalogoContacto(host, productoId, abort.signal).then(result => {
      if (abort.signal.aborted) return
      setData(result)
      // Con varios contactos no hay selección implícita.
      setSelected(result.contactos.length === 1 ? result.contactos[0].id : null)
      if (result.precioPublicoArs !== precioMostrado) setNotice("El precio se actualizó. Revisá el importe antes de continuar.")
      refreshPublicPrices()
    }).catch(() => {
      if (!abort.signal.aborted) setError("No pudimos cargar los contactos de este producto. Podés reintentar.")
    }).finally(() => {
      if (!abort.signal.aborted) { controller.current = null; setBusy(false) }
    })
    return () => { abort.abort(); controller.current?.abort(); document.body.style.overflow = previousOverflow }
    // El diálogo representa el producto y precio al momento de abrirlo.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [host, productoId])

  const retry = async () => {
    if (controller.current) return
    const abort = new AbortController(); controller.current = abort
    setBusy(true); setError(null)
    try {
      const result = await getCatalogoContacto(host, productoId, abort.signal)
      if (abort.signal.aborted) return
      setData(result); setSelected(result.contactos.length === 1 ? result.contactos[0].id : null)
      if (result.precioPublicoArs !== precioMostrado) setNotice("El precio se actualizó. Revisá el importe antes de continuar.")
      refreshPublicPrices()
    } catch { if (!abort.signal.aborted) setError("No pudimos actualizar el producto. Reintentá o volvé al catálogo.") }
    finally { if (!abort.signal.aborted) { controller.current = null; setBusy(false) } }
  }

  const dismiss = () => { dialog.current?.close(); close() }
  const cycleFocus = (event: KeyboardEvent<HTMLDialogElement>) => {
    if (event.key !== "Tab") return
    const controls = [...event.currentTarget.querySelectorAll<HTMLElement>('button:not([disabled]), a[href], input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex="0"]')]
      .filter(control => !control.hidden && control.tabIndex >= 0)
    const first = controls[0], last = controls[controls.length - 1]
    if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last?.focus() }
    else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first?.focus() }
  }

  const continuar = async (contactoId: string) => {
    if (!data || controller.current || opening.current) return
    setSelected(contactoId)
    const abort = new AbortController(); controller.current = abort
    setBusy(true); setError(null); setNotice(null)
    try {
      // Revalidar también al continuar: puede cambiar precio, publicación o contacto mientras se elige.
      const fresh = await getCatalogoContacto(host, productoId, abort.signal)
      if (abort.signal.aborted) return
      const actual = fresh.contactos.find(c => c.id === contactoId)
      const anterior = data.contactos.find(c => c.id === contactoId)
      setData(fresh)
      if (fresh.precioPublicoArs !== data.precioPublicoArs || !actual || actual.whatsapp !== anterior?.whatsapp) {
        setSelected(actual ? contactoId : null)
        setNotice("El precio o el contacto cambió. Revisá la información y volvé a elegir el contacto.")
        refreshPublicPrices()
        return
      }
      const url = buildCatalogoWhatsAppUrl(fresh, actual)
      trackStorefrontEvent("CLICK_WHATSAPP", { host, productoId })
      opening.current = true
      abrirCatalogoWhatsApp(url)
    } catch { opening.current = false; if (!abort.signal.aborted) setError("No pudimos abrir WhatsApp con información actualizada. Reintentá para continuar.") }
    finally { if (!abort.signal.aborted) { controller.current = null; setBusy(opening.current) } }
  }

  return <dialog ref={dialog} aria-labelledby={titleId} aria-describedby={descriptionId}
    onClick={e => e.stopPropagation()} onKeyDown={cycleFocus} onCancel={e => { e.preventDefault(); dismiss() }}
    className="m-auto max-h-[90dvh] w-[calc(100%-2rem)] max-w-lg overflow-y-auto rounded-[1.75rem] border border-slate-200 bg-white p-0 text-slate-900 shadow-2xl backdrop:bg-slate-950/60">
    <div className="space-y-5 p-5 sm:p-7">
      <div className="flex items-start justify-between gap-3">
        <div><p className="text-xs font-semibold uppercase tracking-widest text-emerald-700">Atención directa</p><h2 id={titleId} className="mt-1 text-2xl font-semibold">Elegí con quién comprar</h2></div>
        <button autoFocus type="button" aria-label="Cerrar selector de contactos" className="min-h-11 min-w-11 rounded-full border border-slate-200 text-xl focus-visible:outline-2" onClick={dismiss}>×</button>
      </div>
      
      <p id={descriptionId} className="text-sm text-slate-600">Al elegir un contacto se abre WhatsApp.</p>
      {busy && <p role="status" className="text-sm text-slate-600">Actualizando producto y contactos...</p>}
      {notice && <p role="status" className="rounded-xl border border-amber-200 bg-amber-50 p-3 text-sm text-amber-900">{notice}</p>}
      {error && <div role="alert" className="rounded-xl border border-red-200 bg-red-50 p-3 text-sm text-red-800">{error}<button type="button" className="ml-2 min-h-11 font-semibold underline" disabled={busy} onClick={() => void retry()}>Reintentar</button></div>}
      {data && <>
        <div className="rounded-2xl bg-slate-50 p-4"><p className="break-words font-semibold">{data.nombre}</p><p className="mt-1 break-words text-2xl font-bold">{formatPrecioArs(data.precioPublicoArs)} <span className="text-xs font-normal text-slate-500">ARS</span></p>{data.condicionesEntrega && <p className="mt-2 whitespace-pre-line break-words text-sm text-slate-600">{data.condicionesEntrega}</p>}</div>
        {data.contactos.length === 0 ? <p role="status" className="rounded-xl border border-slate-200 p-4 text-sm">La tienda todavía no tiene contactos de WhatsApp disponibles. Volvé a intentarlo más tarde.</p> : <div role="group" aria-label="Contacto comercial" className="space-y-2">
          {data.contactos.map(c => <button key={c.id} type="button" disabled={busy || Boolean(error)} onClick={() => void continuar(c.id)} className={`flex min-h-16 w-full items-center justify-between gap-3 rounded-2xl border p-4 text-left focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-emerald-700 disabled:opacity-50 ${c.responsable ? "border-emerald-300 bg-emerald-50 hover:bg-emerald-100" : "border-slate-200 hover:bg-slate-50"} ${selected === c.id ? "ring-2 ring-emerald-600" : ""}`}>
            <ContactAvatar name={c.nombre} src={c.fotoUrl} />
            <span className="min-w-0 flex-1"><span className="block break-words font-semibold">{c.nombre}</span><span className={`mt-1 block text-xs ${c.responsable ? "font-semibold text-emerald-800" : "text-slate-500"}`}>{c.responsable ? "Responsable de la tienda" : "Asesor de ventas"}</span>{[c.localidad, c.provincia].filter(Boolean).length > 0 && <span className="mt-1 block break-words text-sm text-slate-600">{[c.localidad, c.provincia].filter(Boolean).join(" · ")}</span>}{c.zonaAtencion && <span className="mt-1 block break-words text-sm text-slate-600">{c.zonaAtencion}</span>}</span>
            <WhatsAppIcon className="h-6 w-6 shrink-0 text-emerald-700" />
          </button>)}
        </div>}
      </>}
    </div>
  </dialog>
}
