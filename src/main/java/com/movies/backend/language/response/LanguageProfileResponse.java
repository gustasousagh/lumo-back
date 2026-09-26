package com.movies.backend.language.response;

import com.movies.backend.language.entity.LanguageProfile;
import java.util.List;
import java.util.Map;

/**
 * O progresso como o front recebe. Os nomes batem com os do TypeScript para a
 * resposta cair direto no estado da tela, sem camada de tradução.
 */
public record LanguageProfileResponse(
        long xpTotal,
        int todayXp,
        int dailyGoalXp,
        int streakDays,
        int longestStreak,
        int freezesAvailable,
        int wordsMastered,
        int wordsTotal,
        String estimatedCefr,
        List<Long> completedLessonIds,
        Map<String, Integer> activity
) {
    public static LanguageProfileResponse from(
            LanguageProfile p, int wordsTotal, String cefr,
            List<Long> completed, Map<String, Integer> activity) {
        return new LanguageProfileResponse(
                p.getXpTotal(), p.getTodayXp(), p.getDailyGoalXp(),
                p.getStreakDays(), p.getLongestStreak(), p.getFreezesAvailable(),
                p.getWordsMastered(), wordsTotal, cefr, completed, activity);
    }
}
