package com.movies.backend.post.repository;

import com.movies.backend.post.entity.PostComment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostCommentRepository extends JpaRepository<PostComment, Long> {

    List<PostComment> findByPostIdOrderByCreatedAtDesc(Long postId);

    long countByPostId(Long postId);
}
