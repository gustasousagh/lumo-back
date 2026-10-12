package com.movies.backend.post.repository;

import com.movies.backend.post.entity.Post;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PostRepository extends JpaRepository<Post, Long> {

    List<Post> findByAuthorIdInOrderByCreatedAtDesc(Collection<Long> authorIds, Pageable pageable);

    List<Post> findByAuthorIdOrderByCreatedAtDesc(Long authorId);

    /** [mediaType, mediaId, média, quantidade] das notas dadas nas resenhas. */
    @Query("select p.mediaType, p.mediaId, avg(p.rating), count(p) from Post p "
            + "where p.rating is not null and p.mediaId is not null group by p.mediaType, p.mediaId")
    List<Object[]> averageRatings();
}
