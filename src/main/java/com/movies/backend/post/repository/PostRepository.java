package com.movies.backend.post.repository;

import com.movies.backend.post.entity.Post;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {

    List<Post> findByAuthorIdInOrderByCreatedAtDesc(Collection<Long> authorIds, Pageable pageable);

    List<Post> findByAuthorIdOrderByCreatedAtDesc(Long authorId);
}
