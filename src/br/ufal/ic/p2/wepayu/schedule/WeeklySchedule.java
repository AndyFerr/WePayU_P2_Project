package br.ufal.ic.p2.wepayu.schedule;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

/**
 * Pagamento a cada {@code everyWeeks} semanas em um dia da semana fixo.
 * Para intervalos maiores que 1, as semanas são contadas a partir de 1/1/2005
 * (ex.: a primeira sexta bi-semanal é 14/1/2005).
 */
public class WeeklySchedule implements PaymentSchedule {
    private static final LocalDate ANCHOR = LocalDate.of(2005, 1, 1);
    private static final int WEEKS_PER_YEAR = 52;

    private final int everyWeeks;
    private final DayOfWeek dayOfWeek;
    private final String description;

    public WeeklySchedule(int everyWeeks, DayOfWeek dayOfWeek, String description) {
        this.everyWeeks = everyWeeks;
        this.dayOfWeek = dayOfWeek;
        this.description = description;
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

    @Override
    public String getDescription() {
        return description;
    }

    /** salário anual (mensal x 12) dividido pelas 52 semanas do ano, x quantas semanas o período cobre. */
    @Override
    public BigDecimal periodicSalary(BigDecimal monthlySalary) {
        BigDecimal annual = monthlySalary.multiply(BigDecimal.valueOf(12L * everyWeeks));
        return annual.divide(BigDecimal.valueOf(WEEKS_PER_YEAR), 10, RoundingMode.DOWN).setScale(2, RoundingMode.DOWN);
    }
}
