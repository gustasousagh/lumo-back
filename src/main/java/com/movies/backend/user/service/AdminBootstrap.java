package com.movies.backend.user.service;

import com.movies.backend.config.AppProperties;
import com.movies.backend.user.entity.Role;
import com.movies.backend.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Promove a ADMIN, no boot, os emails de {@code app.admin-emails} (env
 * ADMIN_EMAILS). Resolve o problema do ovo e da galinha: o painel admin só é
 * acessível por admin, e o cadastro sempre cria usuário comum — sem isto o
 * primeiro admin só nasceria com um UPDATE na mão no banco.
 *
 * <p>Só promove; nunca rebaixa. Tirar o admin de alguém é ação do painel.
 */
@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserRepository userRepository;
    private final AppProperties properties;

    public AdminBootstrap(UserRepository userRepository, AppProperties properties) {
        this.userRepository = userRepository;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        for (String raw : properties.getAdminEmails()) {
            String email = raw == null ? "" : raw.trim();
            if (email.isEmpty()) {
                continue;
            }
            userRepository.findByEmailIgnoreCase(email).ifPresentOrElse(user -> {
                if (user.role() != Role.ADMIN) {
                    user.setRole(Role.ADMIN);
                    userRepository.save(user);
                    log.info("Usuário {} promovido a ADMIN (app.admin-emails)", email);
                }
            }, () -> log.warn("app.admin-emails lista {}, mas não existe conta com esse email", email));
        }
    }
}
