package com.movies.backend.media.response;

import java.util.Map;

/** Resumo das reações de uma mídia. */
public record ReactionSummaryResponse(
        Map<String, Long> counts,
        String myReaction,
        long total
) {
}
