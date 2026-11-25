import { useEffect, useRef } from "react"
export default function StorefrontImageDialog({ src, alt, close }: { src: string; alt: string; close: () => void }) {
  const dialog = useRef<HTMLDialogElement>(null)
  useEffect(() => {
    const origin = document.activeElement as HTMLElement | null
    const overflow = document.body.style.overflow
    dialog.current?.showModal(); document.body.style.overflow = "hidden"
    return () => { document.body.style.overflow = overflow; origin?.focus({ preventScroll: true }) }
  }, [])
  return <dialog ref={dialog} className="sf-image-dialog" aria-label={`Imagen ampliada: ${alt}`} onKeyDown={e => { if (e.key === "Tab") { e.preventDefault(); dialog.current?.querySelector("button")?.focus() } }} onCancel={e => { e.preventDefault(); close() }} onClick={e => { if (e.target === e.currentTarget) close() }}>
    <button type="button" autoFocus className="sf-button" onClick={close}>Cerrar imagen</button>
    <img src={src} alt={alt} />
  </dialog>
}
