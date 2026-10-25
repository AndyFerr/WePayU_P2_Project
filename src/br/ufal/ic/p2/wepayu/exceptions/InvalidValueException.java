package br.ufal.ic.p2.wepayu.exceptions;

/**
 * Valor recusado na entrada de um comando: ausente, fora do formato esperado (não numérico,
 * data inexistente) ou fora da faixa permitida (salário negativo, horas não positivas).
 * É o erro de validação de parâmetro, e por isso o mais comum do sistema.
 */
public class InvalidValueException extends WePayUException {
    public InvalidValueException(String message) {
        super(message);
    }
}