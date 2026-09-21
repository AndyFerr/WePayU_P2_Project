package br.ufal.ic.p2.wepayu.utils;

import java.time.LocalDate;

/** Intervalos semiabertos [from, toExclusive), a convenção usada pelos testes de consulta. */
public final class DateRange {
    private DateRange() {
    }

    public static boolean contains(LocalDate date, LocalDate from, LocalDate toExclusive) {
        return !date.isBefore(from) && date.isBefore(toExclusive);
    }
}
