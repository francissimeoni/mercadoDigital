type StorefrontEmptyStateProps = {
  title: string
  message: string
}

export default function StorefrontEmptyState({ title, message }: StorefrontEmptyStateProps) {
  return (
    <section className="rounded-2xl border border-slate-200 bg-white p-8 text-center shadow-sm">
      <h2 className="text-xl font-semibold text-slate-900">{title}</h2>
      <p className="mt-3 text-sm leading-6 text-slate-600">{message}</p>
    </section>
  )
}
