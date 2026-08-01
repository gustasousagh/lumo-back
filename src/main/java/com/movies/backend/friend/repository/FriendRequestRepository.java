package com.movies.backend.friend.repository;

import com.movies.backend.friend.entity.FriendRequest;
import com.movies.backend.friend.entity.FriendRequestStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FriendRequestRepository extends JpaRepository<FriendRequest, Long> {

    List<FriendRequest> findBySenderIdAndStatus(Long senderId, FriendRequestStatus status);

    List<FriendRequest> findByReceiverIdAndStatus(Long receiverId, FriendRequestStatus status);

    Optional<FriendRequest> findBySenderIdAndReceiverIdAndStatus(
            Long senderId, Long receiverId, FriendRequestStatus status);
}
