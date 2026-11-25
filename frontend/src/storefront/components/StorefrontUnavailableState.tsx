type StorefrontUnavailableStateProps = {
  host?: string | null
  title?: string
  message?: string
}

export default function StorefrontUnavailableState({
  host,
  title = "Esta tienda no esta disponible en este momento",
  message = "La pagina que intentas visitar fue pausada temporalmente. Si necesitas informacion comercial, contactanos por nuestros canales habituales.",
}: StorefrontUnavailableStateProps) {
  return (
    <section className="relative overflow-hidden rounded-[2rem] border border-amber-200/80 bg-white p-6 shadow-xl shadow-amber-100/40 sm:p-10">
      <div
        className="pointer-events-none absolute inset-0 opacity-60"
        style={{
          background:
            "radial-gradient(circle at 15% 25%, rgba(251,191,36,0.18), transparent 40%), radial-gradient(circle at 85% 20%, rgba(251,146,60,0.15), transparent 36%), radial-gradient(circle at 50% 85%, rgba(245,158,11,0.14), transparent 45%)",
        }}
      />

      <div className="relative grid gap-8 lg:grid-cols-[0.95fr_1.05fr] lg:items-center">
        <div className="flex justify-center lg:justify-start">
          <div className="w-full max-w-xs rounded-[1.75rem] border border-amber-200/70 bg-gradient-to-b from-amber-50 to-orange-50 p-5 shadow-md">
            <svg viewBox="0 0 280 220" role="img" aria-label="Tienda temporalmente no disponible" className="h-auto w-full">
              <rect x="35" y="70" width="210" height="120" rx="16" fill="#fff7ed" stroke="#fdba74" strokeWidth="2" />
              <rect x="45" y="82" width="190" height="28" rx="10" fill="#ffedd5" />
              <path d="M45 82H235L222 56H58L45 82Z" fill="#fb923c" />
              <path d="M58 56H222" stroke="#ea580c" strokeWidth="3" />
              <rect x="62" y="122" width="62" height="55" rx="8" fill="#fff" stroke="#fdba74" strokeWidth="2" />
              <rect x="136" y="122" width="82" height="20" rx="6" fill="#fed7aa" />
              <rect x="136" y="150" width="66" height="13" rx="6" fill="#ffedd5" />
              <circle cx="198" cy="42" r="20" fill="#fef3c7" stroke="#f59e0b" strokeWidth="2" />
              <path d="M188 42h20M198 32v20" stroke="#f59e0b" strokeWidth="3" strokeLinecap="round" />
              <rect x="92" y="20" width="96" height="24" rx="12" fill="#7f1d1d" />
              <text x="140" y="36" textAnchor="middle" fontSize="11" fontWeight="700" fill="#fee2e2">
                PAUSADA
              </text>
            </svg>
          </div>
        </div>

        <div className="space-y-4">
          <p className="inline-flex rounded-full border border-amber-300 bg-amber-50 px-3 py-1 text-xs font-semibold uppercase tracking-[0.18em] text-amber-800">
            Tienda online
          </p>
          <h2 className="text-2xl font-bold text-slate-900 sm:text-3xl">{title}</h2>
          <p className="max-w-2xl text-sm leading-7 text-slate-700 sm:text-base">{message}</p>
          {host ? (
            <p className="text-xs font-medium uppercase tracking-[0.16em] text-slate-500">
              Dominio consultado: <span className="normal-case tracking-normal text-slate-700">{host}</span>
            </p>
          ) : null}
          <div className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-600">
            Vuelve a intentar mas tarde o consulta con el administrador de la tienda.
          </div>
        </div>
      </div>
    </section>
  )
}
