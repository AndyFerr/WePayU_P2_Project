package br.ufal.ic.p2.wepayu.utils;

import java.math.BigDecimal;

/**
 * Guardas de invariante dos objetos de domínio.
 *
 * <p>São diferentes da validação de entrada feita nos serviços. Lá, um valor recusado é um
 * erro do usuário, e vira {@code InvalidValueException} com a mensagem que o enunciado define.
 * Aqui, um valor recusado é um erro de programação: significa que alguém construiu um objeto
 * inválido apesar de a entrada já ter sido validada. Por isso estas guardas lançam
 * {@link IllegalArgumentException}, que não se declara nem se captura — ela aponta um bug.
 *
 * <p>O ganho é que um empregado, um cartão de ponto ou uma agenda nunca existem em estado
 * inválido, mesmo que sejam criados de fora dos serviços.
 */
public final class Require {

    private Require() {
    }

    public static String text(String value, String what) {
        if (Parsers.isBlank(value)) {
            throw new IllegalArgumentException(what + " nao pode ser vazio");
        }
        return value;
    }

    public static <T> T present(T value, String what) {
        if (value == null) {
            throw new IllegalArgumentException(what + " nao pode ser nulo");
        }
        return value;
    }

    public static BigDecimal nonNegative(BigDecimal value, String what) {
        present(value, what);
        if (value.signum() < 0) {
            throw new IllegalArgumentException(what + " nao pode ser negativo: " + value);
        }
        return value;
    }

    public static BigDecimal positive(BigDecimal value, String what) {
        present(value, what);
        if (value.signum() <= 0) {
            throw new IllegalArgumentException(what + " deve ser positivo: " + value);
        }
        return value;
    }

    public static int inRange(int value, int min, int max, String what) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(what + " deve estar entre " + min + " e " + max + ": " + value);
        }
        return value;
    }
}