package com.movies.backend.progress.service;

import com.movies.backend.exception.ApiException;
import com.movies.backend.progress.dto.UpdateProgressRequest;
import com.movies.backend.progress.entity.WatchProgress;
import com.movies.backend.progress.repository.ProgressRepository;
import com.movies.backend.progress.response.ProgressResponse;
import com.movies.backend.user.entity.User;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Regras do "continue assistindo": upsert por (userId, contentKey), listar e apagar. */
@Service
public class ProgressService {

    private final ProgressRepository progressRepository;

    public ProgressService(ProgressRepository progressRepository) {
        this.progressRepository = progressRepository;
    }

    /** Cria ou atualiza o progresso do usuário para o conteúdo informado. */
    @Transactional
    public ProgressResponse upsert(User me, UpdateProgressRequest req) {
        String contentKey = buildContentKey(req.mediaType(), req.mediaId(),
                req.seasonNumber(), req.episodeNumber());

        WatchProgress progress = progressRepository
                .findByUserIdAndContentKey(me.getId(), contentKey)
                .orElseGet(() -> {
                    WatchProgress p = new WatchProgress();
                    p.setUserId(me.getId());
                    p.setContentKey(contentKey);
                    return p;
                });

        progress.setMediaType(req.mediaType());
        progress.setMediaId(req.mediaId());
        progress.setContentKind(req.contentKind());
        progress.setTitle(req.title());
        progress.setPosterUrl(req.posterUrl());
        progress.setSeasonNumber(req.seasonNumber());
        progress.setEpisodeNumber(req.episodeNumber());
        progress.setProgressSec(req.progressSec());
        progress.setDurationSec(req.durationSec());
        progress.setUpdatedAt(Instant.now());

        return ProgressResponse.from(progressRepository.save(progress));
    }

    /** Lista os 30 conteúdos mais recentes do usuário. */
    @Transactional(readOnly = true)
    public List<ProgressResponse> list(User me) {
        return progressRepository.findTop30ByUserIdOrderByUpdatedAtDesc(me.getId())
                .stream()
                .map(ProgressResponse::from)
                .toList();
    }

    /** Apaga um item de progresso do próprio usuário. */
    @Transactional
    public void delete(User me, Long id) {
        WatchProgress progress = progressRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Item não encontrado"));
        if (!progress.getUserId().equals(me.getId())) {
            throw ApiException.forbidden("Esse item não é seu");
        }
        progressRepository.delete(progress);
    }

    /** Monta a chave única do conteúdo (0 quando temporada/episódio for nulo). */
    private String buildContentKey(String mediaType, Long mediaId, Integer season, Integer episode) {
        int s = season != null ? season : 0;
        int e = episode != null ? episode : 0;
        return mediaType + ":" + mediaId + ":" + s + ":" + e;
    }
}
