package com.movies.backend.admin.response;

import java.util.List;
import org.springframework.data.domain.Page;
import java.util.function.Function;

/**
 * Página enxuta. O {@code Page} do Spring serializa um JSON enorme e instável
 * entre versões; aqui o front recebe só o que usa para desenhar a paginação.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static <E, T> PageResponse<T> of(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
