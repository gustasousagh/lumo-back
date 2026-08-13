package com.movies.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Roda uma vez por requisição: lê o header "Authorization: Bearer <token>",
 * valida o JWT e, se ok, coloca o usuário no contexto de segurança do Spring.
 * A partir daí endpoints protegidos sabem quem está logado.
 *
 * <p>Também aceita o token na query string (<code>?token=</code>), mas só nas
 * rotas de arquivo da biblioteca. Isso existe porque {@code <audio>}, {@code <img>}
 * e o visualizador de PDF do navegador buscam a URL sozinhos e não têm como
 * mandar header Authorization — sem isso, tocar uma música exigiria baixar o
 * arquivo inteiro por fetch antes de dar play.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /** Prefixos onde o ?token= é aceito. Fora daqui, só header. */
    private static final String[] QUERY_TOKEN_PATHS = {
            "/api/library/", "/api/playlists/", "/api/admin/export"
    };

    private final JwtService jwtService;
    private final AppUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtService jwtService, AppUserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String token = resolveToken(request);
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            if (jwtService.isValid(token)
                    && SecurityContextHolder.getContext().getAuthentication() == null) {
                String email = jwtService.extractEmail(token);
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                // Conta suspensa/não confirmada não autentica: sem isso, banir
                // alguém só teria efeito no próximo login, não nos tokens vivos.
                if (userDetails.isEnabled() && userDetails.isAccountNonLocked()) {
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        } catch (Exception ex) {
            // token inválido/usuário não encontrado -> segue sem autenticar
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    /** Header Authorization; nas rotas de arquivo, cai para o ?token= da query. */
    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        String path = request.getRequestURI();
        for (String allowed : QUERY_TOKEN_PATHS) {
            if (path.startsWith(allowed)) {
                String param = request.getParameter("token");
                return (param != null && !param.isBlank()) ? param : null;
            }
        }
        return null;
    }
}
