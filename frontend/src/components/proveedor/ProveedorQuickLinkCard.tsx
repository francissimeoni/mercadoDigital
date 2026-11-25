import { Link } from "react-router-dom"

type ProveedorQuickLinkCardProps = {
  to: string
  title: string
  description: string
  status: string
  ctaLabel?: string
  detail?: string
  tone?: "slate" | "blue" | "emerald" | "amber"
}

export default function ProveedorQuickLinkCard({
  to,
  title,
  description,
  status,
  ctaLabel = "Abrir modulo",
  detail,
  tone = "slate",
}: ProveedorQuickLinkCardProps) {
  const statusClassName =
    tone === "blue"
      ? "bg-blue-50 text-blue-700"
      : tone === "emerald"
        ? "bg-emerald-50 text-emerald-700"
        : tone === "amber"
          ? "bg-amber-50 text-amber-700"
          : "bg-slate-100 text-slate-600"

  return (
    <Link
      to={to}
      className="group rounded-3xl border border-slate-200 bg-white p-5 shadow-sm transition hover:-translate-y-0.5 hover:border-slate-300 hover:shadow-md"
    >
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="text-lg font-semibold text-slate-950 transition group-hover:text-slate-700">{title}</p>
          <p className="mt-2 text-sm leading-6 text-slate-600">{description}</p>
          {detail ? <p className="mt-3 text-xs font-medium uppercase tracking-[0.18em] text-slate-400">{detail}</p> : null}
        </div>
        <span className={`rounded-full px-3 py-1 text-xs font-semibold uppercase tracking-[0.18em] ${statusClassName}`}>
          {status}
        </span>
      </div>
      <div className="mt-5 flex items-center justify-between border-t border-slate-100 pt-4 text-sm">
        <span className="font-semibold text-slate-700 transition group-hover:text-slate-950">{ctaLabel}</span>
        <span className="text-slate-400 transition group-hover:translate-x-0.5 group-hover:text-slate-700">→</span>
      </div>
    </Link>
  )
}
