package br.ufal.ic.p2.wepayu.exceptions;

/**
 * Erro de regra de negócio. A mensagem é exatamente o texto que os testes de
 * aceitação (expectError) comparam, por isso nunca deve ser alterada "por estética".
 */
public class WePayUException extends Exception {
    public WePayUException(String message) {
        super(message);
    }
}
