package com.movies.backend.user.controller;

import com.movies.backend.security.CurrentUser;
import com.movies.backend.user.dto.UpdateProfileRequest;
import com.movies.backend.user.dto.UpdateWatchingRequest;
import com.movies.backend.user.entity.User;
import com.movies.backend.user.response.MessageResponse;
import com.movies.backend.user.response.ProfileResponse;
import com.movies.backend.user.response.UserMiniResponse;
import com.movies.backend.user.service.ProfileService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de perfil de usuário. O filtro JWT já colocou o usuário no contexto;
 * usamos CurrentUser para resolvê-lo a partir do email (subject do JWT).
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final ProfileService profileService;
    private final CurrentUser currentUser;

    public UserController(ProfileService profileService, CurrentUser currentUser) {
        this.profileService = profileService;
        this.currentUser = currentUser;
    }

    /** GET /api/users/me -> perfil completo do usuário logado (inclui email). */
    @GetMapping("/me")
    public ProfileResponse me(Authentication auth) {
        User me = currentUser.require(auth);
        return profileService.me(me);
    }

    /** PATCH /api/users/me -> atualiza campos do próprio perfil. */
    @PatchMapping("/me")
    public ProfileResponse updateMe(@Valid @RequestBody UpdateProfileRequest body, Authentication auth) {
        User me = currentUser.require(auth);
        return profileService.update(me, body);
    }

    /** PATCH /api/users/me/watching -> define o "assistindo agora" do usuário. */
    @PatchMapping("/me/watching")
    public MessageResponse updateWatching(@Valid @RequestBody UpdateWatchingRequest body, Authentication auth) {
        User me = currentUser.require(auth);
        profileService.updateWatching(me, body);
        return new MessageResponse("Assistindo agora atualizado");
    }

    /** DELETE /api/users/me/watching -> limpa o "assistindo agora" do usuário. */
    @DeleteMapping("/me/watching")
    public MessageResponse clearWatching(Authentication auth) {
        User me = currentUser.require(auth);
        profileService.clearWatching(me);
        return new MessageResponse("Assistindo agora limpo");
    }

    /** GET /api/users/search?q= -> busca usuários (mínimo 2 caracteres). */
    @GetMapping("/search")
    public List<UserMiniResponse> search(@RequestParam("q") String q) {
        return profileService.search(q);
    }

    /** GET /api/users/{username} -> perfil público + friendStatus relativo a quem consulta. */
    @GetMapping("/{username}")
    public ProfileResponse byUsername(@PathVariable String username, Authentication auth) {
        User me = currentUser.require(auth);
        return profileService.getByUsername(me, username);
    }
}
