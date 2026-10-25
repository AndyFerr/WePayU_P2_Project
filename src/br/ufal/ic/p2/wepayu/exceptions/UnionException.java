package br.ufal.ic.p2.wepayu.exceptions;

/**
 * Erro ligado à filiação sindical enquanto tal: a identificação de membro informada não
 * pertence a ninguém, ou já está em uso por outro empregado. O id do sindicato é
 * independente do id da folha, então ele tem suas próprias regras de unicidade.
 */
public class UnionException extends WePayUException {
    public UnionException(String message) {
        super(message);
    }

    public static UnionException unknownMember() {
        return new UnionException("Membro nao existe.");
    }

    public static UnionException duplicatedMemberId() {
        return new UnionException("Ha outro empregado com esta identificacao de sindicato");
    }
}