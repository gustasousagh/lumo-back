package com.movies.backend.config;

import com.movies.backend.security.JwtService;
import com.movies.backend.user.repository.UserRepository;
import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;

/**
 * Configuração do WebSocket (STOMP) para o tempo real (playback, chat, presença,
 * notificações). Autentica no CONNECT lendo o header "Authorization: Bearer".
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final AppProperties appProperties;

    public WebSocketConfig(JwtService jwtService, UserRepository userRepository, AppProperties appProperties) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.appProperties = appProperties;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket puro (clientes STOMP nativos)
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(appProperties.getFrontendUrl());
        // Fallback com SockJS (para browsers sem WebSocket)
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(appProperties.getFrontendUrl())
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(org.springframework.messaging.simp.config.ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor =
                        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String authHeader = accessor.getFirstNativeHeader("Authorization");
                    if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        String token = authHeader.substring(7);
                        if (jwtService.isValid(token)) {
                            String email = jwtService.extractEmail(token);
                            userRepository.findByEmailIgnoreCase(email).ifPresent(user -> {
                                // Principal com name == email (usado por convertAndSendToUser)
                                UsernamePasswordAuthenticationToken principal =
                                        new UsernamePasswordAuthenticationToken(email, null, List.of());
                                accessor.setUser(principal);
                            });
                        }
                    }
                }
                return message;
            }
        });
    }
}
