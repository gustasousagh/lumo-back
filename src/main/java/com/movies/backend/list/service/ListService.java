package com.movies.backend.list.service;

import com.movies.backend.exception.ApiException;
import com.movies.backend.list.dto.CreateListRequest;
import com.movies.backend.list.entity.ListItem;
import com.movies.backend.list.entity.MovieList;
import com.movies.backend.list.repository.ListItemRepository;
import com.movies.backend.list.repository.MovieListRepository;
import com.movies.backend.list.response.ListDetailResponse;
import com.movies.backend.list.response.ListItemResponse;
import com.movies.backend.list.response.ListSummaryResponse;
import com.movies.backend.media.dto.MediaSnapshotRequest;
import com.movies.backend.media.entity.MediaCatalog;
import com.movies.backend.media.repository.MediaCatalogRepository;
import com.movies.backend.media.response.MediaCatalogResponse;
import com.movies.backend.media.service.MediaCatalogService;
import com.movies.backend.user.entity.User;
import com.movies.backend.user.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Regras das listas de mídias. */
@Service
public class ListService {

    private final MovieListRepository listRepository;
    private final ListItemRepository itemRepository;
    private final MediaCatalogRepository catalogRepository;
    private final MediaCatalogService catalogService;
    private final UserRepository userRepository;

    public ListService(MovieListRepository listRepository,
                       ListItemRepository itemRepository,
                       MediaCatalogRepository catalogRepository,
                       MediaCatalogService catalogService,
                       UserRepository userRepository) {
        this.listRepository = listRepository;
        this.itemRepository = itemRepository;
        this.catalogRepository = catalogRepository;
        this.catalogService = catalogService;
        this.userRepository = userRepository;
    }

    @Transactional
    public ListSummaryResponse create(User me, CreateListRequest req) {
        MovieList list = new MovieList();
        list.setOwnerId(me.getId());
        list.setTitle(req.title().trim());
        MovieList saved = listRepository.save(list);
        return summary(saved);
    }

    @Transactional(readOnly = true)
    public List<ListSummaryResponse> myLists(User me) {
        return listRepository.findByOwnerIdOrderByCreatedAtDesc(me.getId())
                .stream().map(this::summary).toList();
    }

    @Transactional(readOnly = true)
    public List<ListSummaryResponse> listsByUsername(String username) {
        User owner = userRepository.findByUsername(username)
                .orElseThrow(() -> ApiException.notFound("Usuário não encontrado"));
        return listRepository.findByOwnerIdOrderByCreatedAtDesc(owner.getId())
                .stream().map(this::summary).toList();
    }

    @Transactional(readOnly = true)
    public ListDetailResponse detail(Long listId) {
        MovieList list = listRepository.findById(listId)
                .orElseThrow(() -> ApiException.notFound("Lista não encontrada"));
        List<ListItemResponse> items = new ArrayList<>();
        for (ListItem item : itemRepository.findByListIdOrderByCreatedAtAsc(listId)) {
            catalogRepository.findById(item.getCatalogId()).ifPresent(catalog ->
                    items.add(new ListItemResponse(
                            item.getId(),
                            item.getCatalogId(),
                            MediaCatalogResponse.from(catalog),
                            item.getCreatedAt())));
        }
        return new ListDetailResponse(list.getId(), list.getTitle(), list.getCreatedAt(), items);
    }

    @Transactional
    public void delete(User me, Long listId) {
        MovieList list = requireOwned(me, listId);
        itemRepository.deleteByListId(list.getId());
        listRepository.delete(list);
    }

    @Transactional
    public ListDetailResponse addItem(User me, Long listId, MediaSnapshotRequest snapshot) {
        MovieList list = requireOwned(me, listId);
        MediaCatalog catalog = catalogService.upsert(snapshot);
        // dedupe: só adiciona se ainda não existir no la lista
        if (itemRepository.findByListIdAndCatalogId(list.getId(), catalog.getId()).isEmpty()) {
            ListItem item = new ListItem();
            item.setListId(list.getId());
            item.setOwnerId(me.getId());
            item.setCatalogId(catalog.getId());
            itemRepository.save(item);
        }
        return detail(list.getId());
    }

    @Transactional
    public void removeItem(User me, Long listId, Long itemId) {
        requireOwned(me, listId);
        ListItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> ApiException.notFound("Item não encontrado"));
        if (!item.getListId().equals(listId)) {
            throw ApiException.badRequest("Item não pertence a essa lista");
        }
        itemRepository.delete(item);
    }

    // -------------------------------------------------------------- HELPERS
    private MovieList requireOwned(User me, Long listId) {
        MovieList list = listRepository.findById(listId)
                .orElseThrow(() -> ApiException.notFound("Lista não encontrada"));
        if (!list.getOwnerId().equals(me.getId())) {
            throw ApiException.forbidden("Essa lista não é sua");
        }
        return list;
    }

    private ListSummaryResponse summary(MovieList list) {
        List<ListItem> items = itemRepository.findByListIdOrderByCreatedAtAsc(list.getId());
        List<String> covers = new ArrayList<>();
        for (ListItem item : items) {
            if (covers.size() >= 4) {
                break;
            }
            catalogRepository.findById(item.getCatalogId()).ifPresent(catalog -> {
                if (catalog.getPosterUrl() != null) {
                    covers.add(catalog.getPosterUrl());
                }
            });
        }
        return new ListSummaryResponse(list.getId(), list.getTitle(), items.size(), covers, list.getCreatedAt());
    }
}
