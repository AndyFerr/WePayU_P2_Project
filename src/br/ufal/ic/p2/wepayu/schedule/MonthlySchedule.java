package br.ufal.ic.p2.wepayu.schedule;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;

/** Pagamento mensal: em um dia fixo do mês ou no último dia útil (feriados são ignorados). */
public class MonthlySchedule implements PaymentSchedule {
    private static final int LAST_BUSINESS_DAY = 0;

    private final int dayOfMonth;

    private MonthlySchedule(int dayOfMonth) {
        this.dayOfMonth = dayOfMonth;
    }

    public static MonthlySchedule lastBusinessDay() {
        return new MonthlySchedule(LAST_BUSINESS_DAY);
    }

    public static MonthlySchedule onDay(int dayOfMonth) {
        return new MonthlySchedule(dayOfMonth);
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

    private static LocalDate lastBusinessDayOf(YearMonth month) {
        LocalDate day = month.atEndOfMonth();
        while (day.getDayOfWeek() == DayOfWeek.SATURDAY || day.getDayOfWeek() == DayOfWeek.SUNDAY) {
            day = day.minusDays(1);
        }
        return day;
    }
}
