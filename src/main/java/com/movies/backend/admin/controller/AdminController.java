package com.movies.backend.admin.controller;

import com.movies.backend.admin.dto.SuspendUserRequest;
import com.movies.backend.admin.dto.UpdateUserRoleRequest;
import com.movies.backend.admin.response.AdminOverviewResponse;
import com.movies.backend.admin.response.AdminUserResponse;
import com.movies.backend.admin.response.PageResponse;
import com.movies.backend.admin.service.AdminService;
import com.movies.backend.library.dto.UpdateBookRequest;
import com.movies.backend.library.dto.UpdateTrackRequest;
import com.movies.backend.library.response.BookResponse;
import com.movies.backend.library.response.TrackResponse;
import com.movies.backend.library.response.UploadResultResponse;
import com.movies.backend.library.service.LibraryDownloadService;
import com.movies.backend.library.service.LibraryService;
import com.movies.backend.library.service.LibraryUploadService;
import com.movies.backend.security.CurrentUser;
import com.movies.backend.user.entity.User;
import com.movies.backend.user.response.MessageResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/**
 * Painel administrativo.
 *
 * <p>{@code @PreAuthorize} na classe inteira, além da regra de URL no
 * SecurityConfig: as duas dizem a mesma coisa de propósito. Se alguém mexer no
 * mapeamento de rotas e o /api/admin/** deixar de casar, os métodos continuam
 * fechados.
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final LibraryUploadService uploadService;
    private final LibraryService libraryService;
    private final LibraryDownloadService downloadService;
    private final CurrentUser currentUser;

    public AdminController(AdminService adminService,
                           LibraryUploadService uploadService,
                           LibraryService libraryService,
                           LibraryDownloadService downloadService,
                           CurrentUser currentUser) {
        this.adminService = adminService;
        this.uploadService = uploadService;
        this.libraryService = libraryService;
        this.downloadService = downloadService;
        this.currentUser = currentUser;
    }

    // -------------------------------------------------------------- DASHBOARD
    /** GET /api/admin/overview -> números, séries e rankings do painel. */
    @GetMapping("/overview")
    public AdminOverviewResponse overview() {
        return adminService.overview();
    }

    // --------------------------------------------------------------- USUÁRIOS
    /** GET /api/admin/users?q=&page=&size= */
    @GetMapping("/users")
    public PageResponse<AdminUserResponse> users(@RequestParam(value = "q", required = false) String q,
                                                 @RequestParam(value = "page", defaultValue = "0") int page,
                                                 @RequestParam(value = "size", defaultValue = "20") int size) {
        return adminService.users(q, page, size);
    }

    /** PATCH /api/admin/users/{id}/role {role} -> promove ou rebaixa. */
    @PatchMapping("/users/{id}/role")
    public AdminUserResponse changeRole(@PathVariable Long id,
                                        @Valid @RequestBody UpdateUserRoleRequest body,
                                        Authentication auth) {
        User me = currentUser.require(auth);
        return adminService.changeRole(me, id, body.role());
    }

    /** PATCH /api/admin/users/{id}/suspended {suspended} -> corta ou devolve o acesso. */
    @PatchMapping("/users/{id}/suspended")
    public AdminUserResponse setSuspended(@PathVariable Long id,
                                          @RequestBody SuspendUserRequest body,
                                          Authentication auth) {
        User me = currentUser.require(auth);
        return adminService.setSuspended(me, id, body.suspended());
    }

    // ----------------------------------------------------------------- UPLOAD
    /** POST /api/admin/library/tracks (multipart "files") -> sobe músicas. */
    @PostMapping("/library/tracks")
    public UploadResultResponse uploadTracks(@RequestParam("files") List<MultipartFile> files,
                                             Authentication auth) {
        return uploadService.uploadTracks(currentUser.require(auth), files);
    }

    /** POST /api/admin/library/books (multipart "files") -> sobe livros em PDF. */
    @PostMapping("/library/books")
    public UploadResultResponse uploadBooks(@RequestParam("files") List<MultipartFile> files,
                                            Authentication auth) {
        return uploadService.uploadBooks(currentUser.require(auth), files);
    }

    // ------------------------------------------------------- EDITAR / APAGAR
    @PatchMapping("/library/tracks/{id}")
    public TrackResponse updateTrack(@PathVariable Long id, @Valid @RequestBody UpdateTrackRequest body) {
        return libraryService.updateTrack(id, body);
    }

    @DeleteMapping("/library/tracks/{id}")
    public MessageResponse deleteTrack(@PathVariable Long id) {
        libraryService.deleteTrack(id);
        return new MessageResponse("Faixa removida da biblioteca");
    }

    @PatchMapping("/library/books/{id}")
    public BookResponse updateBook(@PathVariable Long id, @Valid @RequestBody UpdateBookRequest body) {
        return libraryService.updateBook(id, body);
    }

    @DeleteMapping("/library/books/{id}")
    public MessageResponse deleteBook(@PathVariable Long id) {
        libraryService.deleteBook(id);
        return new MessageResponse("Livro removido da biblioteca");
    }

    // ---------------------------------------------------------------- EXPORT
    /** GET /api/admin/export/everything -> o acervo inteiro num ZIP. */
    @GetMapping("/export/everything")
    public ResponseEntity<StreamingResponseBody> exportEverything() {
        return LibraryDownloadService.asAttachment(downloadService.everythingZip(), "lumo-acervo-completo.zip");
    }

    /** GET /api/admin/export/music -> só as músicas. */
    @GetMapping("/export/music")
    public ResponseEntity<StreamingResponseBody> exportMusic() {
        return LibraryDownloadService.asAttachment(downloadService.tracksZip(List.of()), "lumo-musicas.zip");
    }

    /** GET /api/admin/export/books -> só os livros. */
    @GetMapping("/export/books")
    public ResponseEntity<StreamingResponseBody> exportBooks() {
        return LibraryDownloadService.asAttachment(downloadService.booksZip(List.of()), "lumo-livros.zip");
    }
}
