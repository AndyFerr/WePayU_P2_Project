package br.ufal.ic.p2.wepayu.exceptions;

/**
 * O empregado existe, mas a operação não cabe no que ele é hoje: pedir cartão de ponto a
 * quem não é horista, venda a quem não é comissionado, dados do sindicato a quem não é
 * sindicalizado, ou dados bancários a quem não recebe em banco.
 */
public class EmployeeTypeMismatchException extends WePayUException {
    public EmployeeTypeMismatchException(String message) {
        super(message);
    }

    public static EmployeeTypeMismatchException notHourly() {
        return new EmployeeTypeMismatchException("Empregado nao eh horista.");
    }

    public static EmployeeTypeMismatchException notCommissioned() {
        return new EmployeeTypeMismatchException("Empregado nao eh comissionado.");
    }

    public static EmployeeTypeMismatchException notUnionMember() {
        return new EmployeeTypeMismatchException("Empregado nao eh sindicalizado.");
    }

    public static EmployeeTypeMismatchException notPaidByBank() {
        return new EmployeeTypeMismatchException("Empregado nao recebe em banco.");
    }
}