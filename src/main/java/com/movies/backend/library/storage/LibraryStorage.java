package com.movies.backend.library.storage;

import com.movies.backend.exception.ApiException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * Guarda os arquivos da biblioteca em disco, em ./data/library:
 *
 * <pre>
 *   music/   os MP3
 *   books/   os PDF
 *   covers/  as capas extraídas (JPEG/PNG)
 * </pre>
 *
 * <p>O nome no disco é sempre um UUID, nunca o nome enviado: nome de arquivo do
 * usuário pode conter "../", barra invertida e caracteres que o sistema de
 * arquivos rejeita. O nome original fica só no banco, para o download sair
 * legível.
 *
 * <p>A gravação calcula o SHA-256 no mesmo streaming da cópia — ler o arquivo
 * duas vezes só para tirar o hash seria desperdício num MP3 de 10MB.
 */
@Component
public class LibraryStorage {

    private final Path root;

    public LibraryStorage(@Value("${app.library.dir:./data/library}") String dir) {
        this.root = Paths.get(dir).toAbsolutePath().normalize();
    }

    /** Pastas de cada tipo de arquivo. */
    public enum Bucket {
        MUSIC("music"),
        BOOKS("books"),
        COVERS("covers");

        private final String folder;

        Bucket(String folder) {
            this.folder = folder;
        }

        public String folder() {
            return folder;
        }
    }

    /** Resultado da gravação: onde ficou, quanto ocupa e o hash do conteúdo. */
    public record Stored(String storageKey, long size, String checksum) {
    }

    public Path root() {
        return root;
    }

    public Path bucketDir(Bucket bucket) {
        return root.resolve(bucket.folder());
    }

    /** Caminho absoluto de um arquivo já guardado. */
    public Path resolve(Bucket bucket, String storageKey) {
        Path path = bucketDir(bucket).resolve(storageKey).normalize();
        // Cinto de segurança: se a chave vier adulterada com "../", o caminho
        // resolvido sai de dentro do bucket e a leitura é recusada.
        if (!path.startsWith(bucketDir(bucket))) {
            throw ApiException.badRequest("Caminho de arquivo inválido");
        }
        return path;
    }

    /** Salva um upload e devolve chave, tamanho e checksum. */
    public Stored save(MultipartFile file, Bucket bucket, String extension) {
        String key = UUID.randomUUID() + extension;
        Path target = bucketDir(bucket).resolve(key);
        try (InputStream in = file.getInputStream()) {
            Files.createDirectories(target.getParent());
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (DigestInputStream digesting = new DigestInputStream(in, digest)) {
                Files.copy(digesting, target, StandardCopyOption.REPLACE_EXISTING);
            }
            long size = Files.size(target);
            return new Stored(key, size, HexFormat.of().formatHex(digest.digest()));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível nesta JVM", e);
        } catch (IOException e) {
            throw ApiException.badRequest("Não foi possível salvar o arquivo: " + e.getMessage());
        }
    }

    /** Salva bytes já em memória (usado para as capas extraídas). */
    public String saveBytes(byte[] data, Bucket bucket, String extension) {
        String key = UUID.randomUUID() + extension;
        Path target = bucketDir(bucket).resolve(key);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, data);
            return key;
        } catch (IOException e) {
            throw ApiException.badRequest("Não foi possível salvar a capa: " + e.getMessage());
        }
    }

    /** Remove um arquivo; ausência não é erro (o registro do banco é a verdade). */
    public void delete(Bucket bucket, String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(resolve(bucket, storageKey));
        } catch (IOException | ApiException ignored) {
            // arquivo já sumiu ou chave inválida: apagar do banco é o que importa
        }
    }

    /** Espaço livre no disco onde a biblioteca mora (para o painel admin). */
    public long usableSpaceBytes() {
        try {
            Files.createDirectories(root);
            return Files.getFileStore(root).getUsableSpace();
        } catch (IOException e) {
            return -1;
        }
    }
}
