package com.movies.backend.library.service;

import com.movies.backend.library.entity.Book;
import com.movies.backend.library.entity.PlaylistTrack;
import com.movies.backend.library.entity.Track;
import com.movies.backend.library.repository.BookRepository;
import com.movies.backend.library.repository.PlaylistTrackRepository;
import com.movies.backend.library.repository.TrackRepository;
import com.movies.backend.library.storage.LibraryStorage;
import com.movies.backend.library.storage.RangeSupport;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

/**
 * Empacota arquivos da biblioteca em ZIP: uma playlist inteira, toda a música,
 * todos os livros ou o acervo completo.
 *
 * <p>O ZIP é montado em streaming, entrada por entrada, direto no
 * {@code OutputStream} da resposta. Escrever num arquivo temporário antes de
 * enviar exigiria espaço em disco igual ao acervo inteiro para o admin apertar
 * "baixar tudo" — e o download só começaria depois de compactar tudo.
 *
 * <p>Compressão fica em {@link ZipOutputStream#STORED}? Não: usamos DEFLATE com
 * nível mínimo. MP3 e PDF já vêm comprimidos e não encolhem, então gastar CPU
 * comprimindo de novo só deixaria o download mais lento.
 */
@Service
public class LibraryDownloadService {

    private static final Logger log = LoggerFactory.getLogger(LibraryDownloadService.class);

    private final TrackRepository trackRepository;
    private final BookRepository bookRepository;
    private final PlaylistTrackRepository playlistTrackRepository;
    private final LibraryStorage storage;

    public LibraryDownloadService(TrackRepository trackRepository,
                                  BookRepository bookRepository,
                                  PlaylistTrackRepository playlistTrackRepository,
                                  LibraryStorage storage) {
        this.trackRepository = trackRepository;
        this.bookRepository = bookRepository;
        this.playlistTrackRepository = playlistTrackRepository;
        this.storage = storage;
    }

    /**
     * ZIP de uma playlist: as faixas numeradas na ordem + um .m3u, para o
     * arquivo baixado abrir na ordem certa em qualquer tocador de desktop.
     */
    public StreamingResponseBody playlistZip(Long playlistId, String playlistTitle) {
        List<PlaylistTrack> items = playlistTrackRepository.findByPlaylistIdOrderByPositionAsc(playlistId);
        return out -> {
            try (ZipOutputStream zip = newZip(out)) {
                StringBuilder m3u = new StringBuilder("#EXTM3U\n");
                Set<String> used = new HashSet<>();
                int index = 1;

                for (PlaylistTrack item : items) {
                    Track track = trackRepository.findById(item.getTrackId()).orElse(null);
                    if (track == null) {
                        continue;
                    }
                    String entryName = unique(used, String.format("%02d - %s%s",
                            index, trackLabel(track), extensionOf(track.getOriginalFilename())));
                    if (writeTrack(zip, track, entryName)) {
                        m3u.append("#EXTINF:")
                                .append(track.getDurationSec() == null ? -1 : track.getDurationSec())
                                .append(',').append(trackLabel(track)).append('\n')
                                .append(entryName).append('\n');
                        index++;
                    }
                }

                zip.putNextEntry(new ZipEntry(safe(playlistTitle) + ".m3u"));
                zip.write(m3u.toString().getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        };
    }

    /** ZIP de faixas avulsas (seleção do painel admin ou favoritos). */
    public StreamingResponseBody tracksZip(List<Long> trackIds) {
        List<Track> tracks = trackIds == null || trackIds.isEmpty()
                ? trackRepository.findAll()
                : trackRepository.findByIdIn(trackIds);
        return out -> {
            try (ZipOutputStream zip = newZip(out)) {
                Set<String> used = new HashSet<>();
                for (Track track : tracks) {
                    // Organiza por artista/álbum: 400 MP3 soltos na raiz do ZIP
                    // é inutilizável, e é assim que um acervo grande sai.
                    String folder = safe(orDefault(track.getArtist(), "Sem artista"))
                            + "/" + safe(orDefault(track.getAlbum(), "Sem álbum")) + "/";
                    String entryName = unique(used, folder + trackLabel(track)
                            + extensionOf(track.getOriginalFilename()));
                    writeTrack(zip, track, entryName);
                }
            }
        };
    }

    /** ZIP de livros. Vazio/nulo = todos. */
    public StreamingResponseBody booksZip(List<Long> bookIds) {
        List<Book> books = bookIds == null || bookIds.isEmpty()
                ? bookRepository.findAll()
                : bookRepository.findAllById(bookIds);
        return out -> {
            try (ZipOutputStream zip = newZip(out)) {
                Set<String> used = new HashSet<>();
                for (Book book : books) {
                    String folder = safe(orDefault(book.getAuthor(), "Sem autor")) + "/";
                    String entryName = unique(used, folder + safe(book.getTitle()) + ".pdf");
                    writeBook(zip, book, entryName);
                }
            }
        };
    }

    /** O acervo inteiro, separado em musica/ e livros/. */
    public StreamingResponseBody everythingZip() {
        List<Track> tracks = trackRepository.findAll();
        List<Book> books = bookRepository.findAll();
        return out -> {
            try (ZipOutputStream zip = newZip(out)) {
                Set<String> used = new HashSet<>();
                for (Track track : tracks) {
                    String entryName = unique(used, "musica/"
                            + safe(orDefault(track.getArtist(), "Sem artista")) + "/"
                            + safe(orDefault(track.getAlbum(), "Sem álbum")) + "/"
                            + trackLabel(track) + extensionOf(track.getOriginalFilename()));
                    writeTrack(zip, track, entryName);
                }
                for (Book book : books) {
                    String entryName = unique(used, "livros/"
                            + safe(orDefault(book.getAuthor(), "Sem autor")) + "/"
                            + safe(book.getTitle()) + ".pdf");
                    writeBook(zip, book, entryName);
                }
            }
        };
    }

    /**
     * Embrulha um ZIP em resposta de download.
     *
     * <p>Sem {@code Content-Length}: o tamanho final só se sabe depois de
     * compactar tudo, e esperar por isso adiaria o início do download em vários
     * minutos num acervo grande. A resposta sai em chunked e o navegador mostra
     * "tamanho desconhecido" enquanto baixa.
     */
    public static ResponseEntity<StreamingResponseBody> asAttachment(StreamingResponseBody body, String filename) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + RangeSupport.sanitize(filename) + "\"")
                .body(body);
    }

    // ---------------------------------------------------------------- HELPERS
    private ZipOutputStream newZip(OutputStream out) {
        ZipOutputStream zip = new ZipOutputStream(out, StandardCharsets.UTF_8);
        zip.setLevel(1);
        return zip;
    }

    private boolean writeTrack(ZipOutputStream zip, Track track, String entryName) throws IOException {
        Path file = storage.resolve(LibraryStorage.Bucket.MUSIC, track.getStorageKey());
        return writeEntry(zip, file, entryName);
    }

    private boolean writeBook(ZipOutputStream zip, Book book, String entryName) throws IOException {
        Path file = storage.resolve(LibraryStorage.Bucket.BOOKS, book.getStorageKey());
        return writeEntry(zip, file, entryName);
    }

    /**
     * Um arquivo sumido do disco não pode abortar o ZIP inteiro: o download já
     * começou, a resposta já tem status 200 e não há como voltar atrás e mandar
     * um erro. Pula e registra no log.
     */
    private boolean writeEntry(ZipOutputStream zip, Path file, String entryName) throws IOException {
        if (!Files.isReadable(file)) {
            log.warn("Arquivo ausente ao montar ZIP: {}", file);
            return false;
        }
        zip.putNextEntry(new ZipEntry(entryName));
        RangeSupport.copyTo(file, zip);
        zip.closeEntry();
        return true;
    }

    private String trackLabel(Track track) {
        String artist = track.getArtist();
        return safe(artist == null || artist.isBlank() ? track.getTitle() : artist + " - " + track.getTitle());
    }

    /** Tira o que atrapalha em nome de arquivo dentro do ZIP. */
    private String safe(String value) {
        String cleaned = value == null ? "" : value.replaceAll("[\\\\/:*?\"<>|\\r\\n]", "-").trim();
        if (cleaned.length() > 120) {
            cleaned = cleaned.substring(0, 120).trim();
        }
        return cleaned.isEmpty() ? "sem-nome" : cleaned;
    }

    /** ZIP não aceita duas entradas com o mesmo caminho — acrescenta (2), (3)… */
    private String unique(Set<String> used, String name) {
        if (used.add(name)) {
            return name;
        }
        int dot = name.lastIndexOf('.');
        String base = dot < 0 ? name : name.substring(0, dot);
        String ext = dot < 0 ? "" : name.substring(dot);
        int counter = 2;
        String candidate;
        do {
            candidate = base + " (" + counter++ + ")" + ext;
        } while (!used.add(candidate));
        return candidate;
    }

    private String extensionOf(String filename) {
        if (filename == null) {
            return ".mp3";
        }
        int dot = filename.lastIndexOf('.');
        return dot < 0 ? ".mp3" : filename.substring(dot).toLowerCase(Locale.ROOT);
    }

    private String orDefault(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }
}
