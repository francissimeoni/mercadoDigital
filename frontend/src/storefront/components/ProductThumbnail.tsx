import { useState } from "react"

export default function ProductThumbnail({ src }: { src?: string | null }) {
  const [failed, setFailed] = useState<string | null>(null)
  return <span className="dashboard-thumbnail" aria-hidden="true">{src && src !== failed
    ? <img src={src} alt="" loading="lazy" decoding="async" onError={() => setFailed(src)} />
    : <svg width="24" height="24" viewBox="0 0 24 24" fill="none"><rect x="3" y="3" width="18" height="18" rx="3" stroke="currentColor" /><path d="m4 17 5-5 4 4 3-3 4 4" stroke="currentColor" /><circle cx="15" cy="8" r="2" fill="currentColor" /></svg>}</span>
}
