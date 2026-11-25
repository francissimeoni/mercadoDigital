package ar.com.mercadodigital.backend.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class DateTimeUtil {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private DateTimeUtil() {
    }

    public static String formatIso(LocalDateTime value) {
        return value == null ? null : ISO.format(value);
    }
}
