package com.movies.backend.library.response;

import java.util.List;

/**
 * Resultado de um upload em lote. Cada arquivo tem seu próprio destino: mandar
 * 40 MP3 de uma pasta e ter os 40 recusados porque um estava corrompido seria
 * péssimo, então um arquivo que falha não derruba os outros.
 *
 * @param status "created" (entrou), "duplicate" (já existia, mesmo conteúdo) ou
 *               "error" (não deu — o motivo vai em {@code message})
 */
public record UploadResultResponse(
        List<Item> items,
        int created,
        int duplicates,
        int failed
) {
    public record Item(
            String filename,
            String status,
            String message,
            TrackResponse track,
            BookResponse book
    ) {
        public static Item created(String filename, TrackResponse track) {
            return new Item(filename, "created", null, track, null);
        }

        public static Item created(String filename, BookResponse book) {
            return new Item(filename, "created", null, null, book);
        }

        public static Item duplicate(String filename, TrackResponse track) {
            return new Item(filename, "duplicate", "Esse arquivo já está na biblioteca", track, null);
        }

        public static Item duplicate(String filename, BookResponse book) {
            return new Item(filename, "duplicate", "Esse arquivo já está na biblioteca", null, book);
        }

        public static Item error(String filename, String message) {
            return new Item(filename, "error", message, null, null);
        }
    }

    public static UploadResultResponse of(List<Item> items) {
        int created = (int) items.stream().filter(i -> "created".equals(i.status())).count();
        int duplicates = (int) items.stream().filter(i -> "duplicate".equals(i.status())).count();
        int failed = (int) items.stream().filter(i -> "error".equals(i.status())).count();
        return new UploadResultResponse(items, created, duplicates, failed);
    }
}
