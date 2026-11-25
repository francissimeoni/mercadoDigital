import { useEffect, useId } from "react"
import { isAxiosError } from "axios"

const dirtyForms = new Set<string>()
let previousHistory: { state: { idx?: number }; url: string } | null = null
let restoringHistory = false
function protectHistory(event: PopStateEvent) {
  if (restoringHistory) { restoringHistory = false; return }
  if (!previousHistory || confirmCatalogoLeave()) return
  event.stopImmediatePropagation()
  const currentIndex = window.history.state?.idx
  const previousIndex = previousHistory.state?.idx
  if (typeof currentIndex === "number" && typeof previousIndex === "number" && currentIndex !== previousIndex) {
    restoringHistory = true
    window.history.go(previousIndex - currentIndex)
  } else window.history.replaceState(previousHistory.state, "", previousHistory.url)
}
// Register before BrowserRouter installs its listener. React may unmount the dirty form
// synchronously during POP, so registering only inside an effect is too late.
if (typeof window !== "undefined") window.addEventListener("popstate", protectHistory, true)
function beforeUnload(event: BeforeUnloadEvent) { event.preventDefault(); event.returnValue = "" }
function protectLink(event: MouseEvent) {
  if (event.defaultPrevented || event.ctrlKey || event.metaKey || event.shiftKey || event.altKey || event.button !== 0) return
  const target = event.target instanceof Element ? event.target.closest("a[href]") : null
  if (target instanceof HTMLAnchorElement && target.target !== "_blank" && !target.hasAttribute("download") && !confirmCatalogoLeave()) {
    event.preventDefault(); event.stopPropagation()
  }
}
export function confirmCatalogoLeave() {
  return dirtyForms.size === 0 || window.confirm("Tenés cambios de catálogo sin guardar. ¿Querés salir y descartarlos?")
}
export function useCatalogoDraft(dirty: boolean) {
  const id = useId()
  useEffect(() => {
    if (!dirty) return
    dirtyForms.add(id)
    if (dirtyForms.size === 1) {
      previousHistory = { state: window.history.state, url: window.location.href }
      window.addEventListener("beforeunload", beforeUnload)
      document.addEventListener("click", protectLink, true)
    }
    return () => {
      dirtyForms.delete(id)
      if (dirtyForms.size === 0) {
        previousHistory = null
        restoringHistory = false
        window.removeEventListener("beforeunload", beforeUnload)
        document.removeEventListener("click", protectLink, true)
      }
    }
  }, [id, dirty])
}
export function catalogoError(error: unknown) {
  if (isAxiosError<{ message?: string; details?: string[] }>(error)) {
    const data = error.response?.data
    return data?.details?.length ? data.details.join(". ") : data?.message ?? "No se pudo completar la operación. Volvé a intentarlo."
  }
  return error instanceof Error ? error.message : "No se pudo completar la operación. Volvé a intentarlo."
}
