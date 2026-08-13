package com.movies.backend.library.service;

import com.movies.backend.exception.ApiException;
import com.movies.backend.library.dto.UpdateBookRequest;
import com.movies.backend.library.dto.UpdateTrackRequest;
import com.movies.backend.library.entity.Book;
import com.movies.backend.library.entity.BookProgress;
import com.movies.backend.library.entity.LibraryFavorite;
import com.movies.backend.library.entity.PlayEvent;
import com.movies.backend.library.entity.Track;
import com.movies.backend.library.repository.BookProgressRepository;
import com.movies.backend.library.repository.BookRepository;
import com.movies.backend.library.repository.LibraryFavoriteRepository;
import com.movies.backend.library.repository.PlayEventRepository;
import com.movies.backend.library.repository.PlaylistTrackRepository;
import com.movies.backend.library.repository.TrackRepository;
import com.movies.backend.library.response.AlbumResponse;
import com.movies.backend.library.response.ArtistResponse;
import com.movies.backend.library.response.BookResponse;
import com.movies.backend.library.response.LibraryCountsResponse;
import com.movies.backend.library.response.LibraryHomeResponse;
import com.movies.backend.library.response.LibrarySearchResponse;
import com.movies.backend.library.response.TrackResponse;
import com.movies.backend.library.storage.LibraryStorage;
import com.movies.backend.user.entity.User;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consulta e curadoria da biblioteca: navegar, buscar, favoritar, marcar
 * progresso de leitura e contar plays. Upload fica no {@link LibraryUploadService};
 * playlists, no {@link PlaylistService}.
 */
@Service
public class LibraryService {

    private final TrackRepository trackRepository;
    private final BookRepository bookRepository;
    private final LibraryFavoriteRepository favoriteRepository;
    private final BookProgressRepository progressRepository;
    private final PlayEventRepository playEventRepository;
    private final PlaylistTrackRepository playlistTrackRepository;
    private final PlaylistService playlistService;
    private final LibraryStorage storage;

    public LibraryService(TrackRepository trackRepository,
                          BookRepository bookRepository,
                          LibraryFavoriteRepository favoriteRepository,
                          BookProgressRepository progressRepository,
                          PlayEventRepository playEventRepository,
                          PlaylistTrackRepository playlistTrackRepository,
                          PlaylistService playlistService,
                          LibraryStorage storage) {
        this.trackRepository = trackRepository;
        this.bookRepository = bookRepository;
        this.favoriteRepository = favoriteRepository;
        this.progressRepository = progressRepository;
        this.playEventRepository = playEventRepository;
        this.playlistTrackRepository = playlistTrackRepository;
        this.playlistService = playlistService;
        this.storage = storage;
    }

    // -------------------------------------------------------------------- HOME
    /** Tudo que a tela inicial da biblioteca mostra, numa chamada só. */
    @Transactional(readOnly = true)
    public LibraryHomeResponse home(User me) {
        Set<Long> favTracks = favoriteIds(me, LibraryFavorite.Kind.TRACK);
        Set<Long> favBooks = favoriteIds(me, LibraryFavorite.Kind.BOOK);

        List<BookProgress> reading = progressRepository.findTop12ByUserIdOrderByUpdatedAtDesc(me.getId());
        Map<Long, Integer> pageByBook = reading.stream()
                .collect(Collectors.toMap(BookProgress::getBookId, BookProgress::getPage, (a, b) -> a));
        List<Book> readingBooks = bookRepository.findAllById(pageByBook.keySet());
        // findAllById não garante ordem; reordenamos pela última leitura.
        Map<Long, Book> byId = readingBooks.stream().collect(Collectors.toMap(Book::getId, Function.identity()));
        List<BookResponse> continueReading = reading.stream()
                .map(p -> byId.get(p.getBookId()))
                .filter(java.util.Objects::nonNull)
                .map(b -> BookResponse.from(b, favBooks.contains(b.getId()), pageByBook.get(b.getId())))
                .toList();

        List<Track> favoriteTracks = trackRepository.findAllById(favTracks);

        return new LibraryHomeResponse(
                tracks(trackRepository.findTop12ByOrderByCreatedAtDesc(), favTracks),
                tracks(trackRepository.findTop12ByOrderByPlayCountDescIdDesc(), favTracks),
                tracks(favoriteTracks, favTracks),
                books(bookRepository.findTop12ByOrderByCreatedAtDesc(), favBooks),
                continueReading,
                playlistService.myPlaylists(me),
                playlistService.sharedWithMe(me),
                playlistService.publicPlaylists(me),
                albums(),
                artists(),
                counts());
    }

    @Transactional(readOnly = true)
    public LibraryCountsResponse counts() {
        return new LibraryCountsResponse(
                trackRepository.count(),
                bookRepository.count(),
                playlistService.totalCount(),
                trackRepository.totalDurationSec());
    }

    // ------------------------------------------------------------------ BUSCA
    /**
     * Busca em música e livro ao mesmo tempo. Termo curto devolve vazio em vez
     * de erro: a tela busca a cada tecla, e um 400 a cada letra digitada faria o
     * console piscar em vermelho sem nenhum ganho.
     */
    @Transactional(readOnly = true)
    public LibrarySearchResponse search(User me, String q, int limit) {
        String term = q == null ? "" : q.trim();
        if (term.length() < 2) {
            return new LibrarySearchResponse(List.of(), List.of(), List.of(), List.of(), List.of());
        }
        var page = PageRequest.of(0, Math.clamp(limit, 1, 100));
        Set<Long> favTracks = favoriteIds(me, LibraryFavorite.Kind.TRACK);
        Set<Long> favBooks = favoriteIds(me, LibraryFavorite.Kind.BOOK);

        String lower = term.toLowerCase();
        return new LibrarySearchResponse(
                tracks(trackRepository.search(term, page), favTracks),
                books(bookRepository.search(term, page), favBooks),
                albums().stream().filter(a -> contains(a.name(), lower) || contains(a.artist(), lower)).limit(12).toList(),
                artists().stream().filter(a -> contains(a.name(), lower)).limit(12).toList(),
                playlistService.searchVisible(me, lower));
    }

    // ----------------------------------------------------------------- MÚSICA
    @Transactional(readOnly = true)
    public List<TrackResponse> allTracks(User me) {
        return tracks(trackRepository.findAllByOrderByCreatedAtDesc(), favoriteIds(me, LibraryFavorite.Kind.TRACK));
    }

    @Transactional(readOnly = true)
    public TrackResponse track(User me, Long id) {
        Track track = requireTrack(id);
        return TrackResponse.from(track, isFavorite(me, LibraryFavorite.Kind.TRACK, id));
    }

    @Transactional(readOnly = true)
    public List<TrackResponse> tracksOfAlbum(User me, String album) {
        return tracks(trackRepository.findByAlbumIgnoreCaseOrderByDiscNumberAscTrackNumberAsc(album),
                favoriteIds(me, LibraryFavorite.Kind.TRACK));
    }

    @Transactional(readOnly = true)
    public List<TrackResponse> tracksOfArtist(User me, String artist) {
        return tracks(trackRepository.findByArtistIgnoreCaseOrderByAlbumAscTrackNumberAsc(artist),
                favoriteIds(me, LibraryFavorite.Kind.TRACK));
    }

    @Transactional(readOnly = true)
    public List<AlbumResponse> albums() {
        return trackRepository.albumSummaries().stream()
                .map(row -> new AlbumResponse(
                        (String) row[0],
                        (String) row[1],
                        (String) row[2],
                        ((Number) row[3]).longValue(),
                        row[4] == null ? null : ((Number) row[4]).intValue()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ArtistResponse> artists() {
        return trackRepository.artistSummaries().stream()
                .map(row -> new ArtistResponse(
                        (String) row[0],
                        (String) row[1],
                        ((Number) row[2]).longValue()))
                .toList();
    }

    /** Registra que a faixa tocou. Chamado pelo player após alguns segundos. */
    @Transactional
    public void registerPlay(User me, Long trackId) {
        Track track = requireTrack(trackId);
        track.setPlayCount(track.getPlayCount() + 1);
        trackRepository.save(track);

        PlayEvent event = new PlayEvent();
        event.setTrackId(trackId);
        event.setUserId(me.getId());
        playEventRepository.save(event);
    }

    // ----------------------------------------------------------------- LIVROS
    @Transactional(readOnly = true)
    public List<BookResponse> allBooks(User me) {
        return books(bookRepository.findAllByOrderByCreatedAtDesc(), favoriteIds(me, LibraryFavorite.Kind.BOOK));
    }

    @Transactional(readOnly = true)
    public BookResponse book(User me, Long id) {
        Book book = requireBook(id);
        Integer page = progressRepository.findByUserIdAndBookId(me.getId(), id)
                .map(BookProgress::getPage)
                .orElse(null);
        return BookResponse.from(book, isFavorite(me, LibraryFavorite.Kind.BOOK, id), page);
    }

    /** Salva a página atual e conta a primeira abertura como leitura. */
    @Transactional
    public void saveReadingProgress(User me, Long bookId, int page) {
        Book book = requireBook(bookId);
        BookProgress progress = progressRepository.findByUserIdAndBookId(me.getId(), bookId)
                .orElseGet(() -> {
                    // Primeira vez desta pessoa neste livro: conta como leitura.
                    book.setReadCount(book.getReadCount() + 1);
                    bookRepository.save(book);
                    BookProgress fresh = new BookProgress();
                    fresh.setUserId(me.getId());
                    fresh.setBookId(bookId);
                    return fresh;
                });
        progress.setPage(Math.max(1, page));
        progress.setUpdatedAt(Instant.now());
        progressRepository.save(progress);
    }

    // -------------------------------------------------------------- FAVORITOS
    /** Alterna a curtida e devolve o estado novo. */
    @Transactional
    public boolean toggleFavorite(User me, LibraryFavorite.Kind kind, Long refId) {
        if (kind == LibraryFavorite.Kind.TRACK) {
            requireTrack(refId);
        } else {
            requireBook(refId);
        }
        var existing = favoriteRepository.findByUserIdAndKindAndRefId(me.getId(), kind.name(), refId);
        if (existing.isPresent()) {
            favoriteRepository.delete(existing.get());
            return false;
        }
        LibraryFavorite favorite = new LibraryFavorite();
        favorite.setUserId(me.getId());
        favorite.setKind(kind);
        favorite.setRefId(refId);
        favoriteRepository.save(favorite);
        return true;
    }

    // ------------------------------------------------------- EDIÇÃO (ADMIN)
    @Transactional
    public TrackResponse updateTrack(Long id, UpdateTrackRequest req) {
        Track track = requireTrack(id);
        if (req.title() != null && !req.title().isBlank()) {
            track.setTitle(req.title().trim());
        }
        if (req.artist() != null) {
            track.setArtist(emptyToNull(req.artist()));
        }
        if (req.album() != null) {
            track.setAlbum(emptyToNull(req.album()));
        }
        if (req.albumArtist() != null) {
            track.setAlbumArtist(emptyToNull(req.albumArtist()));
        }
        if (req.genre() != null) {
            track.setGenre(emptyToNull(req.genre()));
        }
        if (req.year() != null) {
            track.setYear(req.year());
        }
        if (req.trackNumber() != null) {
            track.setTrackNumber(req.trackNumber());
        }
        if (req.discNumber() != null) {
            track.setDiscNumber(req.discNumber());
        }
        if (req.coverUrl() != null) {
            track.setCoverUrl(emptyToNull(req.coverUrl()));
        }
        return TrackResponse.from(trackRepository.save(track));
    }

    @Transactional
    public BookResponse updateBook(Long id, UpdateBookRequest req) {
        Book book = requireBook(id);
        if (req.title() != null && !req.title().isBlank()) {
            book.setTitle(req.title().trim());
        }
        if (req.author() != null) {
            book.setAuthor(emptyToNull(req.author()));
        }
        if (req.publisher() != null) {
            book.setPublisher(emptyToNull(req.publisher()));
        }
        if (req.genre() != null) {
            book.setGenre(emptyToNull(req.genre()));
        }
        if (req.language() != null) {
            book.setLanguage(emptyToNull(req.language()));
        }
        if (req.year() != null) {
            book.setYear(req.year());
        }
        if (req.isbn() != null) {
            book.setIsbn(emptyToNull(req.isbn()));
        }
        if (req.description() != null) {
            book.setDescription(emptyToNull(req.description()));
        }
        if (req.coverUrl() != null) {
            book.setCoverUrl(emptyToNull(req.coverUrl()));
        }
        return BookResponse.from(bookRepository.save(book));
    }

    /**
     * Apaga a faixa do banco, do disco e de toda playlist onde ela estava.
     * Deixar a linha na playlist criaria um item fantasma que quebra o player.
     */
    @Transactional
    public void deleteTrack(Long id) {
        Track track = requireTrack(id);
        playlistTrackRepository.deleteByTrackId(id);
        playEventRepository.deleteByTrackId(id);
        favoriteRepository.deleteByKindAndRefId(LibraryFavorite.Kind.TRACK.name(), id);
        trackRepository.delete(track);
        storage.delete(LibraryStorage.Bucket.MUSIC, track.getStorageKey());
    }

    @Transactional
    public void deleteBook(Long id) {
        Book book = requireBook(id);
        progressRepository.deleteByBookId(id);
        favoriteRepository.deleteByKindAndRefId(LibraryFavorite.Kind.BOOK.name(), id);
        bookRepository.delete(book);
        storage.delete(LibraryStorage.Bucket.BOOKS, book.getStorageKey());
    }

    // ----------------------------------------------------------------- ACESSO
    public Track requireTrack(Long id) {
        return trackRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Faixa não encontrada"));
    }

    public Book requireBook(Long id) {
        return bookRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Livro não encontrado"));
    }

    // ---------------------------------------------------------------- HELPERS
    private Set<Long> favoriteIds(User me, LibraryFavorite.Kind kind) {
        if (me == null) {
            return Set.of();
        }
        return favoriteRepository.findByUserIdAndKindOrderByCreatedAtDesc(me.getId(), kind.name())
                .stream()
                .map(LibraryFavorite::getRefId)
                .collect(Collectors.toCollection(HashSet::new));
    }

    private boolean isFavorite(User me, LibraryFavorite.Kind kind, Long refId) {
        return me != null
                && favoriteRepository.findByUserIdAndKindAndRefId(me.getId(), kind.name(), refId).isPresent();
    }

    private List<TrackResponse> tracks(List<Track> list, Set<Long> favorites) {
        List<TrackResponse> out = new ArrayList<>(list.size());
        for (Track track : list) {
            out.add(TrackResponse.from(track, favorites.contains(track.getId())));
        }
        return out;
    }

    private List<BookResponse> books(List<Book> list, Set<Long> favorites) {
        List<BookResponse> out = new ArrayList<>(list.size());
        for (Book book : list) {
            out.add(BookResponse.from(book, favorites.contains(book.getId()), null));
        }
        return out;
    }

    private boolean contains(String value, String lowerTerm) {
        return value != null && value.toLowerCase().contains(lowerTerm);
    }

    private String emptyToNull(String value) {
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
