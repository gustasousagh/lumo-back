package com.movies.backend.progress.controller;

import com.movies.backend.progress.dto.UpdateProgressRequest;
import com.movies.backend.progress.response.ProgressResponse;
import com.movies.backend.progress.service.ProgressService;
import com.movies.backend.security.CurrentUser;
import com.movies.backend.user.entity.User;
import com.movies.backend.user.response.MessageResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints de "continue assistindo" do usuário logado. */
@RestController
@RequestMapping("/api/progress")
public class ProgressController {

    private final ProgressService progressService;
    private final CurrentUser currentUser;

    public ProgressController(ProgressService progressService, CurrentUser currentUser) {
        this.progressService = progressService;
        this.currentUser = currentUser;
    }

    /** PUT /api/progress -> cria/atualiza o progresso e devolve o item. */
    @PutMapping
    public ProgressResponse upsert(@Valid @RequestBody UpdateProgressRequest body, Authentication auth) {
        User me = currentUser.require(auth);
        return progressService.upsert(me, body);
    }

    /** GET /api/progress -> meus itens recentes (mais novos primeiro, até 30). */
    @GetMapping
    public List<ProgressResponse> list(Authentication auth) {
        User me = currentUser.require(auth);
        return progressService.list(me);
    }

    /** DELETE /api/progress/{id} -> apaga um item meu. */
    @DeleteMapping("/{id}")
    public MessageResponse delete(@PathVariable Long id, Authentication auth) {
        User me = currentUser.require(auth);
        progressService.delete(me, id);
        return new MessageResponse("Item removido");
    }
}
