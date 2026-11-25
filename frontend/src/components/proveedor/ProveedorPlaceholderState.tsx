import { Link } from "react-router-dom"

type ProveedorPlaceholderStateProps = {
  title: string
  message: string
  ctaTo?: string
  ctaLabel?: string
}

export default function ProveedorPlaceholderState({
  title,
  message,
  ctaTo,
  ctaLabel,
}: ProveedorPlaceholderStateProps) {
  return (
    <section className="rounded-3xl border border-slate-200 bg-white p-8 shadow-sm">
      <h2 className="text-xl font-semibold text-slate-950">{title}</h2>
      <p className="mt-3 max-w-2xl text-sm leading-6 text-slate-600">{message}</p>
      {ctaTo && ctaLabel ? (
        <Link
          to={ctaTo}
          className="mt-5 inline-flex rounded-full border border-slate-300 px-4 py-2 text-sm font-semibold text-slate-700 transition hover:border-slate-400 hover:text-slate-900"
        >
          {ctaLabel}
        </Link>
      ) : null}
    </section>
  )
}
