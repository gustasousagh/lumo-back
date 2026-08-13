package com.movies.backend.library.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * Nova ordem da playlist: a lista COMPLETA dos ids de item, na ordem desejada.
 *
 * <p>Mandar a lista inteira em vez de "moveu do índice 3 para o 7" é o que faz o
 * arrastar-e-soltar funcionar em playlist colaborativa: se duas pessoas mexem ao
 * mesmo tempo, a última gravação vence de forma previsível, em vez de aplicar um
 * movimento relativo sobre uma ordem que já mudou.
 */
public record ReorderRequest(
        @NotEmpty(message = "Mande a ordem dos itens")
        List<Long> itemIds
) {
}
