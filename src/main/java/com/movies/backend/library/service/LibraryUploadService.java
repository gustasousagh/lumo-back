package com.movies.backend.library.service;

import com.movies.backend.exception.ApiException;
import com.movies.backend.library.entity.Book;
import com.movies.backend.library.entity.Track;
import com.movies.backend.library.metadata.AudioMetadata;
import com.movies.backend.library.metadata.AudioMetadataExtractor;
import com.movies.backend.library.metadata.BookMetadata;
import com.movies.backend.library.metadata.BookMetadataExtractor;
import com.movies.backend.library.repository.BookRepository;
import com.movies.backend.library.repository.TrackRepository;
import com.movies.backend.library.response.BookResponse;
import com.movies.backend.library.response.TrackResponse;
import com.movies.backend.library.response.UploadResultResponse;
import com.movies.backend.library.storage.LibraryStorage;
import com.movies.backend.user.entity.User;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Recebe os arquivos, lê o que dá de dentro deles e cadastra na biblioteca.
 *
 * <p>A ordem importa: o arquivo é gravado ANTES da checagem de duplicado porque
 * o checksum só existe depois de ler o conteúdo inteiro — e ler o upload duas
 * vezes (uma para o hash, outra para gravar) dobraria o custo do maior arquivo
 * do sistema. Quando o duplicado aparece, o arquivo recém-gravado é apagado.
 */
@Service
public class LibraryUploadService {

    private static final Logger log = LoggerFactory.getLogger(LibraryUploadService.class);

    /** Extensões de áudio aceitas -> mime devolvido no streaming. */
    private static final Map<String, String> AUDIO_TYPES = Map.of(
            ".mp3", "audio/mpeg",
            ".m4a", "audio/mp4",
            ".aac", "audio/aac",
            ".ogg", "audio/ogg",
            ".opus", "audio/ogg",
            ".flac", "audio/flac",
            ".wav", "audio/wav");

    private static final Set<String> BOOK_EXTENSIONS = Set.of(".pdf");

    private final LibraryStorage storage;
    private final AudioMetadataExtractor audioExtractor;
    private final BookMetadataExtractor bookExtractor;
    private final TrackRepository trackRepository;
    private final BookRepository bookRepository;
    private final long maxAudioBytes;
    private final long maxBookBytes;

    public LibraryUploadService(LibraryStorage storage,
                                AudioMetadataExtractor audioExtractor,
                                BookMetadataExtractor bookExtractor,
                                TrackRepository trackRepository,
                                BookRepository bookRepository,
                                @Value("${app.library.max-audio-mb:80}") long maxAudioMb,
                                @Value("${app.library.max-book-mb:200}") long maxBookMb) {
        this.storage = storage;
        this.audioExtractor = audioExtractor;
        this.bookExtractor = bookExtractor;
        this.trackRepository = trackRepository;
        this.bookRepository = bookRepository;
        this.maxAudioBytes = maxAudioMb * 1024 * 1024;
        this.maxBookBytes = maxBookMb * 1024 * 1024;
    }

    // ------------------------------------------------------------------ MÚSICA
    /** Sobe uma leva de músicas. Um arquivo ruim não invalida os outros. */
    public UploadResultResponse uploadTracks(User admin, List<MultipartFile> files) {
        requireFiles(files);
        List<UploadResultResponse.Item> results = new ArrayList<>();
        for (MultipartFile file : files) {
            String name = displayName(file);
            try {
                results.add(uploadTrack(admin, file));
            } catch (ApiException ex) {
                results.add(UploadResultResponse.Item.error(name, ex.getMessage()));
            } catch (Exception ex) {
                log.warn("Falha inesperada no upload de {}", name, ex);
                results.add(UploadResultResponse.Item.error(name, "Não deu para processar esse arquivo"));
            }
        }
        return UploadResultResponse.of(results);
    }

    private UploadResultResponse.Item uploadTrack(User admin, MultipartFile file) {
        String filename = displayName(file);
        String extension = extensionOf(filename);
        String mime = AUDIO_TYPES.get(extension);
        if (mime == null) {
            throw ApiException.badRequest("Formato não suportado (use MP3, M4A, FLAC, OGG ou WAV)");
        }
        if (file.getSize() > maxAudioBytes) {
            throw ApiException.badRequest("Arquivo maior que o limite de " + (maxAudioBytes / 1024 / 1024) + "MB");
        }

        LibraryStorage.Stored stored = storage.save(file, LibraryStorage.Bucket.MUSIC, extension);

        var existing = trackRepository.findByChecksum(stored.checksum());
        if (existing.isPresent()) {
            storage.delete(LibraryStorage.Bucket.MUSIC, stored.storageKey());
            return UploadResultResponse.Item.duplicate(filename,
                    TrackResponse.from(existing.get()));
        }

        Path savedPath = storage.resolve(LibraryStorage.Bucket.MUSIC, stored.storageKey());
        AudioMetadata meta = audioExtractor.extract(savedPath);

        Track track = new Track();
        track.setTitle(firstNonBlank(meta.title(), stripExtension(filename)));
        track.setArtist(meta.artist());
        track.setAlbum(meta.album());
        track.setAlbumArtist(meta.albumArtist());
        track.setGenre(meta.genre());
        track.setYear(meta.year());
        track.setTrackNumber(meta.trackNumber());
        track.setDiscNumber(meta.discNumber());
        track.setDurationSec(meta.durationSec());
        track.setBitrateKbps(meta.bitrateKbps());
        track.setStorageKey(stored.storageKey());
        track.setChecksum(stored.checksum());
        track.setFileSize(stored.size());
        track.setMimeType(mime);
        track.setOriginalFilename(filename);
        track.setCoverUrl(saveCover(meta.coverImage(), meta.coverMime()));
        track.setUploadedById(admin.getId());

        Track saved = trackRepository.save(track);
        return UploadResultResponse.Item.created(filename,
                TrackResponse.from(saved));
    }

    // ------------------------------------------------------------------ LIVROS
    public UploadResultResponse uploadBooks(User admin, List<MultipartFile> files) {
        requireFiles(files);
        List<UploadResultResponse.Item> results = new ArrayList<>();
        for (MultipartFile file : files) {
            String name = displayName(file);
            try {
                results.add(uploadBook(admin, file));
            } catch (ApiException ex) {
                results.add(UploadResultResponse.Item.error(name, ex.getMessage()));
            } catch (Exception ex) {
                log.warn("Falha inesperada no upload de {}", name, ex);
                results.add(UploadResultResponse.Item.error(name, "Não deu para processar esse arquivo"));
            }
        }
        return UploadResultResponse.of(results);
    }

    private UploadResultResponse.Item uploadBook(User admin, MultipartFile file) {
        String filename = displayName(file);
        String extension = extensionOf(filename);
        if (!BOOK_EXTENSIONS.contains(extension)) {
            throw ApiException.badRequest("Por enquanto só PDF");
        }
        if (file.getSize() > maxBookBytes) {
            throw ApiException.badRequest("Arquivo maior que o limite de " + (maxBookBytes / 1024 / 1024) + "MB");
        }

        LibraryStorage.Stored stored = storage.save(file, LibraryStorage.Bucket.BOOKS, extension);

        var existing = bookRepository.findByChecksum(stored.checksum());
        if (existing.isPresent()) {
            storage.delete(LibraryStorage.Bucket.BOOKS, stored.storageKey());
            return UploadResultResponse.Item.duplicate(filename,
                    BookResponse.from(existing.get()));
        }

        Path savedPath = storage.resolve(LibraryStorage.Bucket.BOOKS, stored.storageKey());
        BookMetadata meta = bookExtractor.extract(savedPath);

        Book book = new Book();
        book.setTitle(firstNonBlank(meta.title(), stripExtension(filename)));
        book.setAuthor(meta.author());
        book.setPublisher(meta.publisher());
        // O "assunto" do PDF é o campo mais próximo de uma sinopse que existe.
        book.setDescription(firstNonBlank(meta.subject(), meta.keywords()));
        book.setYear(meta.year());
        book.setPageCount(meta.pageCount());
        book.setStorageKey(stored.storageKey());
        book.setChecksum(stored.checksum());
        book.setFileSize(stored.size());
        book.setMimeType("application/pdf");
        book.setOriginalFilename(filename);
        book.setCoverUrl(saveCover(meta.coverImage(), meta.coverMime()));
        book.setUploadedById(admin.getId());

        Book saved = bookRepository.save(book);
        return UploadResultResponse.Item.created(filename,
                BookResponse.from(saved));
    }

    // ----------------------------------------------------------------- HELPERS
    /** Grava a capa extraída e devolve a URL pública, ou null se não veio capa. */
    private String saveCover(byte[] image, String mime) {
        if (image == null || image.length == 0) {
            return null;
        }
        String extension = "image/png".equals(mime) ? ".png" : ".jpg";
        String key = storage.saveBytes(image, LibraryStorage.Bucket.COVERS, extension);
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/library/covers/" + key)
                .toUriString();
    }

    private void requireFiles(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw ApiException.badRequest("Escolha ao menos um arquivo");
        }
    }

    private String displayName(MultipartFile file) {
        String original = file.getOriginalFilename();
        if (original == null || original.isBlank()) {
            return "arquivo";
        }
        // Alguns navegadores mandam o caminho inteiro em upload de pasta.
        String withoutPath = original.replace('\\', '/');
        int slash = withoutPath.lastIndexOf('/');
        return slash >= 0 ? withoutPath.substring(slash + 1) : withoutPath;
    }

    private String extensionOf(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot < 0 ? "" : filename.substring(dot).toLowerCase(Locale.ROOT);
    }

    private String stripExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        String base = dot <= 0 ? filename : filename.substring(0, dot);
        // "01 - Nome da Musica" vira "Nome da Musica": o número da faixa já está
        // no campo próprio, repetir no título só polui a lista.
        return base.replaceFirst("^\\s*\\d{1,3}\\s*[-._]\\s*", "").trim();
    }

    private String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) {
            return a;
        }
        return (b != null && !b.isBlank()) ? b : "Sem título";
    }
}
