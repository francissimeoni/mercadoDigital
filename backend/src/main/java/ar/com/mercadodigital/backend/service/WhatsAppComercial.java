package ar.com.mercadodigital.backend.service;

import ar.com.mercadodigital.backend.exception.BadRequestException;

/** Guarda E.164 con +, de forma idempotente. No verifica que exista una cuenta en WhatsApp. */
public final class WhatsAppComercial {
    private WhatsAppComercial() {}

    /** Captura nueva con país explícito. Los contactos históricos conservan su interpretación anterior. */
    public static String normalizar(String value, String pais, Boolean movil) {
        if (pais == null || pais.isBlank()) return normalizar(value);
        if (value == null || value.isBlank()) return null;
        String region = pais.toUpperCase(java.util.Locale.ROOT);
        var util = com.google.i18n.phonenumbers.PhoneNumberUtil.getInstance();
        if (!util.getSupportedRegions().contains(region)) return invalido();
        String raw = value.trim();
        if (!raw.matches("[+0-9() .-]+") || raw.indexOf('+', 1) >= 0) return invalido();
        String digits = raw.replaceAll("\\D", "");
        if (!raw.startsWith("+") && digits.startsWith("00")) raw = "+" + digits.substring(2);
        try {
            var number = util.parse(raw, region);
            // El usuario indica expresamente celular argentino; nunca se transforma un internacional explícito.
            if (region.equals("AR") && Boolean.TRUE.equals(movil) && !raw.startsWith("+")
                    && number.getCountryCode() == 54 && util.getNationalSignificantNumber(number).length() == 10) {
                number = util.parse("+549" + util.getNationalSignificantNumber(number), "AR");
            }
            if (number.hasExtension() || !util.isValidNumber(number)) return invalido();
            return util.format(number, com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat.E164);
        } catch (com.google.i18n.phonenumbers.NumberParseException ex) { return invalido(); }
    }
    public static String normalizar(String value) {
        if (value == null || value.isBlank()) return null;
        String raw = value.trim();
        if (!raw.matches("[+0-9() .-]+") || raw.indexOf('+', 1) >= 0) return invalido();
        String digits = raw.replaceAll("\\D", "");
        if (raw.startsWith("+")) return internacional(digits);
        if (digits.startsWith("00")) return internacional(digits.substring(2));
        if (digits.startsWith("54")) return internacional(digits);
        // Formato argentino explícitamente móvil: 011 15 1234 5678 / 0351 15 1234567.
        if (digits.startsWith("0")) {
            String nacional = digits.substring(1);
            for (int area = 2; area <= 4; area++) {
                boolean areaValida = area == 2 ? nacional.startsWith("11") : nacional.matches("[23].*");
                if (areaValida && nacional.length() == 12 && nacional.startsWith("15", area)) {
                    return internacional("549" + nacional.substring(0, area) + nacional.substring(area + 2));
                }
            }
        }
        return invalido();
    }
    private static String internacional(String digits) {
        if (!digits.matches("[1-9][0-9]{7,14}")) return invalido();
        if (digits.startsWith("54")) {
            if (digits.startsWith("549")) {
                if (digits.length() != 13 || !digits.substring(3).matches("[123][0-9]{9}")) return invalido();
            } else if (digits.length() != 12 || !digits.substring(2).matches("[123][0-9]{9}")) return invalido();
        }
        return "+" + digits;
    }
    private static String invalido() {
        throw new BadRequestException("WhatsApp inválido o ambiguo. Indicá el número internacional, por ejemplo +54 9 11 1234 5678.");
    }
}
