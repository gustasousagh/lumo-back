package com.movies.backend.media.gocine;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * O motivo principal de trazer o catálogo pra API foi fechar o buraco: no Next
 * estas rotas eram públicas e qualquer um podia usar o acesso ao GoCine.
 * Aqui elas exigem JWT como o resto da API.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MediaBrowseSecurityTest {

    @Autowired private MockMvc mockMvc;

    // 403 (e não 401) é o que a API inteira devolve sem token: a cadeia é
    // stateless e não declara authenticationEntryPoint. O que importa aqui é que
    // o catálogo passou a seguir a mesma regra do resto.
    @Test
    @DisplayName("catálogo sem token é recusado")
    void catalogRequiresAuth() throws Exception {
        mockMvc.perform(get("/api/media/home")).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/media/search").param("q", "matrix"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/media/detail").param("type", "movie").param("id", "3"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/media/stream").param("type", "movie").param("id", "3"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("health continua público")
    void healthStaysPublic() throws Exception {
        mockMvc.perform(get("/api/health")).andExpect(status().isOk());
    }
}
