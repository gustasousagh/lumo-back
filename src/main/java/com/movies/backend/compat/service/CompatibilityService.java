package com.movies.backend.compat.service;

import com.movies.backend.compat.response.CompatibilityResponse;
import com.movies.backend.compat.response.CompatibilityResponse.SharedTitle;
import com.movies.backend.exception.ApiException;
import com.movies.backend.post.entity.Post;
import com.movies.backend.post.repository.PostRepository;
import com.movies.backend.progress.entity.WatchProgress;
import com.movies.backend.progress.repository.ProgressRepository;
import com.movies.backend.user.entity.User;
import com.movies.backend.user.repository.UserRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Calcula a "compatibilidade de gosto" entre dois usuários com base nos títulos
 * que ambos avaliaram (reviews) e/ou assistiram, e no quanto as notas concordam.
 * O app é novo, então o placar cresce conforme as pessoas usam.
 */
@Service
public class CompatibilityService {

    private final PostRepository postRepository;
    private final ProgressRepository progressRepository;
    private final UserRepository userRepository;

    public CompatibilityService(PostRepository postRepository,
                                ProgressRepository progressRepository,
                                UserRepository userRepository) {
        this.postRepository = postRepository;
        this.progressRepository = progressRepository;
        this.userRepository = userRepository;
    }

    /** Sinal de engajamento de um título: título, capa e nota (se avaliou). */
    private record Sig(String mediaType, Long mediaId, String title, String posterUrl, Integer rating) {
    }

    private Map<String, Sig> collect(Long userId) {
        Map<String, Sig> map = new LinkedHashMap<>();
        for (Post p : postRepository.findByAuthorIdOrderByCreatedAtDesc(userId)) {
            if (p.getMediaId() == null) {
                continue;
            }
            String key = p.getMediaType() + ":" + p.getMediaId();
            map.put(key, new Sig(p.getMediaType(), p.getMediaId(), p.getMediaTitle(), p.getMediaPosterUrl(), p.getRating()));
        }
        for (WatchProgress w : progressRepository.findTop30ByUserIdOrderByUpdatedAtDesc(userId)) {
            String key = w.getMediaType() + ":" + w.getMediaId();
            map.putIfAbsent(key, new Sig(w.getMediaType(), w.getMediaId(), w.getTitle(), w.getPosterUrl(), null));
        }
        return map;
    }

    @Transactional(readOnly = true)
    public CompatibilityResponse compute(User me, String username) {
        User other = userRepository.findByUsername(username)
                .orElseThrow(() -> ApiException.notFound("Usuário não encontrado"));

        Map<String, Sig> a = collect(me.getId());
        Map<String, Sig> b = collect(other.getId());

        List<SharedTitle> shared = new ArrayList<>();
        double agreeSum = 0;
        int coRated = 0;
        for (Map.Entry<String, Sig> e : a.entrySet()) {
            Sig sb = b.get(e.getKey());
            if (sb == null) {
                continue;
            }
            Sig sa = e.getValue();
            shared.add(new SharedTitle(sa.mediaType(), sa.mediaId(),
                    sa.title() != null ? sa.title() : sb.title(),
                    sa.posterUrl() != null ? sa.posterUrl() : sb.posterUrl()));
            if (sa.rating() != null && sb.rating() != null) {
                coRated++;
                agreeSum += 1.0 - Math.abs(sa.rating() - sb.rating()) / 9.0;
            }
        }

        int sharedCount = shared.size();
        int score;
        if (sharedCount == 0) {
            score = 0;
        } else {
            int base = 45 + Math.min(sharedCount, 5) * 9; // 1->54 ... 5+->90
            int bonus = coRated == 0 ? 0 : (int) Math.round((agreeSum / coRated - 0.5) * 30);
            score = Math.max(20, Math.min(99, base + bonus));
        }

        // mostra até 12 títulos em comum
        List<SharedTitle> top = shared.size() > 12 ? shared.subList(0, 12) : shared;
        return new CompatibilityResponse(score, sharedCount, label(score, sharedCount), List.copyOf(top));
    }

    private String label(int score, int sharedCount) {
        if (sharedCount == 0) {
            return "Ainda descobrindo os gostos";
        }
        if (score >= 85) {
            return "Alma gêmea de cinema 💞";
        }
        if (score >= 70) {
            return "Muita sintonia 🍿";
        }
        if (score >= 50) {
            return "Boa combinação 🎬";
        }
        return "Alguns gostos em comum ✨";
    }
}
