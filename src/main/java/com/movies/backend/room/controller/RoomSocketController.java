package com.movies.backend.room.controller;

import com.movies.backend.room.dto.ChatPayload;
import com.movies.backend.room.dto.EpisodePayload;
import com.movies.backend.room.dto.PlaybackPayload;
import com.movies.backend.room.dto.ReactionPayload;
import com.movies.backend.room.entity.Room;
import com.movies.backend.room.entity.RoomChatMessage;
import com.movies.backend.room.repository.RoomChatMessageRepository;
import com.movies.backend.room.service.RoomService;
import com.movies.backend.user.entity.User;
import com.movies.backend.user.repository.UserRepository;
import java.security.Principal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

/**
 * Tempo real das salas via STOMP. O front assina "/topic/room/{id}" e envia
 * para "/app/room/{id}/...". A autenticação vem do Principal setado no CONNECT.
 */
@Controller
public class RoomSocketController {

    private final RoomService roomService;
    private final RoomChatMessageRepository chatRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public RoomSocketController(RoomService roomService,
                                RoomChatMessageRepository chatRepository,
                                UserRepository userRepository,
                                SimpMessagingTemplate messagingTemplate) {
        this.roomService = roomService;
        this.chatRepository = chatRepository;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
    }

    /** /app/room/{id}/playback -> só o HOST; persiste e retransmite. */
    @MessageMapping("/room/{id}/playback")
    public void playback(@DestinationVariable Long id,
                         @Payload PlaybackPayload payload,
                         Principal principal) {
        if (principal == null) {
            return;
        }
        Room room = roomService.updatePlayback(id, principal.getName(),
                payload.isPlaying(), payload.currentTimeSec());

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("kind", "playback");
        out.put("isPlaying", room.isPlaying());
        out.put("currentTimeSec", room.getCurrentTimeSec());
        out.put("at", Instant.now().toString());
        messagingTemplate.convertAndSend("/topic/room/" + id, (Object) out);
    }

    /** /app/room/{id}/episode -> só o HOST troca o episódio; reseta e retransmite. */
    @MessageMapping("/room/{id}/episode")
    public void episode(@DestinationVariable Long id,
                        @Payload EpisodePayload payload,
                        Principal principal) {
        if (principal == null) {
            return;
        }
        Room room = roomService.updateEpisode(id, principal.getName(),
                payload.seasonNumber(), payload.episodeNumber());

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("kind", "episode");
        out.put("seasonNumber", room.getSeasonNumber());
        out.put("episodeNumber", room.getEpisodeNumber());
        out.put("at", Instant.now().toString());
        messagingTemplate.convertAndSend("/topic/room/" + id, (Object) out);
    }

    /** /app/room/{id}/chat -> retransmite (e persiste) a mensagem. */
    @MessageMapping("/room/{id}/chat")
    public void chat(@DestinationVariable Long id,
                     @Payload ChatPayload payload,
                     Principal principal) {
        if (principal == null || payload.text() == null || payload.text().isBlank()) {
            return;
        }
        User user = userRepository.findByEmailIgnoreCase(principal.getName()).orElse(null);
        if (user == null) {
            return;
        }

        RoomChatMessage chat = new RoomChatMessage();
        chat.setRoomId(id);
        chat.setUserId(user.getId());
        chat.setText(payload.text().trim());
        RoomChatMessage saved = chatRepository.save(chat);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("kind", "chat");
        out.put("id", saved.getId());
        out.put("user", userView(user));
        out.put("text", saved.getText());
        out.put("at", saved.getCreatedAt().toString());
        messagingTemplate.convertAndSend("/topic/room/" + id, (Object) out);
    }

    /** /app/room/{id}/reaction -> emoji flutuante efêmero. */
    @MessageMapping("/room/{id}/reaction")
    public void reaction(@DestinationVariable Long id,
                         @Payload ReactionPayload payload,
                         Principal principal) {
        if (principal == null) {
            return;
        }
        User user = userRepository.findByEmailIgnoreCase(principal.getName()).orElse(null);
        if (user == null) {
            return;
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("kind", "reaction");
        out.put("emoji", payload.emoji());
        out.put("user", userView(user));
        out.put("at", Instant.now().toString());
        messagingTemplate.convertAndSend("/topic/room/" + id, (Object) out);
    }

    /** /app/room/{id}/presence -> atualiza presença e retransmite o roster. */
    @MessageMapping("/room/{id}/presence")
    public void presence(@DestinationVariable Long id, Principal principal) {
        if (principal == null) {
            return;
        }
        roomService.touchPresence(id, principal.getName());
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("kind", "presence");
        out.put("participants", roomService.participants(id));
        messagingTemplate.convertAndSend("/topic/room/" + id, (Object) out);
    }

    private Map<String, Object> userView(User user) {
        Map<String, Object> u = new LinkedHashMap<>();
        u.put("name", user.getName());
        u.put("username", user.getUsername());
        u.put("avatarUrl", user.getAvatarUrl());
        return u;
    }
}
