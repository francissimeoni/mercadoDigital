import { Link } from "react-router-dom"

type ProveedorMetricCardProps = {
  label: string
  value: string
  helper: string
  tone?: "blue" | "emerald" | "amber" | "violet"
  to?: string
}

const toneStyles: Record<NonNullable<ProveedorMetricCardProps["tone"]>, { shell: string; glow: string; value: string }> = {
  blue: { 
    shell: "border-slate-200 bg-white hover:border-blue-300", 
    glow: "bg-blue-50", 
    value: "text-slate-950" 
  },
  emerald: { 
    shell: "border-slate-200 bg-white hover:border-emerald-300", 
    glow: "bg-emerald-50", 
    value: "text-slate-950" 
  },
  amber: { 
    shell: "border-slate-200 bg-white hover:border-amber-300", 
    glow: "bg-amber-50", 
    value: "text-slate-950" 
  },
  violet: { 
    shell: "border-slate-200 bg-white hover:border-violet-300", 
    glow: "bg-violet-50", 
    value: "text-slate-950" 
  },
}

export default function ProveedorMetricCard({
  label,
  value,
  helper,
  tone = "blue",
  to,
}: ProveedorMetricCardProps) {
  const style = toneStyles[tone]
  const className = `relative overflow-hidden rounded-3xl border p-6 shadow-sm transition-all duration-200 ${style.shell} ${
    to ? "block hover:-translate-y-1 hover:shadow-lg focus:outline-none focus:ring-2 focus:ring-slate-400" : ""
  }`
  const content = (
    <>
      <div className={`absolute -right-4 -top-4 h-24 w-24 rounded-full opacity-50 blur-3xl ${style.glow}`} />
      <p className="relative text-[10px] font-bold uppercase tracking-[0.2em] text-slate-400">{label}</p>
      <p className={`relative mt-4 text-4xl font-bold tracking-tight ${style.value}`}>{value}</p>
      <p className="relative mt-2 text-xs font-medium text-slate-500">{helper}</p>
    </>
  )

  if (to) {
    return (
      <Link to={to} className={className}>
        {content}
      </Link>
    )
  }

  return (
    <article className={className}>
      {content}
    </article>
  )
}
