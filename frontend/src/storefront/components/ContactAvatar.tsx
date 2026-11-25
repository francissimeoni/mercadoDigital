import { useState } from "react"

export default function ContactAvatar({ name, src, className = "" }: { name: string; src?: string | null; className?: string }) {
  const [failed, setFailed] = useState<string | null>(null)
  const initials = name.trim().split(/\s+/).slice(0, 2).map(part => part[0]).join("").toLocaleUpperCase("es") || "?"
  return <span className={`contact-avatar ${className}`} aria-hidden="true">{src && src !== failed
    ? <img src={src} alt="" loading="lazy" decoding="async" onError={() => setFailed(src)} /> : initials}</span>
}
