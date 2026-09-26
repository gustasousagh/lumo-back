package com.movies.backend.language.repository;

import com.movies.backend.language.entity.LanguageProfile;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LanguageProfileRepository extends JpaRepository<LanguageProfile, Long> {
    Optional<LanguageProfile> findByUserId(Long userId);
}
