package com.movies.backend.upload.controller;

import com.movies.backend.exception.ApiException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Upload de imagens em disco local (sem serviço externo). Salva em
 * ./data/uploads e devolve a URL absoluta para consumo pelo front (via <img>).
 */
@RestController
@RequestMapping("/api/uploads")
public class UploadController {

    /** Tamanho máximo aceito (~5MB). */
    private static final long MAX_SIZE = 5L * 1024 * 1024;

    /** Tipos de imagem permitidos -> extensão usada no arquivo salvo. */
    private static final Map<String, String> ALLOWED = Map.of(
            "image/png", ".png",
            "image/jpeg", ".jpg",
            "image/webp", ".webp",
            "image/gif", ".gif");

    private static final Set<String> ALLOWED_TYPES = ALLOWED.keySet();

    private final Path uploadDir = Paths.get("./data/uploads");

    /** POST /api/uploads (multipart "file") -> {url}. */
    @PostMapping
    public Map<String, String> upload(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("Envie um arquivo");
        }
        if (file.getSize() > MAX_SIZE) {
            throw ApiException.badRequest("Imagem muito grande (máx. 5MB)");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw ApiException.badRequest("Formato inválido (use PNG, JPEG, WEBP ou GIF)");
        }

        String filename = UUID.randomUUID() + ALLOWED.get(contentType);
        try {
            Files.createDirectories(uploadDir);
            file.transferTo(uploadDir.resolve(filename).toAbsolutePath());
        } catch (IOException e) {
            throw ApiException.badRequest("Não foi possível salvar a imagem");
        }

        String url = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/uploads/" + filename)
                .toUriString();
        return Map.of("url", url);
    }
}
