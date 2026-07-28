package com.movies.backend.user.controller;

import com.movies.backend.user.dto.ConfirmRequest;
import com.movies.backend.user.dto.ForgotPasswordRequest;
import com.movies.backend.user.dto.LoginRequest;
import com.movies.backend.user.dto.RegisterRequest;
import com.movies.backend.user.dto.ResendConfirmationRequest;
import com.movies.backend.user.dto.ResetPasswordRequest;
import com.movies.backend.user.response.AuthResponse;
import com.movies.backend.user.response.MessageResponse;
import com.movies.backend.user.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller = a "porta de entrada" HTTP. Só recebe a requisição, valida o DTO
 * (@Valid) e delega para o Service. Não contém regra de negócio.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public MessageResponse register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return new MessageResponse("Conta criada! Enviamos um link de confirmação para o seu email.");
    }

    @PostMapping("/confirm")
    public MessageResponse confirm(@Valid @RequestBody ConfirmRequest request) {
        authService.confirmEmail(request.token());
        return new MessageResponse("Email confirmado com sucesso! Agora é só entrar.");
    }

    @PostMapping("/resend-confirmation")
    public MessageResponse resendConfirmation(@Valid @RequestBody ResendConfirmationRequest request) {
        authService.resendConfirmation(request.email());
        return new MessageResponse("Se houver uma conta pendente com esse email, reenviamos o link.");
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/forgot-password")
    public MessageResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.email());
        return new MessageResponse("Se houver uma conta com esse email, enviamos o link de redefinição.");
    }

    @PostMapping("/reset-password")
    public MessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return new MessageResponse("Senha redefinida com sucesso! Já pode entrar com a nova senha.");
    }
}
