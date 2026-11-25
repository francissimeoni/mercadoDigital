import { Link } from "react-router-dom"

type ModuleStatus = "disponible" | "parcial" | "pendiente"

const toneByStatus: Record<ModuleStatus, string> = {
  disponible: "border-emerald-200 bg-emerald-50 text-emerald-700",
  parcial: "border-amber-200 bg-amber-50 text-amber-700",
  pendiente: "border-slate-200 bg-slate-100 text-slate-600",
}

export default function LogisticaPreparedModulePage({
  eyebrow,
  title,
  description,
  status,
  sections,
}: {
  eyebrow: string
  title: string
  description: string
  status: ModuleStatus
  sections: Array<{ title: string; detail: string }>
}) {
  return (
    <section className="space-y-6">
      <header className="rounded-[2rem] border border-slate-200 bg-gradient-to-br from-white via-slate-50 to-sky-50 p-4 shadow-sm sm:p-6 md:p-8">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
          <div className="max-w-4xl">
            <p className="text-xs font-semibold uppercase tracking-[0.35em] text-slate-400">{eyebrow}</p>
            <h2 className="mt-3 text-2xl font-semibold tracking-tight text-slate-950 sm:text-3xl">{title}</h2>
            <p className="mt-3 text-sm leading-6 text-slate-600">{description}</p>
          </div>
          <span className={`rounded-full border px-3 py-1 text-xs font-semibold uppercase tracking-[0.2em] ${toneByStatus[status]}`}>
            {status}
          </span>
        </div>
      </header>

      <div className="grid gap-6 xl:grid-cols-[1.25fr_1fr]">
        <section className="rounded-[2rem] border border-slate-200 bg-white p-4 shadow-sm sm:p-6 md:p-8">
          <h3 className="text-xl font-semibold tracking-tight text-slate-950 sm:text-2xl">Estructura prevista</h3>
          <div className="mt-6 space-y-3">
            {sections.map((section) => (
              <article key={section.title} className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-4">
                <p className="text-sm font-semibold text-slate-900">{section.title}</p>
                <p className="mt-1 text-sm text-slate-600">{section.detail}</p>
              </article>
            ))}
          </div>
        </section>

        <section className="rounded-[2rem] border border-slate-200 bg-white p-4 shadow-sm sm:p-6 md:p-8">
          <h3 className="text-xl font-semibold tracking-tight text-slate-950 sm:text-2xl">Estado del modulo</h3>
          <div className="mt-6 rounded-2xl border border-dashed border-slate-300 bg-slate-50 px-4 py-5 text-sm text-slate-500">
            Este modulo ya queda visible, ubicado y preparado para futura conexion backend. No se muestran numeros ni
            casos simulados para no confundir operacion con maqueta.
          </div>

          <div className="mt-6 space-y-3">
            <Link
              to="/logistica"
              className="block rounded-2xl bg-slate-950 px-4 py-4 text-sm font-medium text-white transition hover:bg-slate-800"
            >
              Volver al dashboard logistica
            </Link>
            <Link
              to="/logistica/envios"
              className="block rounded-2xl border border-slate-200 bg-slate-50 px-4 py-4 text-sm font-medium text-slate-700 transition hover:border-blue-200 hover:bg-blue-50 hover:text-blue-700"
            >
              Abrir modulo operativo actual
            </Link>
          </div>
        </section>
      </div>
    </section>
  )
}
