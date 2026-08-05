package com.movies.backend.friend.service;

import com.movies.backend.exception.ApiException;
import com.movies.backend.friend.entity.FriendRequest;
import com.movies.backend.friend.entity.FriendRequestStatus;
import com.movies.backend.friend.entity.Friendship;
import com.movies.backend.friend.repository.FriendRequestRepository;
import com.movies.backend.friend.repository.FriendshipRepository;
import com.movies.backend.friend.response.FriendRequestResponse;
import com.movies.backend.friend.response.FriendResponse;
import com.movies.backend.friend.response.WatchingNowResponse;
import com.movies.backend.notification.entity.NotificationType;
import com.movies.backend.notification.service.NotificationService;
import com.movies.backend.presence.service.PresenceService;
import com.movies.backend.user.entity.User;
import com.movies.backend.user.repository.UserRepository;
import com.movies.backend.user.response.UserMiniResponse;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regras de amizade baseada em convites: enviar, aceitar, recusar, cancelar,
 * listar amigos e desfazer amizade. Publica notificações nos eventos.
 */
@Service
public class FriendService {

    private final UserRepository userRepository;
    private final FriendRequestRepository requestRepository;
    private final FriendshipRepository friendshipRepository;
    private final NotificationService notificationService;
    private final PresenceService presenceService;

    public FriendService(UserRepository userRepository,
                         FriendRequestRepository requestRepository,
                         FriendshipRepository friendshipRepository,
                         NotificationService notificationService,
                         PresenceService presenceService) {
        this.userRepository = userRepository;
        this.requestRepository = requestRepository;
        this.friendshipRepository = friendshipRepository;
        this.notificationService = notificationService;
        this.presenceService = presenceService;
    }

    // ---------------------------------------------------------- STATUS/HELPERS
    /** Status da relação de "me" em relação a "other": self/friends/pending_out/pending_in/none. */
    @Transactional(readOnly = true)
    public String friendStatus(User me, User other) {
        if (me.getId().equals(other.getId())) {
            return "self";
        }
        if (friendshipRepository.existsByUserIdAndFriendId(me.getId(), other.getId())) {
            return "friends";
        }
        if (requestRepository.findBySenderIdAndReceiverIdAndStatus(
                me.getId(), other.getId(), FriendRequestStatus.PENDING).isPresent()) {
            return "pending_out";
        }
        if (requestRepository.findBySenderIdAndReceiverIdAndStatus(
                other.getId(), me.getId(), FriendRequestStatus.PENDING).isPresent()) {
            return "pending_in";
        }
        return "none";
    }

    public long friendCount(User user) {
        return friendshipRepository.countByUserId(user.getId());
    }

    // ------------------------------------------------------------ ENVIAR CONVITE
    @Transactional
    public FriendRequestResponse sendRequest(User me, String toUsername) {
        User target = userRepository.findByUsername(toUsername)
                .orElseThrow(() -> ApiException.notFound("Usuário não encontrado"));

        if (target.getId().equals(me.getId())) {
            throw ApiException.badRequest("Você não pode adicionar a si mesmo");
        }
        if (friendshipRepository.existsByUserIdAndFriendId(me.getId(), target.getId())) {
            throw ApiException.conflict("Vocês já são amigos");
        }
        // Se já existe convite pendente meu para ele
        if (requestRepository.findBySenderIdAndReceiverIdAndStatus(
                me.getId(), target.getId(), FriendRequestStatus.PENDING).isPresent()) {
            throw ApiException.conflict("Convite já enviado");
        }
        // Se existe convite pendente DELE para mim -> aceita automaticamente
        var reverse = requestRepository.findBySenderIdAndReceiverIdAndStatus(
                target.getId(), me.getId(), FriendRequestStatus.PENDING);
        if (reverse.isPresent()) {
            FriendRequest req = reverse.get();
            acceptInternal(req, me, target);
            return toResponse(req, target);
        }

        FriendRequest request = new FriendRequest();
        request.setSenderId(me.getId());
        request.setReceiverId(target.getId());
        request.setStatus(FriendRequestStatus.PENDING);
        FriendRequest saved = requestRepository.save(request);

        notificationService.push(target.getId(), NotificationType.FRIEND_REQUEST,
                "Novo pedido de amizade",
                me.getName() + " quer ser seu amigo",
                "/friends", me.getId());

        return toResponse(saved, target);
    }

    // ------------------------------------------------------------------ LISTAR
    @Transactional(readOnly = true)
    public List<FriendRequestResponse> listRequests(User me, String box) {
        List<FriendRequestResponse> out = new ArrayList<>();
        if ("outgoing".equalsIgnoreCase(box)) {
            for (FriendRequest r : requestRepository.findBySenderIdAndStatus(me.getId(), FriendRequestStatus.PENDING)) {
                userRepository.findById(r.getReceiverId()).ifPresent(u -> out.add(toResponse(r, u)));
            }
        } else { // incoming (default)
            for (FriendRequest r : requestRepository.findByReceiverIdAndStatus(me.getId(), FriendRequestStatus.PENDING)) {
                userRepository.findById(r.getSenderId()).ifPresent(u -> out.add(toResponse(r, u)));
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ ACEITAR
    @Transactional
    public void accept(User me, Long requestId) {
        FriendRequest req = requestRepository.findById(requestId)
                .orElseThrow(() -> ApiException.notFound("Convite não encontrado"));
        if (!req.getReceiverId().equals(me.getId())) {
            throw ApiException.forbidden("Esse convite não é para você");
        }
        if (req.getStatus() != FriendRequestStatus.PENDING) {
            throw ApiException.badRequest("Convite não está mais pendente");
        }
        User sender = userRepository.findById(req.getSenderId())
                .orElseThrow(() -> ApiException.notFound("Usuário não encontrado"));
        acceptInternal(req, me, sender);
    }

    private void acceptInternal(FriendRequest req, User receiver, User sender) {
        req.setStatus(FriendRequestStatus.ACCEPTED);
        requestRepository.save(req);
        createFriendshipBothWays(receiver.getId(), sender.getId());

        notificationService.push(sender.getId(), NotificationType.FRIEND_ACCEPTED,
                "Pedido de amizade aceito",
                receiver.getName() + " aceitou seu pedido de amizade",
                "/friends", receiver.getId());
    }

    private void createFriendshipBothWays(Long a, Long b) {
        if (!friendshipRepository.existsByUserIdAndFriendId(a, b)) {
            Friendship f1 = new Friendship();
            f1.setUserId(a);
            f1.setFriendId(b);
            friendshipRepository.save(f1);
        }
        if (!friendshipRepository.existsByUserIdAndFriendId(b, a)) {
            Friendship f2 = new Friendship();
            f2.setUserId(b);
            f2.setFriendId(a);
            friendshipRepository.save(f2);
        }
    }

    // ------------------------------------------------------------------ RECUSAR
    @Transactional
    public void decline(User me, Long requestId) {
        FriendRequest req = requestRepository.findById(requestId)
                .orElseThrow(() -> ApiException.notFound("Convite não encontrado"));
        if (!req.getReceiverId().equals(me.getId())) {
            throw ApiException.forbidden("Esse convite não é para você");
        }
        req.setStatus(FriendRequestStatus.DECLINED);
        requestRepository.save(req);
    }

    // ----------------------------------------------------------------- CANCELAR
    @Transactional
    public void cancel(User me, Long requestId) {
        FriendRequest req = requestRepository.findById(requestId)
                .orElseThrow(() -> ApiException.notFound("Convite não encontrado"));
        if (!req.getSenderId().equals(me.getId())) {
            throw ApiException.forbidden("Você só pode cancelar convites que enviou");
        }
        req.setStatus(FriendRequestStatus.CANCELLED);
        requestRepository.save(req);
    }

    // ----------------------------------------------------------- LISTAR AMIGOS
    @Transactional(readOnly = true)
    public List<FriendResponse> listFriends(User me) {
        List<FriendResponse> out = new ArrayList<>();
        for (Friendship f : friendshipRepository.findByUserId(me.getId())) {
            userRepository.findById(f.getFriendId()).ifPresent(friend -> out.add(new FriendResponse(
                    friend.getId(),
                    friend.getName(),
                    friend.getUsername(),
                    friend.getAvatarUrl(),
                    friend.getAccent(),
                    presenceService.isOnline(friend.getId()),
                    WatchingNowResponse.from(friend))));
        }
        return out;
    }

    // -------------------------------------------------------------- DESFAZER
    @Transactional
    public void unfriend(User me, Long otherUserId) {
        friendshipRepository.deleteByUserIdAndFriendId(me.getId(), otherUserId);
        friendshipRepository.deleteByUserIdAndFriendId(otherUserId, me.getId());
    }

    // -------------------------------------------------------------- MAPEAMENTO
    private FriendRequestResponse toResponse(FriendRequest req, User other) {
        return new FriendRequestResponse(
                req.getId(),
                req.getStatus().name(),
                UserMiniResponse.from(other),
                req.getCreatedAt());
    }
}
