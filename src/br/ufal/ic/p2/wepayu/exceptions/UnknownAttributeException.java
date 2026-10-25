package br.ufal.ic.p2.wepayu.exceptions;

/** O atributo informado em getAtributoEmpregado/alteraEmpregado não existe. */
public class UnknownAttributeException extends WePayUException {
    public UnknownAttributeException() {
        super("Atributo nao existe.");
    }
}