package org.gestion.product.repository;

import org.gestion.product.model.Product;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface ProductRepository extends R2dbcRepository<Product, Integer> {
    Flux<Product> findByNameContaining(String name);

}
