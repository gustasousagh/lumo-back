package com.movies.backend.language.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/** Meta diária de XP. Os valores que a tela oferece são 10, 20, 30 e 50. */
public record SetGoalRequest(@Min(5) @Max(200) int dailyGoalXp) {}
