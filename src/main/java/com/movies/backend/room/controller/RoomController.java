package com.movies.backend.room.controller;

import com.movies.backend.room.dto.CreateRoomRequest;
import com.movies.backend.room.dto.RoomInviteRequest;
import com.movies.backend.room.response.RoomResponse;
import com.movies.backend.room.service.RoomService;
import com.movies.backend.security.CurrentUser;
import com.movies.backend.user.entity.User;
import com.movies.backend.user.response.MessageResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints das salas de watch party (o tempo real fica no RoomSocketController). */
@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;
    private final CurrentUser currentUser;

    public RoomController(RoomService roomService, CurrentUser currentUser) {
        this.roomService = roomService;
        this.currentUser = currentUser;
    }

    /** POST /api/rooms (foto da mídia + episódio opcional) -> cria sala. */
    @PostMapping
    public RoomResponse create(@Valid @RequestBody CreateRoomRequest body, Authentication auth) {
        User me = currentUser.require(auth);
        return roomService.create(me, body);
    }

    /** GET /api/rooms/mine -> salas ativas em que participo. */
    @GetMapping("/mine")
    public List<RoomResponse> mine(Authentication auth) {
        User me = currentUser.require(auth);
        return roomService.mine(me);
    }

    /** GET /api/rooms/{id} -> estado completo da sala. */
    @GetMapping("/{id}")
    public RoomResponse get(@PathVariable Long id, Authentication auth) {
        User me = currentUser.require(auth);
        return roomService.get(me, id);
    }

    /** POST /api/rooms/{id}/invite {username} -> convida um amigo. */
    @PostMapping("/{id}/invite")
    public MessageResponse invite(@PathVariable Long id,
                                  @Valid @RequestBody RoomInviteRequest body,
                                  Authentication auth) {
        User me = currentUser.require(auth);
        roomService.invite(me, id, body.username());
        return new MessageResponse("Convite enviado");
    }

    /** POST /api/rooms/join/{code} -> entra pela sala pelo código. */
    @PostMapping("/join/{code}")
    public RoomResponse join(@PathVariable String code, Authentication auth) {
        User me = currentUser.require(auth);
        return roomService.joinByCode(me, code);
    }

    /** POST /api/rooms/{id}/leave -> sai da sala (host saindo encerra a sala). */
    @PostMapping("/{id}/leave")
    public MessageResponse leave(@PathVariable Long id, Authentication auth) {
        User me = currentUser.require(auth);
        roomService.leave(me, id);
        return new MessageResponse("Você saiu da sala");
    }
}
