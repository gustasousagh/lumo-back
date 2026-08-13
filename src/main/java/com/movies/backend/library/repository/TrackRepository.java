package com.movies.backend.library.repository;

import com.movies.backend.library.entity.Track;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TrackRepository extends JpaRepository<Track, Long> {

    /** Usado no upload para não cadastrar o mesmo arquivo duas vezes. */
    Optional<Track> findByChecksum(String checksum);

    List<Track> findAllByOrderByCreatedAtDesc();

    Page<Track> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<Track> findTop12ByOrderByPlayCountDescIdDesc();

    List<Track> findTop12ByOrderByCreatedAtDesc();

    /**
     * Busca livre em título, artista e álbum. LOWER nos dois lados porque o
     * collation do MySQL pode ser sensível a caixa dependendo de como o banco
     * foi criado — e aí "beatles" não acharia "Beatles".
     */
    @Query("""
            select t from Track t
            where lower(t.title) like lower(concat('%', :q, '%'))
               or lower(coalesce(t.artist, '')) like lower(concat('%', :q, '%'))
               or lower(coalesce(t.album, '')) like lower(concat('%', :q, '%'))
               or lower(coalesce(t.genre, '')) like lower(concat('%', :q, '%'))
            order by t.playCount desc, t.title asc
            """)
    List<Track> search(@Param("q") String q, Pageable pageable);

    List<Track> findByAlbumIgnoreCaseOrderByDiscNumberAscTrackNumberAsc(String album);

    List<Track> findByArtistIgnoreCaseOrderByAlbumAscTrackNumberAsc(String artist);

    List<Track> findByIdIn(List<Long> ids);

    long countByCreatedAtAfter(Instant since);

    @Query("select coalesce(sum(t.fileSize), 0) from Track t")
    long totalBytes();

    @Query("select coalesce(sum(t.durationSec), 0) from Track t")
    long totalDurationSec();

    /** Álbuns distintos com quantas faixas cada um, para a aba "Álbuns". */
    @Query("""
            select t.album, min(t.albumArtist), min(t.coverUrl), count(t), min(t.year)
            from Track t
            where t.album is not null and t.album <> ''
            group by t.album
            order by count(t) desc, t.album asc
            """)
    List<Object[]> albumSummaries();

    /** Artistas distintos com contagem de faixas, para a aba "Artistas". */
    @Query("""
            select t.artist, min(t.coverUrl), count(t)
            from Track t
            where t.artist is not null and t.artist <> ''
            group by t.artist
            order by count(t) desc, t.artist asc
            """)
    List<Object[]> artistSummaries();

    /** Gêneros distintos com contagem — alimenta os filtros e o painel. */
    @Query("""
            select t.genre, count(t)
            from Track t
            where t.genre is not null and t.genre <> ''
            group by t.genre
            order by count(t) desc
            """)
    List<Object[]> genreSummaries();
}
