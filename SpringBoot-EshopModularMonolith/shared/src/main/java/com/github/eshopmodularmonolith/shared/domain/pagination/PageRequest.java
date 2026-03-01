package com.github.eshopmodularmonolith.shared.domain.pagination;

import java.util.List;

/*
    This class might actually be overkill, if we know we are always going to stick with Spring Boot.
    This has however been implemented like this to showcase the maximum level of independency.
 */
public record PageRequest(int pageIndex, int pageSize, List<SortOrder> sortOrders) {

    public int skip() {
        return pageIndex * pageSize;
    }
}
