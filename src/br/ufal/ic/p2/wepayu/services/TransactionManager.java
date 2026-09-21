package br.ufal.ic.p2.wepayu.services;

import br.ufal.ic.p2.wepayu.exceptions.WePayUException;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Executa cada operação que altera o sistema como uma transação:
 * <ul>
 *   <li>se ela falhar, o estado anterior é restaurado (nada fica pela metade);</li>
 *   <li>se ela der certo, o estado anterior vai para a pilha de undo e a pilha de redo é zerada.</li>
 * </ul>
 * Guardar snapshots do estado é a opção mais simples e robusta para um sistema deste tamanho;
 * o custo cresce com o número de empregados, o que não é problema aqui.
 */
public class TransactionManager {

    @FunctionalInterface
    public interface Action<T> {
        T run() throws WePayUException;
    }

    @FunctionalInterface
    public interface VoidAction {
        void run() throws WePayUException;
    }

    private final EmployeeRepository repository;
    private final Deque<byte[]> undoStack = new ArrayDeque<>();
    private final Deque<byte[]> redoStack = new ArrayDeque<>();

    public TransactionManager(EmployeeRepository repository) {
        this.repository = repository;
    }

    public <T> T execute(Action<T> action) throws WePayUException {
        byte[] before = repository.snapshot();
        try {
            T result = action.run();
            undoStack.push(before);
            redoStack.clear();
            return result;
        } catch (WePayUException | RuntimeException e) {
            repository.restore(before);
            throw e;
        }
    }

    public void executeVoid(VoidAction action) throws WePayUException {
        execute(() -> {
            action.run();
            return null;
        });
    }

    public void undo() throws WePayUException {
        if (undoStack.isEmpty()) {
            throw new WePayUException("Nao ha comando a desfazer.");
        }
        redoStack.push(repository.snapshot());
        repository.restore(undoStack.pop());
    }

    public void redo() throws WePayUException {
        if (redoStack.isEmpty()) {
            throw new WePayUException("Nao ha comando a refazer.");
        }
        undoStack.push(repository.snapshot());
        repository.restore(redoStack.pop());
    }
}
