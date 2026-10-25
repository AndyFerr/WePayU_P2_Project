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

    /**
     * Primeiro dia de pagamento em {@code day} ou depois dele.
     *
     * <p>Percorrer os dias é proposital: cada agenda decide o que é dia de pagamento à sua
     * maneira (dia da semana, a cada N semanas, dia do mês, último dia útil), e quem precisa
     * caminhar pelos pagamentos não deveria conhecer nenhuma dessas regras.
     */
    default LocalDate firstPayDayOnOrAfter(LocalDate day) {
        LocalDate current = day;
        while (!isPayDay(current)) {
            current = current.plusDays(1);
        }
        return current;
    }

    /** Dia de pagamento seguinte a {@code day}. */
    default LocalDate nextPayDayAfter(LocalDate day) {
        return firstPayDayOnOrAfter(day.plusDays(1));
    }

    /** String usada em agendaPagamento/criarAgendaDePagamentos (ex.: "semanal 5", "mensal $"). */
    String getDescription();

    /** Parte do salário mensal devida em um período completo desta agenda. */
    BigDecimal periodicSalary(BigDecimal monthlySalary);
}