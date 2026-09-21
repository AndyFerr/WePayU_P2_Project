package br.ufal.ic.p2.wepayu.schedule;

import java.time.DayOfWeek;

/** Agendas padrão do sistema ("semanalmente", "bi-semanalmente" e "mensalmente"). */
public final class Schedules {
    public static final PaymentSchedule WEEKLY = new WeeklySchedule(1, DayOfWeek.FRIDAY);
    public static final PaymentSchedule BIWEEKLY = new WeeklySchedule(2, DayOfWeek.FRIDAY);
    public static final PaymentSchedule MONTHLY = MonthlySchedule.lastBusinessDay();

    private Schedules() {
    }
}
