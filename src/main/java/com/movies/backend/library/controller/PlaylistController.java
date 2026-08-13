package com.movies.backend.library.controller;

import com.movies.backend.exception.ApiException;
import com.movies.backend.library.dto.AddTracksRequest;
import com.movies.backend.library.dto.CollaboratorRequest;
import com.movies.backend.library.dto.CreatePlaylistRequest;
import com.movies.backend.library.dto.ReorderRequest;
import com.movies.backend.library.dto.UpdatePlaylistRequest;
import com.movies.backend.library.entity.Playlist;
import com.movies.backend.library.response.PlaylistDetailResponse;
import com.movies.backend.library.response.PlaylistSummaryResponse;
import com.movies.backend.library.service.LibraryDownloadService;
import com.movies.backend.library.service.PlaylistService;
import com.movies.backend.security.CurrentUser;
import com.movies.backend.user.entity.User;
import com.movies.backend.user.response.MessageResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/** Playlists de música: montar, reordenar, compartilhar, colaborar e baixar. */
@RestController
@RequestMapping("/api/playlists")
public class PlaylistController {

    private final PlaylistService playlistService;
    private final LibraryDownloadService downloadService;
    private final CurrentUser currentUser;

    public PlaylistController(PlaylistService playlistService,
                              LibraryDownloadService downloadService,
                              CurrentUser currentUser) {
        this.playlistService = playlistService;
        this.downloadService = downloadService;
        this.currentUser = currentUser;
    }

    /** GET /api/playlists -> as minhas. */
    @GetMapping
    public List<PlaylistSummaryResponse> mine(Authentication auth) {
        return playlistService.myPlaylists(currentUser.require(auth));
    }

    /** GET /api/playlists/shared -> aquelas em que fui convidado a colaborar. */
    @GetMapping("/shared")
    public List<PlaylistSummaryResponse> shared(Authentication auth) {
        return playlistService.sharedWithMe(currentUser.require(auth));
    }

    /** GET /api/playlists/public -> as públicas da galera. */
    @GetMapping("/public")
    public List<PlaylistSummaryResponse> publicOnes(Authentication auth) {
        return playlistService.publicPlaylists(currentUser.require(auth));
    }

    @PostMapping
    public PlaylistSummaryResponse create(@Valid @RequestBody CreatePlaylistRequest body, Authentication auth) {
        return playlistService.create(currentUser.require(auth), body);
    }

    @GetMapping("/{id}")
    public PlaylistDetailResponse detail(@PathVariable Long id, Authentication auth) {
        return playlistService.detail(currentUser.require(auth), id);
    }

    @PatchMapping("/{id}")
    public PlaylistSummaryResponse update(@PathVariable Long id,
                                          @Valid @RequestBody UpdatePlaylistRequest body,
                                          Authentication auth) {
        return playlistService.update(currentUser.require(auth), id, body);
    }

    @DeleteMapping("/{id}")
    public MessageResponse delete(@PathVariable Long id, Authentication auth) {
        playlistService.delete(currentUser.require(auth), id);
        return new MessageResponse("Playlist apagada");
    }

    // ------------------------------------------------------------------ ITENS
    @PostMapping("/{id}/tracks")
    public PlaylistDetailResponse addTracks(@PathVariable Long id,
                                            @Valid @RequestBody AddTracksRequest body,
                                            Authentication auth) {
        return playlistService.addTracks(currentUser.require(auth), id, body.trackIds());
    }

    @DeleteMapping("/{id}/items/{itemId}")
    public PlaylistDetailResponse removeItem(@PathVariable Long id,
                                             @PathVariable Long itemId,
                                             Authentication auth) {
        return playlistService.removeItem(currentUser.require(auth), id, itemId);
    }

    /** PUT /api/playlists/{id}/order {itemIds} -> nova ordem completa. */
    @PutMapping("/{id}/order")
    public PlaylistDetailResponse reorder(@PathVariable Long id,
                                          @Valid @RequestBody ReorderRequest body,
                                          Authentication auth) {
        return playlistService.reorder(currentUser.require(auth), id, body.itemIds());
    }

    // ---------------------------------------------------------- COMPARTILHAR
    /** GET /api/playlists/shared/{code} -> abre pelo link compartilhado. */
    @GetMapping("/shared/{code}")
    public PlaylistDetailResponse byCode(@PathVariable String code, Authentication auth) {
        return playlistService.byShareCode(currentUser.require(auth), code);
    }

    /** POST /api/playlists/{id}/share/rotate -> gera um link novo e mata o antigo. */
    @PostMapping("/{id}/share/rotate")
    public PlaylistSummaryResponse rotateShare(@PathVariable Long id, Authentication auth) {
        return playlistService.rotateShareCode(currentUser.require(auth), id);
    }

    @PostMapping("/{id}/collaborators")
    public PlaylistDetailResponse addCollaborator(@PathVariable Long id,
                                                  @Valid @RequestBody CollaboratorRequest body,
                                                  Authentication auth) {
        return playlistService.addCollaborator(currentUser.require(auth), id, body.username());
    }

    @DeleteMapping("/{id}/collaborators/{userId}")
    public PlaylistDetailResponse removeCollaborator(@PathVariable Long id,
                                                     @PathVariable Long userId,
                                                     Authentication auth) {
        return playlistService.removeCollaborator(currentUser.require(auth), id, userId);
    }

    // ------------------------------------------------------------------- ZIP
    /** GET /api/playlists/{id}/download -> ZIP com as faixas na ordem + .m3u. */
    @GetMapping("/{id}/download")
    public ResponseEntity<StreamingResponseBody> download(@PathVariable Long id, Authentication auth) {
        User me = currentUser.require(auth);
        Playlist playlist = playlistService.require(id);
        if (!playlistService.canView(me, playlist, false)) {
            throw ApiException.forbidden("Essa playlist é privada");
        }
        return LibraryDownloadService.asAttachment(
                downloadService.playlistZip(playlist.getId(), playlist.getTitle()),
                playlist.getTitle() + ".zip");
    }
}
