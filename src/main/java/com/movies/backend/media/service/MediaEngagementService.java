package com.movies.backend.media.service;

import com.movies.backend.media.dto.CommentRequest;
import com.movies.backend.media.dto.ReactionRequest;
import com.movies.backend.media.entity.MediaComment;
import com.movies.backend.media.entity.MediaReaction;
import com.movies.backend.media.repository.MediaCommentRepository;
import com.movies.backend.media.repository.MediaReactionRepository;
import com.movies.backend.media.response.CommentResponse;
import com.movies.backend.media.response.ReactionSummaryResponse;
import com.movies.backend.user.entity.User;
import com.movies.backend.user.repository.UserRepository;
import com.movies.backend.user.response.UserMiniResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reações (emoji) e comentários sobre mídias. */
@Service
public class MediaEngagementService {

    private final MediaReactionRepository reactionRepository;
    private final MediaCommentRepository commentRepository;
    private final UserRepository userRepository;

    public MediaEngagementService(MediaReactionRepository reactionRepository,
                                  MediaCommentRepository commentRepository,
                                  UserRepository userRepository) {
        this.reactionRepository = reactionRepository;
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
    }

    // -------------------------------------------------------------- REAÇÕES
    // season/episode = 0 significa "título/geral"; > 0 escopa por episódio.
    @Transactional(readOnly = true)
    public ReactionSummaryResponse reactionSummary(User me, String mediaType, Long mediaId, int season, int episode) {
        List<MediaReaction> all = reactionRepository
                .findByMediaTypeAndMediaIdAndSeasonNumberAndEpisodeNumber(mediaType, mediaId, season, episode);
        Map<String, Long> counts = new LinkedHashMap<>();
        String mine = null;
        for (MediaReaction r : all) {
            counts.merge(r.getEmoji(), 1L, Long::sum);
            if (me != null && r.getUserId().equals(me.getId())) {
                mine = r.getEmoji();
            }
        }
        return new ReactionSummaryResponse(counts, mine, all.size());
    }

    /** Alterna/substitui a reação do usuário: mesmo emoji remove; outro substitui. */
    @Transactional
    public ReactionSummaryResponse react(User me, String mediaType, Long mediaId, int season, int episode, ReactionRequest body) {
        MediaReaction existing = reactionRepository
                .findByUserIdAndMediaTypeAndMediaIdAndSeasonNumberAndEpisodeNumber(me.getId(), mediaType, mediaId, season, episode)
                .orElse(null);

        if (existing != null && existing.getEmoji().equals(body.emoji())) {
            reactionRepository.delete(existing); // mesmo emoji -> remove (toggle)
        } else if (existing != null) {
            existing.setEmoji(body.emoji()); // troca de emoji
            reactionRepository.save(existing);
        } else {
            MediaReaction reaction = new MediaReaction();
            reaction.setUserId(me.getId());
            reaction.setMediaType(mediaType);
            reaction.setMediaId(mediaId);
            reaction.setSeasonNumber(season);
            reaction.setEpisodeNumber(episode);
            reaction.setEmoji(body.emoji());
            reactionRepository.save(reaction);
        }
        return reactionSummary(me, mediaType, mediaId, season, episode);
    }

    // ----------------------------------------------------------- COMENTÁRIOS
    @Transactional(readOnly = true)
    public List<CommentResponse> listComments(String mediaType, Long mediaId, int season, int episode) {
        return commentRepository
                .findByMediaTypeAndMediaIdAndSeasonNumberAndEpisodeNumberOrderByCreatedAtDesc(mediaType, mediaId, season, episode)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public CommentResponse addComment(User me, String mediaType, Long mediaId, int season, int episode, CommentRequest body) {
        MediaComment comment = new MediaComment();
        comment.setUserId(me.getId());
        comment.setMediaType(mediaType);
        comment.setMediaId(mediaId);
        comment.setSeasonNumber(season);
        comment.setEpisodeNumber(episode);
        comment.setText(body.text().trim());
        MediaComment saved = commentRepository.save(comment);
        return toResponse(saved);
    }

    private CommentResponse toResponse(MediaComment comment) {
        UserMiniResponse author = userRepository.findById(comment.getUserId())
                .map(UserMiniResponse::from)
                .orElse(null);
        return new CommentResponse(comment.getId(), comment.getText(), author, comment.getCreatedAt());
    }
}
