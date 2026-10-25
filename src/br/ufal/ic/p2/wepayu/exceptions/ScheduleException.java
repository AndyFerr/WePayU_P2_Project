package br.ufal.ic.p2.wepayu.exceptions;

/**
 * Erro de agenda de pagamentos (US 9/10): descrição em formato inválido, agenda que não
 * está disponível no sistema, ou tentativa de criar uma agenda que já existe.
 */
public class ScheduleException extends WePayUException {
    public ScheduleException(String message) {
        super(message);
    }

    public static ScheduleException invalidDescription() {
        return new ScheduleException("Descricao de agenda invalida");
    }

    public static ScheduleException notAvailable() {
        return new ScheduleException("Agenda de pagamento nao esta disponivel");
    }

    public static ScheduleException alreadyExists() {
        return new ScheduleException("Agenda de pagamentos ja existe");
    }
}