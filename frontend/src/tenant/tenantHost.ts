export function normalizeTenantHost(host: string): string {
  let normalized = host.trim().toLowerCase().replace(/^https?:\/\//, "")

  const slashIndex = normalized.indexOf("/")
  if (slashIndex >= 0) {
    normalized = normalized.slice(0, slashIndex)
  }

  const portIndex = normalized.indexOf(":")
  if (portIndex >= 0) {
    normalized = normalized.slice(0, portIndex)
  }

  while (normalized.endsWith(".")) {
    normalized = normalized.slice(0, -1)
  }

  return normalized
}

export function alternateWwwHost(host: string): string | null {
  const normalized = normalizeTenantHost(host)
  if (!normalized) {
    return null
  }

  if (normalized.startsWith("www.")) {
    const withoutWww = normalized.slice(4)
    return withoutWww || null
  }

  return `www.${normalized}`
}
