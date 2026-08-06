package com.movies.backend.friend.controller;

import com.movies.backend.friend.dto.SendFriendRequest;
import com.movies.backend.friend.response.FriendRequestResponse;
import com.movies.backend.friend.response.FriendResponse;
import com.movies.backend.friend.service.FriendService;
import com.movies.backend.security.CurrentUser;
import com.movies.backend.user.entity.User;
import com.movies.backend.user.response.MessageResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints de amizade (baseada em convites). */
@RestController
@RequestMapping("/api/friends")
public class FriendController {

    private final FriendService friendService;
    private final CurrentUser currentUser;

    public FriendController(FriendService friendService, CurrentUser currentUser) {
        this.friendService = friendService;
        this.currentUser = currentUser;
    }

    /** POST /api/friends/requests {toUsername} -> envia convite. */
    @PostMapping("/requests")
    public FriendRequestResponse send(@Valid @RequestBody SendFriendRequest body, Authentication auth) {
        User me = currentUser.require(auth);
        return friendService.sendRequest(me, body.toUsername());
    }

    /** GET /api/friends/requests?box=incoming|outgoing -> lista convites pendentes. */
    @GetMapping("/requests")
    public List<FriendRequestResponse> requests(
            @RequestParam(name = "box", defaultValue = "incoming") String box,
            Authentication auth) {
        User me = currentUser.require(auth);
        return friendService.listRequests(me, box);
    }

    /** POST /api/friends/requests/{id}/accept */
    @PostMapping("/requests/{id}/accept")
    public MessageResponse accept(@PathVariable Long id, Authentication auth) {
        User me = currentUser.require(auth);
        friendService.accept(me, id);
        return new MessageResponse("Convite aceito");
    }

    /** POST /api/friends/requests/{id}/decline */
    @PostMapping("/requests/{id}/decline")
    public MessageResponse decline(@PathVariable Long id, Authentication auth) {
        User me = currentUser.require(auth);
        friendService.decline(me, id);
        return new MessageResponse("Convite recusado");
    }

    /** DELETE /api/friends/requests/{id} -> cancela convite enviado. */
    @DeleteMapping("/requests/{id}")
    public MessageResponse cancel(@PathVariable Long id, Authentication auth) {
        User me = currentUser.require(auth);
        friendService.cancel(me, id);
        return new MessageResponse("Convite cancelado");
    }

    /** GET /api/friends -> meus amigos. */
    @GetMapping
    public List<FriendResponse> friends(Authentication auth) {
        User me = currentUser.require(auth);
        return friendService.listFriends(me);
    }

    /** GET /api/friends/suggestions -> pessoas que você talvez conheça. */
    @GetMapping("/suggestions")
    public List<com.movies.backend.friend.response.SuggestionResponse> suggestions(Authentication auth) {
        User me = currentUser.require(auth);
        return friendService.suggestions(me);
    }

    /** DELETE /api/friends/{userId} -> desfaz amizade. */
    @DeleteMapping("/{userId}")
    public MessageResponse unfriend(@PathVariable Long userId, Authentication auth) {
        User me = currentUser.require(auth);
        friendService.unfriend(me, userId);
        return new MessageResponse("Amizade desfeita");
    }
}
