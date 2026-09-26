package com.movies.backend.language.service;

import com.movies.backend.language.dto.CompleteLessonRequest;
import com.movies.backend.language.entity.DailyActivity;
import com.movies.backend.language.entity.LanguageProfile;
import com.movies.backend.language.entity.LessonProgress;
import com.movies.backend.language.repository.DailyActivityRepository;
import com.movies.backend.language.repository.LanguageProfileRepository;
import com.movies.backend.language.repository.LessonProgressRepository;
import com.movies.backend.language.response.LanguageProfileResponse;
import com.movies.backend.language.response.LessonCompletedResponse;
import com.movies.backend.exception.ApiException;
import com.movies.backend.user.entity.User;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regras do curso de idiomas: XP, ofensiva e lições concluídas.
 *
 * O conteúdo do curso não passa por aqui — ele é estático, servido pelo front.
 * Este serviço cuida só do que é de cada pessoa, que é justamente o que não
 * pode morar no navegador.
 */
@Service
public class LanguageService {

    /** XP por lição, e o bônus de gabaritar. Os mesmos números do front. */
    private static final int XP_PER_LESSON = 10;
    private static final int XP_PERFECT_BONUS = 5;

    /** Tamanho do acervo gerado. Sobe junto quando o conteúdo crescer. */
    private static final int WORDS_TOTAL = 11_829;

    /** Quantos dias de atividade o mapa mostra. */
    private static final int ACTIVITY_DAYS = 365;

    private final LanguageProfileRepository profiles;
    private final LessonProgressRepository lessons;
    private final DailyActivityRepository activity;

    public LanguageService(LanguageProfileRepository profiles,
                           LessonProgressRepository lessons,
                           DailyActivityRepository activity) {
        this.profiles = profiles;
        this.lessons = lessons;
        this.activity = activity;
    }

    @Transactional
    public LanguageProfileResponse getProfile(User me) {
        return respond(carregarNormalizado(me));
    }

    /**
     * Fecha uma lição: credita XP, mexe na ofensiva e marca como concluída.
     *
     * O XP é calculado AQUI a partir de acertos e total. O front não manda
     * quantos pontos quer ganhar, porque isso seria pedir para ser burlado.
     */
    @Transactional
    public LessonCompletedResponse completeLesson(User me, CompleteLessonRequest req) {
        if (req.correct() > req.total()) {
            throw ApiException.badRequest("Acertos não podem passar do total de exercícios.");
        }

        LanguageProfile perfil = carregarNormalizado(me);
        LocalDate hoje = LocalDate.now();

        // A ofensiva sobe uma vez por dia, na primeira lição — não a cada lição.
        boolean primeiraDoDia = !hoje.equals(perfil.getLastStudyDate());
        if (primeiraDoDia) {
            perfil.setStreakDays(perfil.getStreakDays() + 1);
            perfil.setLongestStreak(Math.max(perfil.getLongestStreak(), perfil.getStreakDays()));
        }

        boolean gabaritou = req.correct() == req.total();
        int xp = XP_PER_LESSON + (gabaritou ? XP_PERFECT_BONUS : 0);

        perfil.setXpTotal(perfil.getXpTotal() + xp);
        perfil.setTodayXp(perfil.getTodayXp() + xp);
        perfil.setLastStudyDate(hoje);
        perfil.setWordsMastered(Math.min(perfil.getWordsMastered() + req.correct(), WORDS_TOTAL));
        profiles.save(perfil);

        registrarAtividade(me.getId(), hoje, xp);
        registrarLicao(me.getId(), req);

        return new LessonCompletedResponse(xp, respond(perfil));
    }

    @Transactional
    public LanguageProfileResponse setGoal(User me, int meta) {
        LanguageProfile perfil = carregarNormalizado(me);
        perfil.setDailyGoalXp(meta);
        profiles.save(perfil);
        return respond(perfil);
    }

    /**
     * Recomeça do zero. Existe porque testar a trilha sem isso é impraticável.
     *
     * Zera os campos do perfil em vez de apagá-lo e criar outro: na mesma
     * transação o DELETE só vai ao banco no fim, então o INSERT novo esbarraria
     * no índice único de userId antes de a linha antiga sair.
     */
    @Transactional
    public LanguageProfileResponse reset(User me) {
        lessons.deleteByUserId(me.getId());
        activity.deleteByUserId(me.getId());

        LanguageProfile perfil = profiles.findByUserId(me.getId())
                .orElseGet(() -> criar(me.getId()));
        perfil.setXpTotal(0);
        perfil.setTodayXp(0);
        perfil.setStreakDays(0);
        perfil.setLongestStreak(0);
        perfil.setFreezesAvailable(1);
        perfil.setWordsMastered(0);
        perfil.setLastStudyDate(null);
        return respond(profiles.save(perfil));
    }

    // -----------------------------------------------------------------------

    /**
     * Carrega o perfil já com a virada do dia aplicada.
     *
     * A ofensiva decai na LEITURA, não por tarefa agendada: quem sumiu por uma
     * semana descobre a sequência zerada quando volta, e não há job rodando de
     * madrugada num Raspberry para isso.
     */
    private LanguageProfile carregarNormalizado(User me) {
        LanguageProfile perfil = profiles.findByUserId(me.getId())
                .orElseGet(() -> criar(me.getId()));

        LocalDate hoje = LocalDate.now();
        LocalDate ultimo = perfil.getLastStudyDate();
        if (ultimo == null || hoje.equals(ultimo)) {
            return perfil;
        }

        perfil.setTodayXp(0);

        long dias = ChronoUnit.DAYS.between(ultimo, hoje);
        if (dias >= 2) {
            // Exatamente um dia pulado consome o congelamento; mais que isso,
            // nem o congelamento segura.
            if (dias == 2 && perfil.getFreezesAvailable() > 0) {
                perfil.setFreezesAvailable(perfil.getFreezesAvailable() - 1);
            } else {
                perfil.setStreakDays(0);
            }
        }
        return profiles.save(perfil);
    }

    private LanguageProfile criar(Long userId) {
        LanguageProfile perfil = new LanguageProfile();
        perfil.setUserId(userId);
        return profiles.save(perfil);
    }

    private void registrarAtividade(Long userId, LocalDate dia, int xp) {
        DailyActivity registro = activity.findByUserIdAndDay(userId, dia)
                .orElseGet(() -> {
                    DailyActivity novo = new DailyActivity();
                    novo.setUserId(userId);
                    novo.setDay(dia);
                    return novo;
                });
        registro.setXp(registro.getXp() + xp);
        activity.save(registro);
    }

    private void registrarLicao(Long userId, CompleteLessonRequest req) {
        LessonProgress registro = lessons.findByUserIdAndLessonId(userId, req.lessonId())
                .orElseGet(() -> {
                    LessonProgress novo = new LessonProgress();
                    novo.setUserId(userId);
                    novo.setLessonId(req.lessonId());
                    novo.setTimesCompleted(0);
                    return novo;
                });
        registro.setCorrect(req.correct());
        registro.setTotal(req.total());
        registro.setCompletedAt(Instant.now());
        registro.setTimesCompleted(registro.getTimesCompleted() + 1);
        lessons.save(registro);
    }

    private LanguageProfileResponse respond(LanguageProfile perfil) {
        List<Long> concluidas = lessons.findLessonIdsByUserId(perfil.getUserId());

        Map<String, Integer> mapa = new LinkedHashMap<>();
        for (DailyActivity dia : activity.findByUserIdAndDayGreaterThanEqualOrderByDayAsc(
                perfil.getUserId(), LocalDate.now().minusDays(ACTIVITY_DAYS))) {
            mapa.put(dia.getDay().toString(), dia.getXp());
        }

        return LanguageProfileResponse.from(
                perfil, WORDS_TOTAL, estimarNivel(perfil.getWordsMastered()), concluidas, mapa);
    }

    /**
     * Nível estimado pela quantidade de palavras dominadas, nas mesmas faixas
     * de frequência que o gerador usa para classificar o vocabulário.
     */
    private String estimarNivel(int dominadas) {
        if (dominadas < 750) return "A1";
        if (dominadas < 1500) return "A2";
        if (dominadas < 2500) return "B1";
        if (dominadas < 4000) return "B2";
        return "C1";
    }
}
