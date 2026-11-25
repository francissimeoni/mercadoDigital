import { getCountries, parsePhoneNumberWithError, type CountryCode } from "libphonenumber-js/max"

const names = new Intl.DisplayNames(["es"], { type: "region" })
export const phoneCountries = getCountries().map(code => ({ code, name: names.of(code) ?? code })).sort((a, b) => a.name.localeCompare(b.name, "es"))
export function phoneCountry(value?: string | null): CountryCode {
  try { return parsePhoneNumberWithError(value ?? "").country ?? "AR" } catch { return "AR" }
}
export function parseCommercialPhone(value: string, country: CountryCode, mobile: boolean) {
  const raw = value.trim()
  if (!raw || !/^[+0-9() .-]+$/.test(raw) || raw.indexOf("+", 1) >= 0) return null
  const digits = raw.replace(/\D/g, "")
  const explicit = !raw.startsWith("+") && digits.startsWith("00") ? "+" + digits.slice(2) : raw
  try {
    let phone = parsePhoneNumberWithError(explicit, { defaultCountry: country, extract: false })
    if (country === "AR" && mobile && !explicit.startsWith("+") && phone.countryCallingCode === "54" && phone.nationalNumber.length === 10)
      phone = parsePhoneNumberWithError("+549" + phone.nationalNumber)
    if (!phone.isValid() || phone.ext) return null
    return { number: phone.number, formatted: phone.formatInternational() }
  } catch { return null }
}
