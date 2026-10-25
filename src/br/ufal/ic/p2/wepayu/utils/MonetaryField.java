package br.ufal.ic.p2.wepayu.utils;

/**
 * Campos numéricos aceitos nos comandos, com as mensagens de erro de cada um.
 *
 * <p>As mensagens variam em gênero e em redação ("Salario ... nulo" x "Comissao ... nula"),
 * e são comparadas literalmente pelos testes de aceitação. Mantê-las como dados — uma linha
 * por campo — evita os quatro métodos de leitura quase idênticos que existiam antes.
 */
public enum MonetaryField {
    SALARY("Salario nao pode ser nulo.", "Salario deve ser numerico.", "Salario deve ser nao-negativo."),
    COMMISSION("Comissao nao pode ser nula.", "Comissao deve ser numerica.", "Comissao deve ser nao-negativa."),
    UNION_FEE("Taxa sindical nao pode ser nula.", "Taxa sindical deve ser numerica.",
            "Taxa sindical deve ser nao-negativa."),
    AMOUNT("Valor deve ser positivo.", "Valor deve ser numerico.", "Valor deve ser positivo."),
    HOURS("Horas devem ser positivas.", "Horas devem ser numericas.", "Horas devem ser positivas.");

    private final String missing;
    private final String notNumeric;
    private final String outOfRange;

    MonetaryField(String missing, String notNumeric, String outOfRange) {
        this.missing = missing;
        this.notNumeric = notNumeric;
        this.outOfRange = outOfRange;
    }

    String missing() {
        return missing;
    }

    String notNumeric() {
        return notNumeric;
    }

    String outOfRange() {
        return outOfRange;
    }
}