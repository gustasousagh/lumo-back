package com.movies.backend.room.repository;

import com.movies.backend.room.entity.RoomChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomChatMessageRepository extends JpaRepository<RoomChatMessage, Long> {
}
