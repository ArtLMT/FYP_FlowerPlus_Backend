package com.lmt.fyp.flowerplus.module.material.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record MaterialVersionRequest(
        @NotNull
        @PositiveOrZero
        Long version
) {
}
