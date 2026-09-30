package com.lmt.fyp.flowerplus.module.product.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record CategoryVersionRequest(
        @NotNull
        @PositiveOrZero
        Long version
) {
}
