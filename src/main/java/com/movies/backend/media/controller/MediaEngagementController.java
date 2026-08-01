package com.movies.backend.media.controller;

import com.movies.backend.media.dto.CommentRequest;
import com.movies.backend.media.dto.ReactionRequest;
import com.movies.backend.media.response.CommentResponse;
import com.movies.backend.media.response.ReactionSummaryResponse;
import com.movies.backend.media.service.MediaEngagementService;
import com.movies.backend.security.CurrentUser;
import com.movies.backend.user.entity.User;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Reações e comentários sobre uma mídia. */
@RestController
@RequestMapping("/api/media/{mediaType}/{mediaId}")
public class MediaEngagementController {

    private final MediaEngagementService engagementService;
    private final CurrentUser currentUser;

    public MediaEngagementController(MediaEngagementService engagementService, CurrentUser currentUser) {
        this.engagementService = engagementService;
        this.currentUser = currentUser;
    }

    /** GET .../reactions?season=&episode= -> {counts, myReaction, total} (season/episode 0 = geral). */
    @GetMapping("/reactions")
    public ReactionSummaryResponse reactions(@PathVariable String mediaType,
                                             @PathVariable Long mediaId,
                                             @RequestParam(defaultValue = "0") int season,
                                             @RequestParam(defaultValue = "0") int episode,
                                             Authentication auth) {
        User me = currentUser.require(auth);
        return engagementService.reactionSummary(me, mediaType, mediaId, season, episode);
    }

    /** POST .../reactions?season=&episode= {emoji} -> alterna/substitui e devolve o resumo. */
    @PostMapping("/reactions")
    public ReactionSummaryResponse react(@PathVariable String mediaType,
                                         @PathVariable Long mediaId,
                                         @RequestParam(defaultValue = "0") int season,
                                         @RequestParam(defaultValue = "0") int episode,
                                         @Valid @RequestBody ReactionRequest body,
                                         Authentication auth) {
        User me = currentUser.require(auth);
        return engagementService.react(me, mediaType, mediaId, season, episode, body);
    }

    /** GET .../comments?season=&episode= -> comentários (mais novos primeiro) com autor. */
    @GetMapping("/comments")
    public List<CommentResponse> comments(@PathVariable String mediaType,
                                          @PathVariable Long mediaId,
                                          @RequestParam(defaultValue = "0") int season,
                                          @RequestParam(defaultValue = "0") int episode) {
        return engagementService.listComments(mediaType, mediaId, season, episode);
    }

    /** POST .../comments?season=&episode= {text} -> cria e devolve o comentário com autor. */
    @PostMapping("/comments")
    public CommentResponse addComment(@PathVariable String mediaType,
                                      @PathVariable Long mediaId,
                                      @RequestParam(defaultValue = "0") int season,
                                      @RequestParam(defaultValue = "0") int episode,
                                      @Valid @RequestBody CommentRequest body,
                                      Authentication auth) {
        User me = currentUser.require(auth);
        return engagementService.addComment(me, mediaType, mediaId, season, episode, body);
    }
}
