package com.github.eshopmodularmonolith.shared.domain.pagination;

import java.util.List;

public record PageResult<T>(List<T> items, int pageIndex, int pageSize, long totalItems, int totalPages) {

    public static <T> PageResult<T> of(List<T> items, int pageIndex, int pageSize, long totalItems){
        return new PageResult<>(items, pageIndex, pageSize, totalItems, (int) Math.ceil((double) totalItems / pageSize));
    }
}
