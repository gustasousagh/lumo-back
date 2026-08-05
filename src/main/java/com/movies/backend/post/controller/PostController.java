package com.movies.backend.post.controller;

import com.movies.backend.post.dto.CreatePostRequest;
import com.movies.backend.post.dto.PostCommentRequest;
import com.movies.backend.post.response.PostCommentResponse;
import com.movies.backend.post.response.PostResponse;
import com.movies.backend.post.service.PostService;
import com.movies.backend.security.CurrentUser;
import com.movies.backend.user.entity.User;
import com.movies.backend.user.response.MessageResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints do feed social: publicações, feed, curtidas e comentários.
 * Não usa @RequestMapping de classe porque abrange /api/posts, /api/feed
 * e /api/users/{username}/posts.
 */
@RestController
public class PostController {

    private final PostService postService;
    private final CurrentUser currentUser;

    public PostController(PostService postService, CurrentUser currentUser) {
        this.postService = postService;
        this.currentUser = currentUser;
    }

    /** POST /api/posts -> cria uma publicação (texto e/ou resenha de mídia). */
    @PostMapping("/api/posts")
    public PostResponse create(@Valid @RequestBody CreatePostRequest body, Authentication auth) {
        User me = currentUser.require(auth);
        return postService.create(me, body);
    }

    /** GET /api/feed -> publicações minhas + dos meus amigos (mais novas primeiro, até 50). */
    @GetMapping("/api/feed")
    public List<PostResponse> feed(Authentication auth) {
        User me = currentUser.require(auth);
        return postService.feed(me);
    }

    /** GET /api/users/{username}/posts -> publicações públicas de um usuário. */
    @GetMapping("/api/users/{username}/posts")
    public List<PostResponse> byUsername(@PathVariable String username, Authentication auth) {
        User me = currentUser.require(auth);
        return postService.byUsername(me, username);
    }

    /** POST /api/posts/{id}/like -> alterna a curtida do usuário; devolve {liked, likeCount}. */
    @PostMapping("/api/posts/{id}/like")
    public Map<String, Object> toggleLike(@PathVariable Long id, Authentication auth) {
        User me = currentUser.require(auth);
        return postService.toggleLike(me, id);
    }

    /** GET /api/posts/{id}/comments -> comentários da publicação (mais novos primeiro). */
    @GetMapping("/api/posts/{id}/comments")
    public List<PostCommentResponse> comments(@PathVariable Long id) {
        return postService.listComments(id);
    }

    /** POST /api/posts/{id}/comments {text} -> cria e devolve o comentário com autor. */
    @PostMapping("/api/posts/{id}/comments")
    public PostCommentResponse addComment(@PathVariable Long id,
                                          @Valid @RequestBody PostCommentRequest body,
                                          Authentication auth) {
        User me = currentUser.require(auth);
        return postService.addComment(me, id, body);
    }

    /** DELETE /api/posts/{id} -> apaga uma publicação (só o autor). */
    @DeleteMapping("/api/posts/{id}")
    public MessageResponse delete(@PathVariable Long id, Authentication auth) {
        User me = currentUser.require(auth);
        postService.delete(me, id);
        return new MessageResponse("Publicação removida");
    }
}
