package com.movies.backend.user.controller;

import com.movies.backend.exception.ApiException;
import com.movies.backend.user.response.UserResponse;
import com.movies.backend.user.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints do usuário autenticado. O filtro JWT já colocou o usuário no
 * contexto de segurança; aqui só perguntamos "quem é o usuário logado?".
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /** GET /api/users/me -> a "tela autenticada" que devolve o usuário logado. */
    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw ApiException.unauthorized("Não autenticado");
        }
        // authentication.getName() == email (o "subject" do JWT)
        return userService.getByEmail(authentication.getName());
    }
}
