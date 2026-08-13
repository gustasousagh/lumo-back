package com.movies.backend.admin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * O painel administrativo é a parte mais sensível da API: por trás dele estão
 * promover admins, suspender contas, apagar conteúdo e baixar o acervo inteiro.
 * A trava do frontend é só de interface — quem garante é o backend, e é isso
 * que estes testes prendem.
 *
 * <p>A proteção existe em duas camadas de propósito (regra de URL no
 * SecurityConfig + {@code @PreAuthorize} no controller). Testar pela URL cobre
 * as duas de uma vez, que é como um atacante chegaria.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminSecurityTest {

    @Autowired private MockMvc mockMvc;

    @Test
    @DisplayName("sem token, /api/admin/** é recusado")
    void anonymousIsRejected() throws Exception {
        mockMvc.perform(get("/api/admin/overview")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/users")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/export/everything")).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("usuário comum logado não entra no painel")
    @WithMockUser(username = "alguem@lumo.test", roles = "USER")
    void plainUserIsRejected() throws Exception {
        mockMvc.perform(get("/api/admin/overview")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/users")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/admin/export/music")).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("admin passa pela barreira de autorização")
    @WithMockUser(username = "admin@lumo.test", roles = "ADMIN")
    void adminGetsThrough() throws Exception {
        // O que importa aqui é NÃO ser 401/403. Um 200 confirma de vez que a
        // regra de papel deixou passar.
        mockMvc.perform(get("/api/admin/overview")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("biblioteca e playlists continuam exigindo login")
    void libraryRequiresAuth() throws Exception {
        mockMvc.perform(get("/api/library/home")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/library/tracks")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/library/tracks/1/stream")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/playlists")).andExpect(status().isForbidden());
    }
}
