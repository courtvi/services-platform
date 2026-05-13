package org.gestion.product.reactivewebservice;

import org.gestion.product.dto.ProductRequest;
import org.gestion.product.dto.ProductResponse;
import org.gestion.product.model.Product;
import org.gestion.product.service.ProductService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.net.URI;

@Component
public class ProductHandler {

    private final ProductService productService;

    public ProductHandler(ProductService productService) {
        this.productService = productService;
    }

    public Mono<ServerResponse> getAllProducts(ServerRequest request) {

        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(productService.findAll(), ProductResponse.class);
    }

    public Mono<ServerResponse> createProduct(ServerRequest request) {
        return request.bodyToMono(Product.class)
                .flatMap(productService::save)
                .flatMap(savedProduct ->
                        ServerResponse
                                .created(URI.create("/api/products/" + savedProduct.getId()))
                                .bodyValue(savedProduct)
                );
    }

    /*
    public Mono<ServerResponse> createProduct(ServerRequest request) {
        return request.bodyToMono(ProductRequest.class)
                .flatMap(saved ->
                        ServerResponse
                                .status(HttpStatus.CREATED)
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(saved)
                );
    }*/
}
