import { Link } from "react-router-dom"
import type { TenantBranding } from "../../tenant/tenantTypes"
import { resolveStorefrontDisplayName } from "../utils/storefrontBranding"

type StorefrontHeroProps = {
  branding: TenantBranding | null
}

export default function StorefrontHero({ branding }: StorefrontHeroProps) {
  const accentColor = branding?.colorPrimario || "#1d4ed8"
  const secondaryColor = branding?.colorSecundario || "#e2e8f0"

  return (
    <section
      className="overflow-hidden rounded-[2rem] border border-white/20 shadow-xl"
      style={{
        backgroundImage: `linear-gradient(135deg, ${accentColor} 0%, ${accentColor}ee 50%, ${secondaryColor}40 100%)`,
      }}
    >
      <div className="grid gap-6 px-5 py-6 text-white sm:px-6 sm:py-8 lg:grid-cols-[1.1fr_0.9fr] lg:px-10 lg:py-10">
        <div className="space-y-5">
          <span className="inline-flex rounded-full bg-white/15 px-3 py-1 text-xs font-semibold uppercase tracking-[0.24em] text-white backdrop-blur-sm">
            Tienda oficial
          </span>

          <div className="space-y-3">
            <h2 className="max-w-2xl text-3xl font-bold tracking-tight text-white lg:text-4xl">
              {branding?.heroTitulo || resolveStorefrontDisplayName(branding) || "Tienda online"}
            </h2>
            <p className="max-w-2xl text-base leading-7 text-white/90">
              {branding?.heroSubtitulo ||
                "Una tienda clara, profesional y lista para comprar online con precios publicados, productos visibles y seguimiento del pedido."}
            </p>
          </div>

          <div className="grid gap-3 sm:grid-cols-3">
            <FeaturePill title="Compra directa" detail="Del producto a formas de pago en pocos pasos." />
            <FeaturePill title="Pago claro" detail="Formas de pago seguras y confirmacion al instante." />
            <FeaturePill title="Seguimiento" detail="Pedido, envio y postventa en un mismo recorrido." />
          </div>

          <div className="flex flex-col gap-3 sm:flex-row">
            <Link
              to="/"
              className="min-h-[44px] inline-flex items-center justify-center rounded-full bg-white px-5 py-3 text-sm font-semibold shadow-lg transition hover:bg-white/95"
              style={{ color: accentColor }}
            >
              Explorar productos
            </Link>
            <Link
              to="/login"
              className="min-h-[44px] inline-flex items-center justify-center rounded-full border-2 border-white/30 bg-white/10 px-5 py-3 text-sm font-semibold text-white backdrop-blur-sm transition hover:bg-white/20"
            >
              Iniciar sesion
            </Link>
          </div>
        </div>

        <div className="grid gap-4">
          <div className="overflow-hidden rounded-[1.75rem] border border-white/20 bg-white/10 text-white shadow-xl backdrop-blur-sm">
            {branding?.heroImagenUrl ? (
              <div className="relative h-full min-h-64 w-full overflow-hidden">
                <img
                  src={branding.heroImagenUrl}
                  alt={branding.heroTitulo || branding.nombreComercial}
                  className="h-full min-h-64 w-full object-cover"
                />
                <div className="absolute inset-0 bg-gradient-to-t from-black/60 via-black/20 to-transparent" />
                <div className="absolute inset-x-0 bottom-0 p-6">
                  <p className="text-xs uppercase tracking-[0.28em] text-white/80">Compra online</p>
                  <p className="mt-2 max-w-sm text-xl font-semibold text-white">Una tienda pensada para decidir rapido y comprar con confianza.</p>
                </div>
              </div>
            ) : (
              <div className="flex h-full min-h-64 flex-col justify-between p-6">
                <p className="text-sm uppercase tracking-[0.24em] text-white/70">Experiencia comercial</p>
                <div className="space-y-2">
                  <p className="text-2xl font-bold text-white">{resolveStorefrontDisplayName(branding) || "Tienda online"}</p>
                  <p className="max-w-sm text-sm leading-6 text-white/80">
                    Una presencia digital prolija para exhibir marca, vender y sostener el seguimiento del pedido de punta a punta.
                  </p>
                </div>
              </div>
            )}
          </div>

          <div className="grid gap-3 sm:grid-cols-3">
            <StatCard label="Productos visibles" value="Compra simple" />
            <StatCard label="Operacion real" value="Compra segura" />
            <StatCard label="Postventa" value="Seguimiento activo" />
          </div>
        </div>
      </div>
    </section>
  )
}

function FeaturePill({ title, detail }: { title: string; detail: string }) {
  return (
    <article className="rounded-3xl border border-white/20 bg-white/10 px-4 py-4 backdrop-blur-sm">
      <p className="text-sm font-semibold text-white">{title}</p>
      <p className="mt-1 text-sm leading-6 text-white/80">{detail}</p>
    </article>
  )
}

function StatCard({ label, value }: { label: string; value: string }) {
  return (
    <article className="rounded-[1.5rem] border border-white/20 bg-white/10 px-4 py-4 backdrop-blur-sm">
      <p className="text-[11px] font-semibold uppercase tracking-[0.2em] text-white/70">{label}</p>
      <p className="mt-2 text-lg font-semibold text-white">{value}</p>
    </article>
  )
}
