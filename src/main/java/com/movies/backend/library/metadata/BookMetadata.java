package com.movies.backend.library.metadata;

/**
 * Metadados lidos de dentro de um PDF. Como no áudio, tudo é opcional: muito
 * PDF vem sem título ou com o título errado (o nome do programa que gerou, por
 * exemplo), e o admin corrige depois no painel.
 *
 * @param coverImage a primeira página renderizada como JPEG, usada de capa
 */
public record BookMetadata(
        String title,
        String author,
        String publisher,
        String subject,
        String keywords,
        Integer year,
        Integer pageCount,
        byte[] coverImage,
        String coverMime
) {
    public static BookMetadata empty() {
        return new BookMetadata(null, null, null, null, null, null, null, null, null);
    }
}
