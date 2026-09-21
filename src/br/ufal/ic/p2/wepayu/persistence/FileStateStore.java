package br.ufal.ic.p2.wepayu.persistence;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Guarda/recupera o estado do sistema em um arquivo. Trabalha só com bytes,
 * então trocar o formato (ex.: XMLEncoder/XMLDecoder, como sugere o enunciado)
 * significa mudar apenas o snapshot/restore do repositório.
 */
public class FileStateStore {
    private final Path file;

    public FileStateStore(Path file) {
        this.file = file;
    }

    public void save(byte[] data) throws IOException {
        Files.write(file, data);
    }

    public Optional<byte[]> load() throws IOException {
        return Files.exists(file) ? Optional.of(Files.readAllBytes(file)) : Optional.empty();
    }

    public void delete() throws IOException {
        Files.deleteIfExists(file);
    }
}
