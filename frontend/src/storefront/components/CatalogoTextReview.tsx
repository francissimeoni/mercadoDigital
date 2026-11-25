import type { StorefrontContent } from "../api/storefrontOwnerApi"

export default function CatalogoTextReview({ content }: { content: StorefrontContent }) {
  const fields = [
    ["Título de portada", content.heroTitulo], ["Texto de portada", content.heroSubtitulo],
    ["Texto del pie de página", content.footerTexto], ["Quiénes somos", content.quienesTexto],
    ...(content.portadas ?? []).flatMap((slide, index) => [[`Portada ${index + 1}: título`, slide.titulo], [`Portada ${index + 1}: texto`, slide.subtitulo]]),
  ]
  const flagged = fields.filter(([, text]) => text && /checkout|seguro|log[ií]stica|pasarela|compra directa|env[ií]o gratis|compra asegurada|pago (?:online|en l[ií]nea)/i.test(text))
  return <aside className="space-y-2 rounded-xl border border-amber-200 bg-amber-50 p-4 text-sm text-amber-950">
    <p className="font-semibold">Revisión de los textos de tu catálogo</p>
    <p>La compra se coordina con el contacto elegido por WhatsApp. Revisá que tus textos describan tus condiciones actuales. Un envío incluido en el precio no implica envío gratis.</p>
    {flagged.length ? <><p>Revisá especialmente estos campos por sus referencias al proceso de compra o entrega:</p><ul className="list-disc pl-5">{flagged.map(([label]) => <li key={label}>{label}</li>)}</ul></> : <p>No detectamos esas expresiones en los campos revisados. Leé igualmente tu contenido antes de publicarlo.</p>}
    <p className="text-xs">Esta ayuda identifica expresiones; no modifica tus textos ni verifica las condiciones que ofrecés.</p>
  </aside>
}
