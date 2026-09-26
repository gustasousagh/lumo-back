package com.movies.backend.language;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.movies.backend.exception.ApiException;
import com.movies.backend.language.dto.CompleteLessonRequest;
import com.movies.backend.language.entity.LanguageProfile;
import com.movies.backend.language.repository.LanguageProfileRepository;
import com.movies.backend.language.response.LanguageProfileResponse;
import com.movies.backend.language.service.LanguageService;
import com.movies.backend.user.entity.User;
import com.movies.backend.user.repository.UserRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * A ofensiva e o XP.
 *
 * Testar isto à mão exigiria esperar dias virarem. O truque aqui é o contrário:
 * em vez de mover o relógio, movemos `lastStudyDate` para trás e verificamos o
 * que acontece na próxima leitura — que é exatamente quando a regra roda.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class LanguageServiceTest {

    @Autowired private LanguageService languageService;
    @Autowired private LanguageProfileRepository profiles;
    @Autowired private UserRepository users;

    private User eu;

    @BeforeEach
    void criarUsuario() {
        User u = new User();
        u.setName("Teste");
        u.setEmail("idiomas-" + System.nanoTime() + "@teste.com");
        u.setPassword("x");
        eu = users.save(u);
    }

    private void fingirQueEstudouEm(LocalDate dia, int ofensiva, int congelamentos) {
        LanguageProfile p = profiles.findByUserId(eu.getId()).orElseThrow();
        p.setLastStudyDate(dia);
        p.setStreakDays(ofensiva);
        p.setFreezesAvailable(congelamentos);
        p.setTodayXp(99);
        profiles.save(p);
    }

    @Test
    void perfilNovoComecaZerado() {
        LanguageProfileResponse p = languageService.getProfile(eu);
        assertThat(p.xpTotal()).isZero();
        assertThat(p.streakDays()).isZero();
        assertThat(p.completedLessonIds()).isEmpty();
        assertThat(p.estimatedCefr()).isEqualTo("A1");
        assertThat(p.wordsTotal()).isEqualTo(11_829);
    }

    @Test
    void licaoPerfeitaGanhaBonus() {
        LanguageProfileResponse p =
                languageService.completeLesson(eu, new CompleteLessonRequest(1L, 10, 10)).profile();
        assertThat(p.xpTotal()).isEqualTo(15); // 10 + 5 de bônus
        assertThat(p.streakDays()).isEqualTo(1);
        assertThat(p.completedLessonIds()).containsExactly(1L);
    }

    @Test
    void licaoComErroNaoGanhaBonus() {
        LanguageProfileResponse p =
                languageService.completeLesson(eu, new CompleteLessonRequest(1L, 8, 10)).profile();
        assertThat(p.xpTotal()).isEqualTo(10);
    }

    @Test
    void ofensivaSobeUmaVezPorDia_naoUmaPorLicao() {
        languageService.completeLesson(eu, new CompleteLessonRequest(1L, 10, 10));
        LanguageProfileResponse p =
                languageService.completeLesson(eu, new CompleteLessonRequest(2L, 10, 10)).profile();

        assertThat(p.streakDays()).isEqualTo(1);   // duas lições, um dia
        assertThat(p.xpTotal()).isEqualTo(30);     // mas o XP soma as duas
        assertThat(p.completedLessonIds()).containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    void diaSeguidoContinuaAOfensiva() {
        languageService.completeLesson(eu, new CompleteLessonRequest(1L, 10, 10));
        fingirQueEstudouEm(LocalDate.now().minusDays(1), 5, 1);

        LanguageProfileResponse p =
                languageService.completeLesson(eu, new CompleteLessonRequest(2L, 10, 10)).profile();
        assertThat(p.streakDays()).isEqualTo(6);
    }

    @Test
    void umDiaPuladoConsomeOCongelamento_semZerarAOfensiva() {
        languageService.completeLesson(eu, new CompleteLessonRequest(1L, 10, 10));
        fingirQueEstudouEm(LocalDate.now().minusDays(2), 7, 1);

        LanguageProfileResponse p = languageService.getProfile(eu);
        assertThat(p.streakDays()).isEqualTo(7);
        assertThat(p.freezesAvailable()).isZero();
    }

    @Test
    void doisDiasPuladosZeramAOfensiva() {
        languageService.completeLesson(eu, new CompleteLessonRequest(1L, 10, 10));
        fingirQueEstudouEm(LocalDate.now().minusDays(3), 7, 1);

        LanguageProfileResponse p = languageService.getProfile(eu);
        assertThat(p.streakDays()).isZero();
        assertThat(p.freezesAvailable()).isEqualTo(1); // congelamento não é gasto à toa
    }

    @Test
    void semCongelamentoUmDiaPuladoJaZera() {
        languageService.completeLesson(eu, new CompleteLessonRequest(1L, 10, 10));
        fingirQueEstudouEm(LocalDate.now().minusDays(2), 7, 0);

        assertThat(languageService.getProfile(eu).streakDays()).isZero();
    }

    @Test
    void xpDeHojeZeraNaViradaDoDia_masOTotalNao() {
        languageService.completeLesson(eu, new CompleteLessonRequest(1L, 10, 10));
        fingirQueEstudouEm(LocalDate.now().minusDays(1), 3, 1);

        LanguageProfileResponse p = languageService.getProfile(eu);
        assertThat(p.todayXp()).isZero();
        assertThat(p.xpTotal()).isEqualTo(15);
    }

    @Test
    void refazerLicaoNaoDuplicaNaTrilha() {
        languageService.completeLesson(eu, new CompleteLessonRequest(1L, 6, 10));
        LanguageProfileResponse p =
                languageService.completeLesson(eu, new CompleteLessonRequest(1L, 10, 10)).profile();
        assertThat(p.completedLessonIds()).containsExactly(1L);
    }

    @Test
    void acertosAcimaDoTotalSaoRecusados() {
        assertThatThrownBy(() ->
                languageService.completeLesson(eu, new CompleteLessonRequest(1L, 99, 10)))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void nivelEstimadoSobeComOVocabulario() {
        // 80 lições perfeitas de 10 = 800 acertos, que passa da faixa de A1.
        for (long i = 1; i <= 80; i++) {
            languageService.completeLesson(eu, new CompleteLessonRequest(i, 10, 10));
        }
        assertThat(languageService.getProfile(eu).estimatedCefr()).isEqualTo("A2");
    }

    @Test
    void resetLimpaTudo() {
        languageService.completeLesson(eu, new CompleteLessonRequest(1L, 10, 10));
        LanguageProfileResponse p = languageService.reset(eu);
        assertThat(p.xpTotal()).isZero();
        assertThat(p.streakDays()).isZero();
        assertThat(p.completedLessonIds()).isEmpty();
    }
}
