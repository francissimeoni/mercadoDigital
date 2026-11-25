/** Punto o coma decimal, sin separadores de miles; nunca convierte dinero a Number. */
export function cotizacionDosDecimales(value: string | number | null | undefined): string {
  if (value == null || !/^\d+(?:\.\d+)?$/.test(String(value))) return ""
  const [integer, decimals = ""] = String(value).split(".")
  const cents = BigInt(integer) * 100n + BigInt(decimals.padEnd(2, "0").slice(0, 2)) + (Number(decimals[2] ?? "0") >= 5 ? 1n : 0n)
  return `${cents / 100n}.${String(cents % 100n).padStart(2, "0")}`
}

export function decimalCatalogo(value: string, fraction = 2): string | null {
  const text = value.trim()
  if (!new RegExp(`^\\d+(?:[.,]\\d{1,${fraction}})?$`).test(text)) return null
  const [integer, decimals = ""] = text.replace(",", ".").split(".")
  if (integer.replace(/^0+/, "").length > (fraction === 6 ? 13 : 17)) return null
  return `${integer.replace(/^0+(?=\d)/, "")}.${decimals.padEnd(fraction, "0")}`
}

export function dineroCatalogo(value: string | number | null | undefined): string {
  if (value == null) return "Pendiente"
  const text = String(value)
  if (!/^\d+(?:\.\d+)?$/.test(text)) return "Pendiente"
  const [integer, decimals = ""] = text.split(".")
  return `$ ${BigInt(integer).toLocaleString("es-AR")},${decimals.padEnd(2, "0")}`
}
