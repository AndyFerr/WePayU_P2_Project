package br.ufal.ic.p2.wepayu.exceptions;

/** Falha ao gravar o arquivo de saída da folha de pagamento (US 7). */
public class ReportWriteException extends WePayUException {
    public ReportWriteException(String file, Throwable cause) {
        super("Nao foi possivel gravar a folha em '" + file + "'.", cause);
    }
}