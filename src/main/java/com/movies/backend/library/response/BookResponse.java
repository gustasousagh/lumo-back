package com.movies.backend.library.response;

import com.movies.backend.library.entity.Book;

/** Livro como o front recebe. {@code lastPage} vem do progresso de quem consulta. */
public record BookResponse(
        Long id,
        String title,
        String author,
        String publisher,
        String genre,
        String language,
        Integer year,
        Integer pageCount,
        String isbn,
        String description,
        String coverUrl,
        long fileSize,
        long readCount,
        boolean favorite,
        Integer lastPage
) {
    public static BookResponse from(Book book) {
        return from(book, false, null);
    }

    public static BookResponse from(Book book, boolean favorite, Integer lastPage) {
        return new BookResponse(
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getPublisher(),
                book.getGenre(),
                book.getLanguage(),
                book.getYear(),
                book.getPageCount(),
                book.getIsbn(),
                book.getDescription(),
                book.getCoverUrl(),
                book.getFileSize(),
                book.getReadCount(),
                favorite,
                lastPage);
    }
}
