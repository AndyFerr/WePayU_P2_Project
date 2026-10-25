package br.ufal.ic.p2.wepayu.utils;

import br.ufal.ic.p2.wepayu.exceptions.InvalidValueException;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Lê valores numéricos dos comandos aplicando a faixa exigida pelo campo.
 *
 * <p>Substitui os métodos {@code parseSalary}, {@code parseCommission}, {@code parseUnionFee}
 * e {@code parsePositive}, que eram o mesmo algoritmo repetido com mensagens diferentes: aqui
 * o algoritmo aparece uma vez e a mensagem vem do {@link MonetaryField}.
 */
public final class ValueParser {

    private ValueParser() {
    }

    /** Aceita zero; recusa negativo. Usado por salário, comissão e taxa sindical. */
    public static BigDecimal nonNegative(String raw, MonetaryField field) throws InvalidValueException {
        BigDecimal value = Parsers.decimal(raw, field.missing(), field.notNumeric());
        if (value.signum() < 0) {
            throw new InvalidValueException(field.outOfRange());
        }
        return value;
    }

    /** Recusa zero e negativo. Usado por horas trabalhadas e valor de venda ou taxa. */
    public static BigDecimal positive(String raw, MonetaryField field) throws InvalidValueException {
        BigDecimal value = Parsers.decimal(raw, field.missing(), field.notNumeric());
        if (value.signum() <= 0) {
            throw new InvalidValueException(field.outOfRange());
        }
        return value;
    }

    /**
     * Período dos comandos de consulta, já validado.
     *
     * @return intervalo [from, toExclusive): a data final informada não entra na soma,
     *         que é como os comandos de consulta do enunciado se comportam.
     */
    public static Period period(String start, String end) throws InvalidValueException {
        LocalDate from = Parsers.date(start, "Data inicial invalida.");
        LocalDate toExclusive = Parsers.date(end, "Data final invalida.");
        if (from.isAfter(toExclusive)) {
            throw new InvalidValueException("Data inicial nao pode ser posterior aa data final.");
        }
        return new Period(from, toExclusive);
    }

    /** Intervalo semiaberto [from, toExclusive) usado pelas consultas. */
    public record Period(LocalDate from, LocalDate toExclusive) {
    }
}