package com.movies.backend.library.repository;

import com.movies.backend.library.entity.Book;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookRepository extends JpaRepository<Book, Long> {

    Optional<Book> findByChecksum(String checksum);

    List<Book> findAllByOrderByCreatedAtDesc();

    Page<Book> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<Book> findTop12ByOrderByCreatedAtDesc();

    List<Book> findTop12ByOrderByReadCountDescIdDesc();

    @Query("""
            select b from Book b
            where lower(b.title) like lower(concat('%', :q, '%'))
               or lower(coalesce(b.author, '')) like lower(concat('%', :q, '%'))
               or lower(coalesce(b.publisher, '')) like lower(concat('%', :q, '%'))
               or lower(coalesce(b.genre, '')) like lower(concat('%', :q, '%'))
            order by b.readCount desc, b.title asc
            """)
    List<Book> search(@Param("q") String q, Pageable pageable);

    List<Book> findByAuthorIgnoreCaseOrderByTitleAsc(String author);

    long countByCreatedAtAfter(Instant since);

    @Query("select coalesce(sum(b.fileSize), 0) from Book b")
    long totalBytes();

    @Query("select coalesce(sum(b.pageCount), 0) from Book b")
    long totalPages();

    @Query("""
            select b.author, min(b.coverUrl), count(b)
            from Book b
            where b.author is not null and b.author <> ''
            group by b.author
            order by count(b) desc, b.author asc
            """)
    List<Object[]> authorSummaries();

    @Query("""
            select b.genre, count(b)
            from Book b
            where b.genre is not null and b.genre <> ''
            group by b.genre
            order by count(b) desc
            """)
    List<Object[]> genreSummaries();
}
