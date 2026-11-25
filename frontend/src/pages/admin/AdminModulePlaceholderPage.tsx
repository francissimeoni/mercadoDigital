import { Link } from "react-router-dom"

type ModuleStatus = "conectable" | "parcial" | "planificado"

const toneByStatus: Record<ModuleStatus, string> = {
  conectable: "border-emerald-200 bg-emerald-50 text-emerald-700",
  parcial: "border-amber-200 bg-amber-50 text-amber-700",
  planificado: "border-slate-200 bg-slate-100 text-slate-600",
}

const copyByStatus: Record<ModuleStatus, string> = {
  conectable: "Hay backend o superficies reales que permiten construir esta pantalla en la siguiente iteracion.",
  parcial: "El modulo tiene base real, pero todavia necesita conexion o consolidacion de contratos para verse completo.",
  planificado: "La maqueta queda preparada para el flujo futuro, sin inventar logica ni estados que hoy no existen.",
}

export default function AdminModulePlaceholderPage({
  eyebrow,
  title,
  description,
  status,
  bullets,
  primaryCta,
  secondaryCta,
}: {
  eyebrow: string
  title: string
  description: string
  status: ModuleStatus
  bullets: string[]
  primaryCta?: { label: string; to: string }
  secondaryCta?: { label: string; to: string }
}) {
  return (
    <section className="space-y-6">
      <header className="rounded-[2rem] border border-slate-200 bg-white p-8 shadow-sm">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div className="max-w-3xl">
            <p className="text-xs font-semibold uppercase tracking-[0.35em] text-slate-400">{eyebrow}</p>
            <h2 className="mt-3 text-3xl font-semibold tracking-tight text-slate-950">{title}</h2>
            <p className="mt-3 text-sm leading-6 text-slate-600">{description}</p>
          </div>
          <span className={`rounded-full border px-3 py-1 text-xs font-semibold uppercase tracking-[0.2em] ${toneByStatus[status]}`}>
            {status}
          </span>
        </div>
      </header>

      <div className="grid gap-6 xl:grid-cols-[1.35fr_1fr]">
        <section className="rounded-[2rem] border border-slate-200 bg-white p-8 shadow-sm">
          <h3 className="text-xl font-semibold tracking-tight text-slate-950">Que va a resolver este modulo</h3>
          <p className="mt-2 text-sm text-slate-600">{copyByStatus[status]}</p>
          <div className="mt-6 grid gap-3">
            {bullets.map((bullet) => (
              <article key={bullet} className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-4 text-sm text-slate-700">
                {bullet}
              </article>
            ))}
          </div>
        </section>

        <section className="rounded-[2rem] border border-slate-200 bg-white p-8 shadow-sm">
          <h3 className="text-xl font-semibold tracking-tight text-slate-950">Siguiente paso recomendado</h3>
          <p className="mt-2 text-sm leading-6 text-slate-600">
            Esta pantalla ya queda integrada al layout admin y lista para recibir conexiones reales sin volver a
            replantear navegacion, jerarquia ni estados visuales base.
          </p>

          <div className="mt-6 space-y-3">
            {primaryCta ? (
              <Link
                to={primaryCta.to}
                className="block rounded-2xl bg-slate-950 px-4 py-4 text-sm font-medium text-white transition hover:bg-slate-800"
              >
                {primaryCta.label}
              </Link>
            ) : null}
            {secondaryCta ? (
              <Link
                to={secondaryCta.to}
                className="block rounded-2xl border border-slate-200 bg-slate-50 px-4 py-4 text-sm font-medium text-slate-700 transition hover:border-blue-200 hover:bg-blue-50 hover:text-blue-700"
              >
                {secondaryCta.label}
              </Link>
            ) : null}
            <Link
              to="/admin"
              className="block rounded-2xl border border-slate-200 bg-white px-4 py-4 text-sm font-medium text-slate-700 transition hover:border-blue-200 hover:bg-blue-50 hover:text-blue-700"
            >
              Volver al dashboard admin
            </Link>
          </div>
        </section>
      </div>
    </section>
  )
}
