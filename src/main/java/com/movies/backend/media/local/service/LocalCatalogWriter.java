package com.movies.backend.media.local.service;

import com.movies.backend.media.gocine.dto.GocineTypes.MediaDetail;
import com.movies.backend.media.local.entity.LocalEpisode;
import com.movies.backend.media.local.entity.LocalKind;
import com.movies.backend.media.local.entity.LocalTitle;
import com.movies.backend.media.local.repository.LocalEpisodeRepository;
import com.movies.backend.media.local.repository.LocalTitleRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * Grava um título importado — título e episódios numa transação só, para uma
 * série nunca ficar pela metade no banco se o Pi cair no meio.
 */
@Service
public class LocalCatalogWriter {

    private final LocalTitleRepository titles;
    private final LocalEpisodeRepository episodes;
    private final ObjectMapper om;

    public LocalCatalogWriter(LocalTitleRepository titles, LocalEpisodeRepository episodes, ObjectMapper om) {
        this.titles = titles;
        this.episodes = episodes;
        this.om = om;
    }

    /**
     * @param streamsJson players do filme; null para série/anime
     * @param eps episódios (sem titleId ainda); null para filme
     */
    @Transactional
    public LocalTitle save(LocalKind kind, MediaDetail d, String streamsJson, List<LocalEpisode> eps) {
        LocalTitle t = titles.findByKindAndSourceId(kind, d.id()).orElseGet(LocalTitle::new);
        t.setKind(kind);
        t.setSourceId(d.id());
        t.setTitle(cut(d.title(), 255));
        t.setOverview(d.overview());
        t.setPosterUrl(cut(d.posterUrl(), 500));
        t.setBackdropUrl(cut(d.backdropUrl(), 500));
        t.setReleaseDate(cut(d.releaseDate(), 255));
        t.setGenre(cut(d.genre(), 255));
        t.setGenres(cut(d.genres() == null ? null : String.join(",", d.genres()), 500));
        t.setRuntimeMinutes(d.runtimeMinutes());
        t.setTrailerUrl(cut(d.trailerUrl(), 500));
        t.setCastJson(om.writeValueAsString(d.cast() == null ? List.of() : d.cast()));
        t.setStreamsJson(streamsJson);
        // importedAt fica o da PRIMEIRA importação: é o que ordena "lançados recentemente"
        titles.save(t);

        if (eps != null) {
            episodes.deleteByTitleId(t.getId());
            for (LocalEpisode e : eps) e.setTitleId(t.getId());
            episodes.saveAll(eps);
        }
        return t;
    }

    static String cut(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max);
    }
}
