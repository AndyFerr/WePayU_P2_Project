package br.ufal.ic.p2.wepayu.persistence;

import br.ufal.ic.p2.wepayu.exceptions.StatePersistenceException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Guarda/recupera o estado do sistema em um arquivo. Trabalha só com bytes,
 * então trocar o formato (ex.: XMLEncoder/XMLDecoder, como sugere o enunciado)
 * significa mudar apenas o snapshot/restore do repositório.
 *
 * <p>Falha de leitura e ausência do arquivo são respostas diferentes: um arquivo que não
 * existe devolve {@link Optional#empty()} (sistema novo), enquanto um arquivo que existe mas
 * não pode ser lido vira {@link StatePersistenceException}, para que o problema não passe
 * em silêncio.
 */
public class FileStateStore {
    private final Path file;

    public FileStateStore(Path file) {
        this.file = file;
    }

    /** Nome do arquivo, para compor mensagens de erro. */
    public String describe() {
        return file.toString();
    }

    public void save(byte[] data) throws StatePersistenceException {
        try {
            Files.write(file, data);
        } catch (IOException e) {
            throw StatePersistenceException.unwritable(describe(), e);
        }
    }

    /** @return vazio quando o arquivo não existe; nunca vazio por causa de erro de leitura. */
    public Optional<byte[]> load() throws StatePersistenceException {
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Files.readAllBytes(file));
        } catch (IOException e) {
            throw StatePersistenceException.unreadable(describe(), e);
        }
    }

    public void delete() throws StatePersistenceException {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw StatePersistenceException.unwritable(describe(), e);
        }
    }
}