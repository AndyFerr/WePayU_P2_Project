package br.ufal.ic.p2.wepayu.schedule;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;

/** Pagamento mensal: em um dia fixo do mês, ou no último dia útil (feriados são ignorados). */
public class MonthlySchedule implements PaymentSchedule {
    private static final int LAST_BUSINESS_DAY = 0;

    private final int dayOfMonth;
    private final String description;

    private MonthlySchedule(int dayOfMonth, String description) {
        this.dayOfMonth = dayOfMonth;
        this.description = description;
    }

    public static MonthlySchedule lastBusinessDay(String description) {
        return new MonthlySchedule(LAST_BUSINESS_DAY, description);
    }

    public static MonthlySchedule onDay(int dayOfMonth, String description) {
        return new MonthlySchedule(dayOfMonth, description);
    }

    @Override
    public boolean isPayDay(LocalDate date) {
        if (dayOfMonth == LAST_BUSINESS_DAY) {
            return date.equals(lastBusinessDayOf(YearMonth.from(date)));
        }
        return date.getDayOfMonth() == Math.min(dayOfMonth, date.lengthOfMonth());
    }

    @Override
    public LocalDate periodStart(LocalDate payDay) {
        if (dayOfMonth == LAST_BUSINESS_DAY) {
            return payDay.withDayOfMonth(1);
        }
        return payDay.minusMonths(1).plusDays(1);
    }

    @Override
    public String getDescription() {
        return description;
    }

    /** O período mensal sempre equivale a um mês inteiro de salário. */
    @Override
    public BigDecimal periodicSalary(BigDecimal monthlySalary) {
        return monthlySalary;
    }

    private static LocalDate lastBusinessDayOf(YearMonth month) {
        LocalDate day = month.atEndOfMonth();
        while (day.getDayOfWeek() == DayOfWeek.SATURDAY || day.getDayOfWeek() == DayOfWeek.SUNDAY) {
            day = day.minusDays(1);
        }
        return day;
    }
}
