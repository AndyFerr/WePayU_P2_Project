package br.ufal.ic.p2.wepayu.services;

import br.ufal.ic.p2.wepayu.exceptions.StatePersistenceException;
import br.ufal.ic.p2.wepayu.exceptions.SystemClosedException;
import br.ufal.ic.p2.wepayu.exceptions.WePayUException;
import br.ufal.ic.p2.wepayu.persistence.EmployeeRepository;
import br.ufal.ic.p2.wepayu.persistence.FileStateStore;
import br.ufal.ic.p2.wepayu.persistence.TransactionManager;

import java.util.Optional;

/**
 * Ciclo de vida do sistema: carregar e gravar os dados, zerar, encerrar, desfazer e refazer.
 *
 * <p>Concentra também a regra "não aceita comandos depois de encerrarSistema". Antes ela
 * morava na fachada e se repetia em cada comando; aqui vira uma regra de negócio com dono,
 * e a fachada apenas encaminha através de {@link #run(Command)}.
 */
public class SystemService {

    @FunctionalInterface
    public interface Command<T> {
        T run() throws WePayUException;
    }

    @FunctionalInterface
    public interface VoidCommand {
        void run() throws WePayUException;
    }

    private final EmployeeRepository repository;
    private final TransactionManager transactions;
    private final FileStateStore store;

    private boolean closed;

    /**
     * Falha ocorrida ao carregar o arquivo de dados, se houve. É relatada no primeiro comando
     * em vez de ser engolida: um sistema que aparece vazio porque o arquivo não pôde ser lido
     * é indistinguível, para quem usa, de um sistema que perdeu os empregados.
     */
    private StatePersistenceException loadFailure;

    public SystemService(EmployeeRepository repository, TransactionManager transactions, FileStateStore store) {
        this.repository = repository;
        this.transactions = transactions;
        this.store = store;
        load();
    }

    private void load() {
        try {
            Optional<byte[]> saved = store.load();
            if (saved.isPresent()) {
                repository.restore(saved.get());
            }
        } catch (StatePersistenceException e) {
            loadFailure = e;
            repository.clear();
        } catch (RuntimeException e) {
            // arquivo existe mas o conteúdo não é um estado válido (corrompido, truncado,
            // ou gravado por outra versão do sistema)
            loadFailure = StatePersistenceException.unreadable(store.describe(), e);
            repository.clear();
        }
    }

    /** Executa um comando do usuário, recusando-o se o sistema já foi encerrado. */
    public <T> T run(Command<T> command) throws WePayUException {
        requireActive();
        return command.run();
    }

    public void run(VoidCommand command) throws WePayUException {
        requireActive();
        command.run();
    }

    private void requireActive() throws WePayUException {
        if (closed) {
            throw new SystemClosedException();
        }
        reportPendingLoadFailure();
    }

    /** Relata, uma vez só, a falha de carregamento ocorrida na inicialização. */
    private void reportPendingLoadFailure() throws StatePersistenceException {
        if (loadFailure != null) {
            StatePersistenceException failure = loadFailure;
            loadFailure = null;
            throw failure;
        }
    }

    /** Também é desfazível: us8.txt faz "undo de zerarSistema" e espera os empregados de volta. */
    public void reset() throws WePayUException {
        closed = false;
        loadFailure = null; // o usuário pediu explicitamente para começar do zero
        transactions.executeVoid(repository::clear);
    }

    public void close() throws WePayUException {
        reportPendingLoadFailure();
        store.save(repository.snapshot());
        closed = true;
    }

    public void undo() throws WePayUException {
        run((VoidCommand) transactions::undo);
    }

    public void redo() throws WePayUException {
        run((VoidCommand) transactions::redo);
    }
}