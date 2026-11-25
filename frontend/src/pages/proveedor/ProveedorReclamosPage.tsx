import ProveedorModuleHeader from "../../components/proveedor/ProveedorModuleHeader"
import { useProveedorContext } from "../../hooks/useProveedorContext"

export default function ProveedorReclamosPage() {
  const { proveedorId } = useProveedorContext()

  return (
    <section className="space-y-6">
      <ProveedorModuleHeader
        eyebrow="Proveedor"
        title="Garantías y Soporte"
        description="Centro de resolución de incidencias, devoluciones y seguimiento de calidad de productos entregados."
        proveedorId={proveedorId}
      />
       <section className="rounded-3xl border border-slate-200 bg-white p-12 text-center shadow-sm">
        <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-slate-50 text-slate-400">
          <svg className="h-8 w-8" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>
        </div>
        <h3 className="mt-6 text-xl font-bold text-slate-950">Sin incidencias registradas</h3>
        <p className="mx-auto mt-2 max-w-sm text-sm text-slate-500">No hay reclamos o solicitudes de garantía activos vinculados a su cuenta de proveedor.</p>
      </section>
    </section>
  )
}
