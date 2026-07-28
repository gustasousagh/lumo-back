package com.movies.backend.user.service;

import com.movies.backend.config.AppProperties;
import com.movies.backend.email.EmailService;
import com.movies.backend.exception.ApiException;
import com.movies.backend.security.JwtService;
import com.movies.backend.user.dto.LoginRequest;
import com.movies.backend.user.dto.RegisterRequest;
import com.movies.backend.user.dto.ResetPasswordRequest;
import com.movies.backend.user.entity.Token;
import com.movies.backend.user.entity.TokenType;
import com.movies.backend.user.entity.User;
import com.movies.backend.user.repository.TokenRepository;
import com.movies.backend.user.repository.UserRepository;
import com.movies.backend.user.response.AuthResponse;
import com.movies.backend.user.response.UserResponse;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Camada de SERVICE = onde ficam as regras de negócio da autenticação.
 * Fluxo geral das camadas:
 *   Controller (web)  ->  Service (regras)  ->  Repository (banco)  ->  Entity
 * O Controller nunca fala com o Repository direto; passa pelo Service.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final AppProperties appProperties;

    public AuthService(UserRepository userRepository,
                       TokenRepository tokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       EmailService emailService,
                       AppProperties appProperties) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.emailService = emailService;
        this.appProperties = appProperties;
    }

    // ---------------------------------------------------------------- REGISTRO
    @Transactional
    public void register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("Já existe uma conta com esse email");
        }

        User user = new User();
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setEnabled(false);
        userRepository.save(user);

        sendVerification(user);
    }

    @Transactional
    public void resendConfirmation(String rawEmail) {
        String email = rawEmail.trim().toLowerCase();
        // Resposta genérica lá no controller; aqui só agimos se fizer sentido.
        userRepository.findByEmailIgnoreCase(email).ifPresent(user -> {
            if (!user.isEnabled()) {
                sendVerification(user);
            }
        });
    }

    // ------------------------------------------------------------ CONFIRMAÇÃO
    @Transactional
    public void confirmEmail(String tokenValue) {
        Token token = consumeToken(tokenValue, TokenType.EMAIL_VERIFICATION);
        User user = token.getUser();
        user.setEnabled(true);
        userRepository.save(user);
    }

    // ------------------------------------------------------------------- LOGIN
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> ApiException.unauthorized("Email ou senha inválidos"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw ApiException.unauthorized("Email ou senha inválidos");
        }
        if (!user.isEnabled()) {
            throw ApiException.forbidden("Confirme seu email antes de entrar. Verifique sua caixa de entrada.");
        }

        String jwt = jwtService.generateToken(user.getEmail());
        return new AuthResponse(jwt, UserResponse.from(user));
    }

    // ---------------------------------------------------- ESQUECI/RESET SENHA
    @Transactional
    public void forgotPassword(String rawEmail) {
        String email = rawEmail.trim().toLowerCase();
        userRepository.findByEmailIgnoreCase(email).ifPresent(user -> {
            tokenRepository.deleteByUserAndType(user, TokenType.PASSWORD_RESET);
            Token token = createToken(user, TokenType.PASSWORD_RESET,
                    appProperties.getTokens().getResetExpirationMinutes());
            String link = appProperties.getFrontendUrl() + "/reset-password?token=" + token.getValue();
            emailService.sendPasswordResetEmail(user, link);
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        Token token = consumeToken(request.token(), TokenType.PASSWORD_RESET);
        User user = token.getUser();
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        // Se por acaso a conta ainda não estava confirmada, um reset bem-sucedido também confirma.
        user.setEnabled(true);
        userRepository.save(user);
    }

    // --------------------------------------------------------------- HELPERS
    private void sendVerification(User user) {
        tokenRepository.deleteByUserAndType(user, TokenType.EMAIL_VERIFICATION);
        Token token = createToken(user, TokenType.EMAIL_VERIFICATION,
                appProperties.getTokens().getVerificationExpirationMinutes());
        String link = appProperties.getFrontendUrl() + "/confirm?token=" + token.getValue();
        emailService.sendVerificationEmail(user, link);
    }

    private Token createToken(User user, TokenType type, long expirationMinutes) {
        Token token = new Token();
        token.setValue(UUID.randomUUID().toString());
        token.setType(type);
        token.setUser(user);
        token.setExpiresAt(Instant.now().plus(expirationMinutes, ChronoUnit.MINUTES));
        token.setUsed(false);
        return tokenRepository.save(token);
    }

    /** Valida o token (existe, do tipo certo, não usado, não expirado) e o marca como usado. */
    private Token consumeToken(String value, TokenType type) {
        Token token = tokenRepository.findByValueAndType(value, type)
                .orElseThrow(() -> ApiException.badRequest("Link inválido ou já utilizado"));
        if (token.isUsed()) {
            throw ApiException.badRequest("Este link já foi utilizado");
        }
        if (token.isExpired()) {
            throw ApiException.badRequest("Este link expirou. Solicite um novo.");
        }
        token.setUsed(true);
        tokenRepository.save(token);
        return token;
    }
}
