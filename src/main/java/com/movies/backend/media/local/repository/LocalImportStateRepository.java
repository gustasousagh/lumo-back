package com.movies.backend.media.local.repository;

import com.movies.backend.media.local.entity.LocalImportState;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LocalImportStateRepository extends JpaRepository<LocalImportState, Long> {}
