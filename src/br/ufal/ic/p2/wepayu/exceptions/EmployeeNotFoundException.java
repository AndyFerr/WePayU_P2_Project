package br.ufal.ic.p2.wepayu.exceptions;

/**
 * O empregado pedido não existe, ou não foi identificado: id nulo/vazio, id que não está
 * no sistema, ou busca por nome que não encontrou ninguém naquela posição.
 */
public class EmployeeNotFoundException extends WePayUException {
    public EmployeeNotFoundException(String message) {
        super(message);
    }

    public static EmployeeNotFoundException missingId() {
        return new EmployeeNotFoundException("Identificacao do empregado nao pode ser nula.");
    }

    public static EmployeeNotFoundException unknownId() {
        return new EmployeeNotFoundException("Empregado nao existe.");
    }

    public static EmployeeNotFoundException unknownName() {
        return new EmployeeNotFoundException("Nao ha empregado com esse nome.");
    }
}