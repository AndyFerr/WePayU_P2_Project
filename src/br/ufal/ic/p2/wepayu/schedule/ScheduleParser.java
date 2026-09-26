package br.ufal.ic.p2.wepayu.schedule;

import br.ufal.ic.p2.wepayu.exceptions.WePayUException;

import java.time.DayOfWeek;

/**
 * Interpreta as strings de agenda usadas em criarAgendaDePagamentos e em
 * alteraEmpregado/agendaPagamento (US 10):
 * <ul>
 *   <li>"semanal D": toda semana, no dia D (1=segunda ... 7=domingo);</li>
 *   <li>"semanal N D": a cada N semanas (1-52), no dia D;</li>
 *   <li>"mensal D": todo mês, no dia D (1-28);</li>
 *   <li>"mensal $": todo mês, no último dia útil.</li>
 * </ul>
 * Qualquer outro formato, ou número fora da faixa, é inválido.
 */
public final class ScheduleParser {
    private static final String INVALID = "Descricao de agenda invalida";

    private ScheduleParser() {
    }

    public static PaymentSchedule parse(String description) throws WePayUException {
        if (description == null || description.isBlank()) {
            throw new WePayUException(INVALID);
        }
        String trimmed = description.trim();
        String[] tokens = trimmed.split("\\s+");
        try {
            if (tokens[0].equals("semanal")) {
                return parseWeekly(tokens, trimmed);
            }
            if (tokens[0].equals("mensal")) {
                return parseMonthly(tokens, trimmed);
            }
        } catch (NumberFormatException e) {
            throw new WePayUException(INVALID);
        }
        throw new WePayUException(INVALID);
    }

    private static PaymentSchedule parseWeekly(String[] tokens, String description) throws WePayUException {
        if (tokens.length == 2) {
            int day = Integer.parseInt(tokens[1]);
            require(day >= 1 && day <= 7);
            return new WeeklySchedule(1, DayOfWeek.of(day), description);
        }
        if (tokens.length == 3) {
            int every = Integer.parseInt(tokens[1]);
            int day = Integer.parseInt(tokens[2]);
            require(every >= 1 && every <= 52 && day >= 1 && day <= 7);
            return new WeeklySchedule(every, DayOfWeek.of(day), description);
        }
        throw new WePayUException(INVALID);
    }

    private static PaymentSchedule parseMonthly(String[] tokens, String description) throws WePayUException {
        if (tokens.length != 2) {
            throw new WePayUException(INVALID);
        }
        if (tokens[1].equals("$")) {
            return MonthlySchedule.lastBusinessDay(description);
        }
        int day = Integer.parseInt(tokens[1]);
        require(day >= 1 && day <= 28);
        return MonthlySchedule.onDay(day, description);
    }

    private static void require(boolean condition) throws WePayUException {
        if (!condition) {
            throw new WePayUException(INVALID);
        }
    }
}
