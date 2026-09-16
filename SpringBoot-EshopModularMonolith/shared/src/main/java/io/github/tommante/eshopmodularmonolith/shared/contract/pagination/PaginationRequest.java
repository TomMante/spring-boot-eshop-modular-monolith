package io.github.tommante.eshopmodularmonolith.shared.contract.pagination;

public record PaginationRequest(int pageIndex, int pageSize) {

    public PaginationRequest() {
        this(0, 10);
    }

    public PaginationRequest(int pageIndex){
        this(pageIndex, 10);
    }
}
