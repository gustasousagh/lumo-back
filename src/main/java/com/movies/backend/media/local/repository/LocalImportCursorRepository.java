package com.movies.backend.media.local.repository;

import com.movies.backend.media.local.entity.LocalImportCursor;
import com.movies.backend.media.local.entity.LocalKind;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LocalImportCursorRepository extends JpaRepository<LocalImportCursor, LocalKind> {}
