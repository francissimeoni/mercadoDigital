import { useMutation } from "@tanstack/react-query"
import { useState, type FormEvent } from "react"
import { Link, useParams } from "react-router-dom"
import { uploadTransferReceipt } from "../api/storefrontPedidoApi"
import FeedbackAlert from "../../components/ui/FeedbackAlert"

export default function StorefrontTransferReceiptPage() {
  const { token = "" } = useParams()
  const [file, setFile] = useState<File | null>(null)
  const upload = useMutation({ mutationFn: () => { if (!file) throw new Error("Selecciona un comprobante"); return uploadTransferReceipt(token, file) } })
  const submit = (event: FormEvent) => { event.preventDefault(); upload.mutate() }

  if (upload.data) return <section className="mx-auto max-w-2xl space-y-5 rounded-[2rem] border border-emerald-200 bg-white p-6 shadow-sm sm:p-9"><p className="text-xs font-bold uppercase tracking-[.2em] text-emerald-700">Comprobante recibido</p><h1 className="text-3xl font-semibold text-slate-950">¡Gracias por tu compra!</h1><p className="leading-7 text-slate-600">El pedido esta en proceso. Te informaremos por correo las diferentes etapas.</p><FeedbackAlert variant="success" title="Carga exitosa">{upload.data.mensaje}</FeedbackAlert><div className="rounded-2xl bg-slate-50 p-4 text-sm text-slate-700"><p>Para dudas o consultas comunicate con <strong>{upload.data.tiendaNombre}</strong>.</p>{upload.data.telefono ? <p className="mt-2">WhatsApp: {upload.data.telefono}</p> : null}{upload.data.email ? <p>Email: {upload.data.email}</p> : null}</div><div className="flex flex-wrap gap-3"><Link to={`/pedido/${upload.data.trackingToken}`} className="rounded-full bg-slate-950 px-5 py-3 text-sm font-semibold text-white">Ver proceso del pedido</Link><Link to="/ingresar" className="rounded-full border border-slate-300 px-5 py-3 text-sm font-semibold text-slate-700">Ingresar a mi cuenta</Link></div></section>

  return <section className="mx-auto max-w-2xl rounded-[2rem] border border-slate-200 bg-white p-6 shadow-sm sm:p-9"><p className="text-xs font-bold uppercase tracking-[.2em] text-slate-500">Pago por transferencia</p><h1 className="mt-2 text-3xl font-semibold text-slate-950">Cargar comprobante</h1><p className="mt-3 text-sm leading-6 text-slate-600">Adjunta una imagen o PDF legible. El comercio revisara el pago antes de confirmar el pedido.</p><form onSubmit={submit} className="mt-6 space-y-4"><label className="block rounded-2xl border-2 border-dashed border-slate-300 bg-slate-50 p-6 text-center"><input required type="file" accept="image/png,image/jpeg,image/webp,application/pdf" onChange={(event) => setFile(event.target.files?.[0] ?? null)} className="block w-full text-sm" /><span className="mt-2 block text-xs text-slate-500">JPG, PNG, WEBP o PDF · maximo 8 MB</span></label>{upload.isError ? <FeedbackAlert variant="error" title="No se pudo cargar">Revisa el archivo o solicita nuevamente el enlace al comercio.</FeedbackAlert> : null}<button disabled={!file || upload.isPending || !token} className="w-full rounded-full bg-emerald-600 px-5 py-3 text-sm font-semibold text-white disabled:opacity-50">{upload.isPending ? "Cargando..." : "Enviar comprobante"}</button></form></section>
}
