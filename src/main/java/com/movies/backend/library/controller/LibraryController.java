package com.movies.backend.library.controller;

import com.movies.backend.library.dto.BookProgressRequest;
import com.movies.backend.library.entity.Book;
import com.movies.backend.library.entity.LibraryFavorite;
import com.movies.backend.library.entity.Track;
import com.movies.backend.library.response.AlbumResponse;
import com.movies.backend.library.response.ArtistResponse;
import com.movies.backend.library.response.BookResponse;
import com.movies.backend.library.response.LibraryHomeResponse;
import com.movies.backend.library.response.LibrarySearchResponse;
import com.movies.backend.library.response.TrackResponse;
import com.movies.backend.library.service.LibraryDownloadService;
import com.movies.backend.library.service.LibraryService;
import com.movies.backend.library.storage.LibraryStorage;
import com.movies.backend.library.storage.RangeSupport;
import com.movies.backend.security.CurrentUser;
import com.movies.backend.user.entity.User;
import com.movies.backend.user.response.MessageResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/**
 * A biblioteca do lado de quem consome: navegar, buscar, tocar, ler e baixar.
 * Tudo aqui exige login; cadastrar e apagar conteúdo é no /api/admin.
 *
 * <p>As rotas de arquivo (/stream, /file, /download) aceitam o JWT na query
 * string além do header — veja {@code JwtAuthenticationFilter}. É o que permite
 * apontar um {@code <audio src>} direto para cá.
 */
@RestController
@RequestMapping("/api/library")
public class LibraryController {

    private final LibraryService libraryService;
    private final LibraryDownloadService downloadService;
    private final LibraryStorage storage;
    private final CurrentUser currentUser;

    public LibraryController(LibraryService libraryService,
                             LibraryDownloadService downloadService,
                             LibraryStorage storage,
                             CurrentUser currentUser) {
        this.libraryService = libraryService;
        this.downloadService = downloadService;
        this.storage = storage;
        this.currentUser = currentUser;
    }

    // -------------------------------------------------------------------- HOME
    /** GET /api/library/home -> tudo da tela inicial numa chamada. */
    @GetMapping("/home")
    public LibraryHomeResponse home(Authentication auth) {
        return libraryService.home(currentUser.require(auth));
    }

    /** GET /api/library/search?q=&limit= -> música, livro, álbum, artista e playlist. */
    @GetMapping("/search")
    public LibrarySearchResponse search(@RequestParam("q") String q,
                                        @RequestParam(value = "limit", defaultValue = "30") int limit,
                                        Authentication auth) {
        return libraryService.search(currentUser.require(auth), q, limit);
    }

    // ------------------------------------------------------------------ MÚSICA
    @GetMapping("/tracks")
    public List<TrackResponse> tracks(Authentication auth) {
        return libraryService.allTracks(currentUser.require(auth));
    }

    @GetMapping("/tracks/{id}")
    public TrackResponse track(@PathVariable Long id, Authentication auth) {
        return libraryService.track(currentUser.require(auth), id);
    }

    /** GET /api/library/tracks/{id}/stream -> o áudio, com suporte a Range. */
    @GetMapping("/tracks/{id}/stream")
    public ResponseEntity<Resource> stream(@PathVariable Long id, HttpServletRequest request) {
        Track track = libraryService.requireTrack(id);
        return RangeSupport.serve(
                request,
                storage.resolve(LibraryStorage.Bucket.MUSIC, track.getStorageKey()),
                track.getMimeType(),
                null);
    }

    /** GET /api/library/tracks/{id}/download -> o mesmo arquivo, como anexo. */
    @GetMapping("/tracks/{id}/download")
    public ResponseEntity<Resource> downloadTrack(@PathVariable Long id, HttpServletRequest request) {
        Track track = libraryService.requireTrack(id);
        return RangeSupport.serve(
                request,
                storage.resolve(LibraryStorage.Bucket.MUSIC, track.getStorageKey()),
                track.getMimeType(),
                track.getOriginalFilename());
    }

    /** POST /api/library/tracks/{id}/play -> conta um play (o player chama após alguns segundos). */
    @PostMapping("/tracks/{id}/play")
    public MessageResponse registerPlay(@PathVariable Long id, Authentication auth) {
        libraryService.registerPlay(currentUser.require(auth), id);
        return new MessageResponse("Play registrado");
    }

    /** POST /api/library/tracks/{id}/favorite -> alterna a curtida. */
    @PostMapping("/tracks/{id}/favorite")
    public Map<String, Boolean> favoriteTrack(@PathVariable Long id, Authentication auth) {
        User me = currentUser.require(auth);
        return Map.of("favorite", libraryService.toggleFavorite(me, LibraryFavorite.Kind.TRACK, id));
    }

    @GetMapping("/albums")
    public List<AlbumResponse> albums() {
        return libraryService.albums();
    }

    @GetMapping("/albums/tracks")
    public List<TrackResponse> albumTracks(@RequestParam("name") String name, Authentication auth) {
        return libraryService.tracksOfAlbum(currentUser.require(auth), name);
    }

    @GetMapping("/artists")
    public List<ArtistResponse> artists() {
        return libraryService.artists();
    }

    @GetMapping("/artists/tracks")
    public List<TrackResponse> artistTracks(@RequestParam("name") String name, Authentication auth) {
        return libraryService.tracksOfArtist(currentUser.require(auth), name);
    }

    // ------------------------------------------------------------------ LIVROS
    @GetMapping("/books")
    public List<BookResponse> books(Authentication auth) {
        return libraryService.allBooks(currentUser.require(auth));
    }

    @GetMapping("/books/{id}")
    public BookResponse book(@PathVariable Long id, Authentication auth) {
        return libraryService.book(currentUser.require(auth), id);
    }

    /**
     * GET /api/library/books/{id}/file -> o PDF para ler no navegador.
     * Vai como "inline" e com Range: é o que deixa o leitor de PDF pular para
     * uma página sem baixar o livro inteiro antes.
     */
    @GetMapping("/books/{id}/file")
    public ResponseEntity<Resource> bookFile(@PathVariable Long id, HttpServletRequest request) {
        Book book = libraryService.requireBook(id);
        return RangeSupport.serve(
                request,
                storage.resolve(LibraryStorage.Bucket.BOOKS, book.getStorageKey()),
                book.getMimeType(),
                null);
    }

    @GetMapping("/books/{id}/download")
    public ResponseEntity<Resource> downloadBook(@PathVariable Long id, HttpServletRequest request) {
        Book book = libraryService.requireBook(id);
        return RangeSupport.serve(
                request,
                storage.resolve(LibraryStorage.Bucket.BOOKS, book.getStorageKey()),
                book.getMimeType(),
                book.getOriginalFilename());
    }

    /** PUT /api/library/books/{id}/progress {page} -> guarda onde a pessoa parou. */
    @PutMapping("/books/{id}/progress")
    public MessageResponse saveProgress(@PathVariable Long id,
                                        @Valid @RequestBody BookProgressRequest body,
                                        Authentication auth) {
        libraryService.saveReadingProgress(currentUser.require(auth), id, body.page());
        return new MessageResponse("Progresso salvo");
    }

    @PostMapping("/books/{id}/favorite")
    public Map<String, Boolean> favoriteBook(@PathVariable Long id, Authentication auth) {
        User me = currentUser.require(auth);
        return Map.of("favorite", libraryService.toggleFavorite(me, LibraryFavorite.Kind.BOOK, id));
    }

    // ------------------------------------------------------------------ ZIP
    /** GET /api/library/download/tracks?ids=1,2,3 -> ZIP das faixas escolhidas. */
    @GetMapping("/download/tracks")
    public ResponseEntity<StreamingResponseBody> downloadTracks(@RequestParam("ids") List<Long> ids) {
        return LibraryDownloadService.asAttachment(downloadService.tracksZip(ids), "lumo-musicas.zip");
    }

    /** GET /api/library/download/books?ids=1,2,3 -> ZIP dos livros escolhidos. */
    @GetMapping("/download/books")
    public ResponseEntity<StreamingResponseBody> downloadBooks(@RequestParam("ids") List<Long> ids) {
        return LibraryDownloadService.asAttachment(downloadService.booksZip(ids), "lumo-livros.zip");
    }

}
