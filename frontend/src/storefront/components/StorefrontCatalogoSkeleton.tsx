export default function StorefrontCatalogoSkeleton() {
  return (
    <div className="grid grid-cols-1 gap-5 md:grid-cols-2 xl:grid-cols-3">
      {Array.from({ length: 6 }).map((_, index) => (
        <article key={index} className="overflow-hidden rounded-3xl border border-slate-200 bg-white shadow-sm">
          <div className="aspect-[4/3] animate-pulse bg-slate-200" />
          <div className="space-y-4 p-5">
            <div className="h-3 w-1/3 animate-pulse rounded bg-slate-200" />
            <div className="h-6 w-2/3 animate-pulse rounded bg-slate-200" />
            <div className="h-7 w-1/2 animate-pulse rounded bg-slate-200" />
            <div className="grid gap-2 sm:grid-cols-2">
              <div className="h-10 animate-pulse rounded-2xl bg-slate-200" />
              <div className="h-10 animate-pulse rounded-2xl bg-slate-200" />
            </div>
            <div className="h-4 w-full animate-pulse rounded bg-slate-200" />
            <div className="h-10 w-32 animate-pulse rounded-full bg-slate-200" />
          </div>
        </article>
      ))}
    </div>
  )
}
