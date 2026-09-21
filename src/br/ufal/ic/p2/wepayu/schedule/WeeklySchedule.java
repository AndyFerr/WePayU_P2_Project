package br.ufal.ic.p2.wepayu.schedule;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

/**
 * Pagamento a cada {@code everyWeeks} semanas em um dia da semana fixo.
 * Para intervalos maiores que 1, as semanas são contadas a partir de 1/1/2005
 * (primeira sexta bi-semanal = 14/1/2005).
 */
public class WeeklySchedule implements PaymentSchedule {
    private static final LocalDate ANCHOR = LocalDate.of(2005, 1, 1);

    private final int everyWeeks;
    private final DayOfWeek dayOfWeek;

    public WeeklySchedule(int everyWeeks, DayOfWeek dayOfWeek) {
        this.everyWeeks = everyWeeks;
        this.dayOfWeek = dayOfWeek;
    }

    @Override
    public boolean isPayDay(LocalDate date) {
        if (date.getDayOfWeek() != dayOfWeek) {
            return false;
        }
        LocalDate first = ANCHOR.with(TemporalAdjusters.nextOrSame(dayOfWeek)).plusWeeks(everyWeeks - 1L);
        long weeks = ChronoUnit.WEEKS.between(first, date);
        return Math.floorMod(weeks, (long) everyWeeks) == 0;
    }

    @Override
    public LocalDate periodStart(LocalDate payDay) {
        return payDay.minusDays(7L * everyWeeks - 1);
    }
}
