package com.movies.backend.post.service;

import com.movies.backend.exception.ApiException;
import com.movies.backend.friend.entity.Friendship;
import com.movies.backend.friend.repository.FriendshipRepository;
import com.movies.backend.post.dto.CreatePostRequest;
import com.movies.backend.post.dto.PostCommentRequest;
import com.movies.backend.post.entity.Post;
import com.movies.backend.post.entity.PostComment;
import com.movies.backend.post.entity.PostLike;
import com.movies.backend.post.repository.PostCommentRepository;
import com.movies.backend.post.repository.PostLikeRepository;
import com.movies.backend.post.repository.PostRepository;
import com.movies.backend.post.response.PostCommentResponse;
import com.movies.backend.post.response.PostResponse;
import com.movies.backend.notification.entity.NotificationType;
import com.movies.backend.notification.service.NotificationService;
import com.movies.backend.user.entity.User;
import com.movies.backend.user.repository.UserRepository;
import com.movies.backend.user.response.UserMiniResponse;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Regras do feed social: publicações, curtidas e comentários. */
@Service
public class PostService {

    private final PostRepository postRepository;
    private final PostLikeRepository likeRepository;
    private final PostCommentRepository commentRepository;
    private final UserRepository userRepository;
    private final FriendshipRepository friendshipRepository;
    private final NotificationService notificationService;

    public PostService(PostRepository postRepository,
                       PostLikeRepository likeRepository,
                       PostCommentRepository commentRepository,
                       UserRepository userRepository,
                       FriendshipRepository friendshipRepository,
                       NotificationService notificationService) {
        this.postRepository = postRepository;
        this.likeRepository = likeRepository;
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
        this.friendshipRepository = friendshipRepository;
        this.notificationService = notificationService;
    }

    // ------------------------------------------------------------------ CRIAR
    @Transactional
    public PostResponse create(User me, CreatePostRequest req) {
        boolean hasText = req.text() != null && !req.text().isBlank();
        boolean hasMedia = req.mediaId() != null;
        if (!hasText && !hasMedia) {
            throw ApiException.badRequest("Escreva algo ou anexe uma mídia");
        }

        Post post = new Post();
        post.setAuthorId(me.getId());
        post.setText(hasText ? req.text().trim() : null);
        post.setKind(hasMedia ? "REVIEW" : "TEXT");
        if (hasMedia) {
            post.setMediaType(req.mediaType());
            post.setMediaId(req.mediaId());
            post.setMediaTitle(req.mediaTitle());
            post.setMediaPosterUrl(req.mediaPosterUrl());
            post.setRating(req.rating());
        }
        Post saved = postRepository.save(post);
        return toResponse(saved, me);
    }

    // ------------------------------------------------------------------- FEED
    @Transactional(readOnly = true)
    public List<PostResponse> feed(User me) {
        Set<Long> authorIds = new LinkedHashSet<>();
        authorIds.add(me.getId());
        for (Friendship f : friendshipRepository.findByUserId(me.getId())) {
            authorIds.add(f.getFriendId());
        }
        return postRepository
                .findByAuthorIdInOrderByCreatedAtDesc(authorIds, PageRequest.of(0, 50))
                .stream()
                .map(p -> toResponse(p, me))
                .toList();
    }

    // -------------------------------------------------------- POSTS DO USUÁRIO
    @Transactional(readOnly = true)
    public List<PostResponse> byUsername(User me, String username) {
        User author = userRepository.findByUsername(username)
                .orElseThrow(() -> ApiException.notFound("Usuário não encontrado"));
        return postRepository.findByAuthorIdOrderByCreatedAtDesc(author.getId())
                .stream()
                .map(p -> toResponse(p, me))
                .toList();
    }

    // ------------------------------------------------------------------ CURTIR
    @Transactional
    public Map<String, Object> toggleLike(User me, Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> ApiException.notFound("Publicação não encontrada"));
        PostLike existing = likeRepository.findByPostIdAndUserId(post.getId(), me.getId()).orElse(null);
        boolean liked;
        if (existing != null) {
            likeRepository.delete(existing);
            liked = false;
        } else {
            PostLike like = new PostLike();
            like.setPostId(post.getId());
            like.setUserId(me.getId());
            likeRepository.save(like);
            liked = true;
            // notifica o autor (se não for você mesmo)
            if (!post.getAuthorId().equals(me.getId())) {
                notificationService.push(post.getAuthorId(), NotificationType.GENERIC,
                        "Curtiram sua publicação ❤️",
                        me.getName() + " curtiu o que você postou.",
                        "/feed", me.getId());
            }
        }
        return Map.of("liked", liked, "likeCount", likeRepository.countByPostId(post.getId()));
    }

    // -------------------------------------------------------------- COMENTÁRIOS
    @Transactional(readOnly = true)
    public List<PostCommentResponse> listComments(Long postId) {
        if (!postRepository.existsById(postId)) {
            throw ApiException.notFound("Publicação não encontrada");
        }
        return commentRepository.findByPostIdOrderByCreatedAtDesc(postId)
                .stream()
                .map(this::toCommentResponse)
                .toList();
    }

    @Transactional
    public PostCommentResponse addComment(User me, Long postId, PostCommentRequest req) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> ApiException.notFound("Publicação não encontrada"));
        PostComment comment = new PostComment();
        comment.setPostId(postId);
        comment.setUserId(me.getId());
        comment.setText(req.text().trim());
        PostCommentResponse response = toCommentResponse(commentRepository.save(comment));
        // notifica o autor do post (se não for você mesmo)
        if (!post.getAuthorId().equals(me.getId())) {
            String preview = comment.getText().length() > 60
                    ? comment.getText().substring(0, 60) + "…" : comment.getText();
            notificationService.push(post.getAuthorId(), NotificationType.COMMENT,
                    "Comentaram sua publicação 💬",
                    me.getName() + ": " + preview,
                    "/feed", me.getId());
        }
        return response;
    }

    // ------------------------------------------------------------------ APAGAR
    @Transactional
    public void delete(User me, Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> ApiException.notFound("Publicação não encontrada"));
        if (!post.getAuthorId().equals(me.getId())) {
            throw ApiException.forbidden("Você só pode apagar suas próprias publicações");
        }
        postRepository.delete(post);
    }

    // -------------------------------------------------------------- MAPEAMENTO
    private PostResponse toResponse(Post post, User viewer) {
        UserMiniResponse author = userRepository.findById(post.getAuthorId())
                .map(UserMiniResponse::from)
                .orElse(null);
        PostResponse.MediaRef media = null;
        if (post.getMediaId() != null) {
            media = new PostResponse.MediaRef(
                    post.getMediaType(),
                    post.getMediaId(),
                    post.getMediaTitle(),
                    post.getMediaPosterUrl());
        }
        boolean likedByMe = viewer != null
                && likeRepository.existsByPostIdAndUserId(post.getId(), viewer.getId());
        return new PostResponse(
                post.getId(),
                author,
                post.getText(),
                post.getKind(),
                media,
                post.getRating(),
                likeRepository.countByPostId(post.getId()),
                commentRepository.countByPostId(post.getId()),
                likedByMe,
                post.getCreatedAt());
    }

    private PostCommentResponse toCommentResponse(PostComment comment) {
        UserMiniResponse author = userRepository.findById(comment.getUserId())
                .map(UserMiniResponse::from)
                .orElse(null);
        return new PostCommentResponse(comment.getId(), author, comment.getText(), comment.getCreatedAt());
    }
}
