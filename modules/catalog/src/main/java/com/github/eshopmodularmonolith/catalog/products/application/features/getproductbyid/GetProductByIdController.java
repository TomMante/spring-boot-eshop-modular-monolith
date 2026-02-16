package com.github.eshopmodularmonolith.catalog.products.application.features.getproductbyid;

import com.github.eshopmodularmonolith.catalog.products.contract.ProductMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class GetProductByIdController {

    private final GetProductByIdHandler handler;

    public GetProductByIdController(GetProductByIdHandler handler) {
        this.handler = handler;
    }

    @GetMapping("/product/{id}")
    public ResponseEntity<GetProductByIdResponse> getProductById(@PathVariable("id") UUID id) {
        var productDTO = handler.handle(new GetProductByIdQuery(id)).product();
        var response = new GetProductByIdResponse(productDTO);
        return ResponseEntity.ok(response);
    }
}
