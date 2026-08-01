package com.movies.backend.list.repository;

import com.movies.backend.list.entity.MovieList;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovieListRepository extends JpaRepository<MovieList, Long> {

    List<MovieList> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);
}
