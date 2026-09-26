package br.ufal.ic.p2.wepayu.schedule;

import java.time.DayOfWeek;

/** Agendas default do sistema: "semanal 5" (horista), "mensal $" (assalariado), "semanal 2 5" (comissionado). */
public final class Schedules {
    public static final PaymentSchedule WEEKLY = new WeeklySchedule(1, DayOfWeek.FRIDAY, "semanal 5");
    public static final PaymentSchedule BIWEEKLY = new WeeklySchedule(2, DayOfWeek.FRIDAY, "semanal 2 5");
    public static final PaymentSchedule MONTHLY = MonthlySchedule.lastBusinessDay("mensal $");

    private Schedules() {
    }
}
