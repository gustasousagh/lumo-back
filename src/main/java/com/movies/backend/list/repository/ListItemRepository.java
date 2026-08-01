package com.movies.backend.list.repository;

import com.movies.backend.list.entity.ListItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ListItemRepository extends JpaRepository<ListItem, Long> {

    List<ListItem> findByListIdOrderByCreatedAtAsc(Long listId);

    Optional<ListItem> findByListIdAndCatalogId(Long listId, Long catalogId);

    long countByListId(Long listId);

    void deleteByListId(Long listId);
}
