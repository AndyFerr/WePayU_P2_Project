package br.ufal.ic.p2.wepayu.exceptions;

/** Comando recebido depois de encerrarSistema. */
public class SystemClosedException extends WePayUException {
    public SystemClosedException() {
        super("Nao pode dar comandos depois de encerrarSistema.");
    }
}