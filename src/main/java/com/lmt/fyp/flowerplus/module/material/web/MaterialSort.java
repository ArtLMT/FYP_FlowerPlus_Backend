package com.lmt.fyp.flowerplus.module.material.web;

import org.springframework.data.domain.Sort;

public enum MaterialSort {
    NAME_ASC("name", Sort.Direction.ASC),
    NAME_DESC("name", Sort.Direction.DESC),
    PRICE_ASC("sellingPrice", Sort.Direction.ASC),
    PRICE_DESC("sellingPrice", Sort.Direction.DESC),
    CREATED_NEWEST("createdAt", Sort.Direction.DESC),
    CREATED_OLDEST("createdAt", Sort.Direction.ASC);

    private final String property;
    private final Sort.Direction direction;

    MaterialSort(String property, Sort.Direction direction) {
        this.property = property;
        this.direction = direction;
    }

    public static MaterialSort from(String value) {
        if (value == null) {
            return NAME_ASC;
        }

        try {
            return valueOf(value);
        } catch (IllegalArgumentException ex) {
            return NAME_ASC;
        }
    }

    public Sort toSort() {
        return Sort.by(direction, property).and(Sort.by(Sort.Direction.ASC, "id"));
    }
}
