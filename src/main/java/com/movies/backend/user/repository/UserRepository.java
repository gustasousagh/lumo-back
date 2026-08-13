package com.movies.backend.user.repository;

import com.movies.backend.user.entity.User;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repository = camada de acesso ao banco. O Spring Data cria a implementação
 * sozinho a partir do nome dos métodos. O Service chama esses métodos.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    /** Busca por apelido OU nome (usada na barra de busca de usuários). */
    List<User> findTop20ByUsernameContainingIgnoreCaseOrNameContainingIgnoreCase(String a, String b);

    // ------------------------------------------------------------------ ADMIN
    long countByRole(String role);

    long countBySuspended(boolean suspended);

    long countByEnabled(boolean enabled);

    long countByCreatedAtAfter(Instant since);

    /**
     * Listagem paginada do painel, com busca opcional. Termo vazio devolve tudo
     * — é o comportamento esperado quando o admin abre a tela sem digitar nada.
     */
    @Query("""
            select u from User u
            where :q = ''
               or lower(u.name) like lower(concat('%', :q, '%'))
               or lower(u.email) like lower(concat('%', :q, '%'))
               or lower(coalesce(u.username, '')) like lower(concat('%', :q, '%'))
            """)
    Page<User> adminSearch(@Param("q") String q, Pageable pageable);

    /** Instantes dos cadastros na janela; o agrupamento por dia é feito em Java. */
    @Query("select u.createdAt from User u where u.createdAt >= :since")
    List<Instant> createdAtSince(@Param("since") Instant since);
}
