package com.movies.backend.language.response;

/**
 * Resposta de "terminei a lição": quanto foi ganho e como ficou o perfil.
 *
 * O `xpEarned` vem separado porque a tela de fim de lição mostra "+15 XP", e
 * derivar isso no front exigiria repetir a regra de pontuação lá — duas cópias
 * da mesma regra é uma a mais do que o necessário para elas divergirem.
 */
public record LessonCompletedResponse(int xpEarned, LanguageProfileResponse profile) {}
