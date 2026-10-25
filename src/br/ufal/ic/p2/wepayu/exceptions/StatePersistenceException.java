package br.ufal.ic.p2.wepayu.exceptions;

/**
 * Falha ao gravar ou ao ler o arquivo de dados do sistema.
 *
 * <p>Existe para que esse problema não passe em silêncio. Um arquivo de estado presente mas
 * ilegível (corrompido, truncado, ou gravado por uma versão anterior do sistema) não é a
 * mesma coisa que um sistema novo sem dados: começar vazio sem avisar faria parecer que os
 * empregados simplesmente deixaram de existir.
 */
public class StatePersistenceException extends WePayUException {
    public StatePersistenceException(String message, Throwable cause) {
        super(message, cause);
    }

    public static StatePersistenceException unreadable(String file, Throwable cause) {
        return new StatePersistenceException(
                "Nao foi possivel ler o arquivo de dados '" + file
                        + "'. O sistema iniciou vazio; use zerarSistema para comecar do zero.", cause);
    }

    public static StatePersistenceException unwritable(String file, Throwable cause) {
        return new StatePersistenceException(
                "Nao foi possivel gravar o arquivo de dados '" + file + "'.", cause);
    }
}