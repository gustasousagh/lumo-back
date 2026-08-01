package com.movies.backend.list.controller;

import com.movies.backend.list.dto.CreateListRequest;
import com.movies.backend.list.response.ListDetailResponse;
import com.movies.backend.list.response.ListSummaryResponse;
import com.movies.backend.list.service.ListService;
import com.movies.backend.media.dto.MediaSnapshotRequest;
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
import org.springframework.web.bind.annotation.RestController;

/** Endpoints das listas de mídias. */
@RestController
@RequestMapping("/api")
public class ListController {

    private final ListService listService;
    private final CurrentUser currentUser;

    public ListController(ListService listService, CurrentUser currentUser) {
        this.listService = listService;
        this.currentUser = currentUser;
    }

    /** POST /api/lists {title} -> cria lista. */
    @PostMapping("/lists")
    public ListSummaryResponse create(@Valid @RequestBody CreateListRequest body, Authentication auth) {
        User me = currentUser.require(auth);
        return listService.create(me, body);
    }

    /** GET /api/lists -> minhas listas (com itemCount e até 4 capas). */
    @GetMapping("/lists")
    public List<ListSummaryResponse> myLists(Authentication auth) {
        User me = currentUser.require(auth);
        return listService.myLists(me);
    }

    /** GET /api/lists/{id} -> lista com itens resolvidos do catálogo. */
    @GetMapping("/lists/{id}")
    public ListDetailResponse detail(@PathVariable Long id) {
        return listService.detail(id);
    }

    /** DELETE /api/lists/{id} -> remove a lista. */
    @DeleteMapping("/lists/{id}")
    public MessageResponse delete(@PathVariable Long id, Authentication auth) {
        User me = currentUser.require(auth);
        listService.delete(me, id);
        return new MessageResponse("Lista removida");
    }

    /** POST /api/lists/{id}/items (foto de mídia) -> upserta catálogo e adiciona item. */
    @PostMapping("/lists/{id}/items")
    public ListDetailResponse addItem(@PathVariable Long id,
                                      @Valid @RequestBody MediaSnapshotRequest body,
                                      Authentication auth) {
        User me = currentUser.require(auth);
        return listService.addItem(me, id, body);
    }

    /** DELETE /api/lists/{id}/items/{itemId} -> remove item. */
    @DeleteMapping("/lists/{id}/items/{itemId}")
    public MessageResponse removeItem(@PathVariable Long id,
                                      @PathVariable Long itemId,
                                      Authentication auth) {
        User me = currentUser.require(auth);
        listService.removeItem(me, id, itemId);
        return new MessageResponse("Item removido da lista");
    }

    /** GET /api/users/{username}/lists -> listas públicas de um usuário. */
    @GetMapping("/users/{username}/lists")
    public List<ListSummaryResponse> userLists(@PathVariable String username) {
        return listService.listsByUsername(username);
    }
}
