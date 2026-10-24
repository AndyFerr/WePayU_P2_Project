package br.ufal.ic.p2.wepayu.exceptions;

/**
 * A descrição da agenda não existe, ou não foi identificado: id nulo/vazio, id que não está
 * no sistema, ou busca por nome que não encontrou ninguém naquela posição.
 */

public class ScheduleException extends RuntimeException {
    public ScheduleException(String message) {

        super(message);
    }

    public static ScheduleException invalidDescription(){
        return new ScheduleException("Descricao de agenda invalida");
    }
}
