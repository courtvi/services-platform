package org.gestion.product.reactivewebservice;


import org.gestion.product.model.Product;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class ProductClient {

    private final WebClient client;

    public ProductClient(WebClient.Builder builder) {
        this.client = builder
                .baseUrl("http://localhost:8081")
                .build();
    }

    public Mono<Product> getProductById(Integer id) {
        return this.client.get()
                .uri("/products/{id}", id)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(Product.class);

    }
}
