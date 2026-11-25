import { useState } from "react"
type Props = { src: string | null; alt: string; className?: string; fallbackClassName?: string; priority?: boolean }
export default function StorefrontProductImage({ src, alt, className, fallbackClassName, priority = false }: Props) {
  const [failedSource, setFailedSource] = useState<string | null>(null)
  if (!src || src === failedSource) return <div className={fallbackClassName || "sf-image-fallback"} role="img" aria-label={`${alt}, imagen no disponible`}>Imagen no disponible</div>
  return <img src={src} alt={alt} className={className} loading={priority ? "eager" : "lazy"} decoding="async" onError={() => setFailedSource(src)} />
}
