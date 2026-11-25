export default function ProductoDetalleSkeleton() {
  return (
    <section className="rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        <div className="aspect-[4/3] w-full animate-pulse rounded bg-slate-200" />
        <div className="space-y-4">
          <div className="h-8 w-2/3 animate-pulse rounded bg-slate-200" />
          <div className="h-20 w-full animate-pulse rounded bg-slate-200" />
          <div className="h-7 w-1/3 animate-pulse rounded bg-slate-200" />
          <div className="h-4 w-1/2 animate-pulse rounded bg-slate-200" />
          <div className="h-4 w-1/3 animate-pulse rounded bg-slate-200" />
          <div className="h-10 w-40 animate-pulse rounded bg-slate-200" />
        </div>
      </div>
    </section>
  )
}
