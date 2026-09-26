package br.ufal.ic.p2.wepayu.schedule;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Agenda de pagamento (Strategy). Decide se um dia é dia de pagamento, qual o início do
 * período que esse pagamento cobre e, para assalariado/comissionado, qual a fração do
 * salário mensal cabe a um período dessa agenda (US 9/10: cada empregado escolhe a sua
 * entre as agendas registradas no sistema, e novas agendas podem ser criadas em runtime).
 */
public interface PaymentSchedule extends Serializable {

    boolean isPayDay(LocalDate date);

    /** Primeiro dia (inclusive) do período pago em {@code payDay}. */
    LocalDate periodStart(LocalDate payDay);

    /** String usada em agendaPagamento/criarAgendaDePagamentos (ex.: "semanal 5", "mensal $"). */
    String getDescription();

    /** Parte do salário mensal devida em um período completo desta agenda. */
    BigDecimal periodicSalary(BigDecimal monthlySalary);
}
