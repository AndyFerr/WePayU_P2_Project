package br.ufal.ic.p2.wepayu.exceptions;

/**
 * Erro de regra de negócio. A mensagem é exatamente o texto que os testes de
 * aceitação (expectError) comparam, por isso nunca deve ser alterada "por estética".
 */
public abstract class WePayUException extends Exception {
    protected WePayUException(String message) {
        super(message);
    }

    protected WePayUException(String message, Throwable cause) {   // ← este
        super(message, cause);
    }
}
