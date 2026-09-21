package br.ufal.ic.p2.wepayu.schedule;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * Agenda de pagamento (Strategy). Decide se um dia é dia de pagamento e qual
 * o início do período que esse pagamento cobre. As User Stories 9 e 10
 * (agendas escolhidas/criadas pelo usuário) só precisarão de novas
 * implementações e de um parser de strings como "semanal 2 segunda".
 */
public interface PaymentSchedule extends Serializable {

    boolean isPayDay(LocalDate date);

    /** Primeiro dia (inclusive) do período pago em {@code payDay}. */
    LocalDate periodStart(LocalDate payDay);
}
