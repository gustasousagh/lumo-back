package com.movies.backend.compat.controller;

import com.movies.backend.compat.response.CompatibilityResponse;
import com.movies.backend.compat.service.CompatibilityService;
import com.movies.backend.security.CurrentUser;
import com.movies.backend.user.entity.User;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** Compatibilidade de gosto entre o usuário logado e outro perfil. */
@RestController
public class CompatibilityController {

    private final CompatibilityService compatibilityService;
    private final CurrentUser currentUser;

    public CompatibilityController(CompatibilityService compatibilityService, CurrentUser currentUser) {
        this.compatibilityService = compatibilityService;
        this.currentUser = currentUser;
    }

    /** GET /api/users/{username}/compatibility -> % de compatibilidade + títulos em comum. */
    @GetMapping("/api/users/{username}/compatibility")
    public CompatibilityResponse compatibility(@PathVariable String username, Authentication auth) {
        User me = currentUser.require(auth);
        return compatibilityService.compute(me, username);
    }
}
