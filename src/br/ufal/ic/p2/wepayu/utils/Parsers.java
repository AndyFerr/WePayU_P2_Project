package br.ufal.ic.p2.wepayu.utils;

import br.ufal.ic.p2.wepayu.exceptions.WePayUException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

/** Conversão de texto (formato dos scripts de teste) para tipos do domínio. */
public final class Parsers {
    /** Datas no formato d/M/yyyy, com validação estrita (30/2/2005 é inválida). */
    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("d/M/uuuu").withResolverStyle(ResolverStyle.STRICT);

    private Parsers() {
    }

    public static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    /** Aceita vírgula ou ponto como separador decimal ("2500,50"). */
    public static BigDecimal decimal(String raw, String nullMessage, String notNumericMessage)
            throws WePayUException {
        if (isBlank(raw)) {
            throw new WePayUException(nullMessage);
        }
        try {
            return new BigDecimal(raw.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new WePayUException(notNumericMessage);
        }
    }

    public static LocalDate date(String raw, String errorMessage) throws WePayUException {
        if (isBlank(raw)) {
            throw new WePayUException(errorMessage);
        }
        try {
            return LocalDate.parse(raw.trim(), DATE);
        } catch (DateTimeParseException e) {
            throw new WePayUException(errorMessage);
        }
    }
}
