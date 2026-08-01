package com.movies.backend.room.repository;

import com.movies.backend.room.entity.RoomInvite;
import com.movies.backend.room.entity.RoomInviteStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomInviteRepository extends JpaRepository<RoomInvite, Long> {

    Optional<RoomInvite> findByRoomIdAndInviteeIdAndStatus(Long roomId, Long inviteeId, RoomInviteStatus status);
}
