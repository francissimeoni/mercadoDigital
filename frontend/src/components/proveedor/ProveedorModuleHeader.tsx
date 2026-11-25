import type { ReactNode } from "react"

type ProveedorModuleHeaderProps = {
  eyebrow: string
  title: string
  description: string
  proveedorId?: string | null
  aside?: ReactNode
}

export default function ProveedorModuleHeader({
  eyebrow,
  title,
  description,
  proveedorId,
  aside,
}: ProveedorModuleHeaderProps) {
  return (
    <header className="rounded-3xl border border-slate-200 bg-white p-8 shadow-sm">
      <div className="flex flex-col gap-6 xl:flex-row xl:items-center xl:justify-between">
        <div>
          <p className="text-[10px] font-bold uppercase tracking-[0.25em] text-slate-400">{eyebrow}</p>
          <h2 className="mt-2 text-3xl font-bold tracking-tight text-slate-950">{title}</h2>
          <p className="mt-3 max-w-2xl text-sm leading-relaxed text-slate-500">{description}</p>
          {proveedorId ? (
            <p className="mt-4 inline-flex items-center rounded-lg bg-slate-50 px-2.5 py-1 text-[10px] font-bold uppercase tracking-wider text-slate-500 ring-1 ring-inset ring-slate-200">
              ID Cuenta: {proveedorId}
            </p>
          ) : null}
        </div>
        {aside ? <div className="flex flex-wrap gap-3">{aside}</div> : null}
      </div>
    </header>
  )
}
