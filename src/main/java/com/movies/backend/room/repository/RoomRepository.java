package com.movies.backend.room.repository;

import com.movies.backend.room.entity.Room;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<Room, Long> {

    Optional<Room> findByCode(String code);

    boolean existsByCode(String code);

    List<Room> findByIdInAndStatus(List<Long> ids, com.movies.backend.room.entity.RoomStatus status);
}
