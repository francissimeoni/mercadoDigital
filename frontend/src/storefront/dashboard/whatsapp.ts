/** Matches accepted backend formats; this validates syntax, not WhatsApp membership. */
export function normalizeCommercialPhone(value: string | null | undefined): string | null {
  const raw = value?.trim() ?? ""
  if (!raw || !/^[+0-9() .-]+$/.test(raw) || raw.indexOf("+", 1) >= 0) return null
  let digits = raw.replace(/\D/g, "")
  if (!raw.startsWith("+") && digits.startsWith("00")) digits = digits.slice(2)
  else if (!raw.startsWith("+") && !digits.startsWith("54")) {
    if (!digits.startsWith("0")) return null
    const local = digits.slice(1)
    let converted = ""
    for (let area = 2; area <= 4; area++) {
      const validArea = area === 2 ? local.startsWith("11") : /^[23]/.test(local)
      if (validArea && local.length === 12 && local.startsWith("15", area)) { converted = "549" + local.slice(0, area) + local.slice(area + 2); break }
    }
    if (!converted) return null
    digits = converted
  }
  if (!/^[1-9][0-9]{7,14}$/.test(digits)) return null
  if (digits.startsWith("549") && !/^549[123][0-9]{9}$/.test(digits)) return null
  if (digits.startsWith("54") && !digits.startsWith("549") && !/^54[123][0-9]{9}$/.test(digits)) return null
  return "+" + digits
}
export function contactStatus(active: boolean, phone?: string | null) {
  if (!active) return "Inactivo"
  if (!phone?.trim()) return "Sin WhatsApp: no aparece al comprar"
  return normalizeCommercialPhone(phone) ? "Disponible para compras" : "Número inválido: no aparece al comprar"
}
