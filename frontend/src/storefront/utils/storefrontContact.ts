export function normalizeWhatsAppPhone(phone: string): string | null {
  const digits = phone.replace(/\D/g, "")
  if (!digits) {
    return null
  }

  if (digits.startsWith("54")) {
    return digits.length >= 12 ? digits : null
  }

  if (digits.startsWith("0")) {
    const localNumber = digits.replace(/^0+/, "")
    if (localNumber.length === 10) {
      return `549${localNumber}`
    }

    return localNumber.length >= 8 ? `54${localNumber}` : null
  }

  if (digits.length === 10) {
    return `549${digits}`
  }

  if (digits.length === 11 && digits.startsWith("9")) {
    return `54${digits}`
  }

  return digits.length >= 8 ? digits : null
}

export function buildStorefrontWhatsAppUrl(phone: string, storeName?: string | null): string | null {
  const normalizedPhone = normalizeWhatsAppPhone(phone)
  if (!normalizedPhone) {
    return null
  }

  const trimmedStoreName = storeName?.trim()
  const message = trimmedStoreName
    ? `Hola ${trimmedStoreName}, me comunico desde su tienda online.`
    : "Hola, me comunico desde su tienda online."

  return `https://wa.me/${normalizedPhone}?text=${encodeURIComponent(message)}`
}
