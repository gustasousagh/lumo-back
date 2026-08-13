package com.movies.backend.library.dto;

import jakarta.validation.constraints.Size;

/** Correção manual dos metadados de um livro (painel admin). */
public record UpdateBookRequest(
        @Size(max = 300) String title,
        @Size(max = 200) String author,
        @Size(max = 200) String publisher,
        @Size(max = 100) String genre,
        @Size(max = 10) String language,
        Integer year,
        @Size(max = 20) String isbn,
        String description,
        String coverUrl
) {
}
