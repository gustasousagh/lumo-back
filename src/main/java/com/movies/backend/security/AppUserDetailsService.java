package com.movies.backend.security;

import com.movies.backend.user.entity.User;
import com.movies.backend.user.repository.UserRepository;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Ponte entre o nosso User (entity) e o modelo de usuário do Spring Security.
 * O filtro JWT usa isto para carregar o usuário a partir do email do token.
 */
@Service
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public AppUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));

        // A authority vem do papel do usuário: é o que faz hasRole('ADMIN')
        // funcionar nos endpoints de /api/admin/**.
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())
                .authorities(List.of(new SimpleGrantedAuthority(user.role().authority())))
                .disabled(!user.isEnabled())
                .accountLocked(user.isSuspended())
                .build();
    }
}
