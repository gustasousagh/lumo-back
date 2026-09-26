package com.movies.backend.language.controller;

import com.movies.backend.language.dto.CompleteLessonRequest;
import com.movies.backend.language.dto.SetGoalRequest;
import com.movies.backend.language.response.LanguageProfileResponse;
import com.movies.backend.language.response.LessonCompletedResponse;
import com.movies.backend.language.service.LanguageService;
import com.movies.backend.security.CurrentUser;
import com.movies.backend.user.entity.User;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Progresso do usuário no curso de idiomas.
 *
 * Não há endpoint de conteúdo aqui: o curso é estático e vem do front. Estes
 * endpoints existem para o progresso deixar de morrer no localStorage.
 */
@RestController
@RequestMapping("/api/language")
public class LanguageController {

    private final LanguageService languageService;
    private final CurrentUser currentUser;

    public LanguageController(LanguageService languageService, CurrentUser currentUser) {
        this.languageService = languageService;
        this.currentUser = currentUser;
    }

    /** GET /api/language/me -> XP, ofensiva, lições concluídas e atividade. */
    @GetMapping("/me")
    public LanguageProfileResponse me(Authentication auth) {
        return languageService.getProfile(currentUser.require(auth));
    }

    /** POST /api/language/lessons/complete -> fecha a lição e devolve o perfil. */
    @PostMapping("/lessons/complete")
    public LessonCompletedResponse complete(@Valid @RequestBody CompleteLessonRequest body,
                                            Authentication auth) {
        return languageService.completeLesson(currentUser.require(auth), body);
    }

    /** PATCH /api/language/goal -> muda a meta diária. */
    @PatchMapping("/goal")
    public LanguageProfileResponse goal(@Valid @RequestBody SetGoalRequest body,
                                        Authentication auth) {
        return languageService.setGoal(currentUser.require(auth), body.dailyGoalXp());
    }

    /** POST /api/language/reset -> recomeça do zero. */
    @PostMapping("/reset")
    public LanguageProfileResponse reset(Authentication auth) {
        return languageService.reset(currentUser.require(auth));
    }
}
