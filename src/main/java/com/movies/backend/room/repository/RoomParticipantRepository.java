package com.movies.backend.room.repository;

import com.movies.backend.room.entity.RoomParticipant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomParticipantRepository extends JpaRepository<RoomParticipant, Long> {

    List<RoomParticipant> findByRoomId(Long roomId);

    List<RoomParticipant> findByUserId(Long userId);

    Optional<RoomParticipant> findByRoomIdAndUserId(Long roomId, Long userId);
}
