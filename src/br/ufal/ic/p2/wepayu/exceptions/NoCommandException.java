package br.ufal.ic.p2.wepayu.exceptions;

/** Não há comando a desfazer (undo) ou a refazer (redo). */
public class NoCommandException extends WePayUException {
    private NoCommandException(String message) {
        super(message);
    }

    public static NoCommandException toUndo() {
        return new NoCommandException("Nao ha comando a desfazer.");
    }

    public static NoCommandException toRedo() {
        return new NoCommandException("Nao ha comando a refazer.");
    }
}