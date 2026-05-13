package org.gestion.product.service;


import org.gestion.product.model.Product;
import org.gestion.product.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Service
public class ProductService {

   @Autowired
   ProductRepository productRepository;

    public Flux<Product> findAll() {
        return productRepository.findAll();
    }

    public Flux<Product> findByTitleContaining(String name) {
        return productRepository.findByNameContaining(name);
    }

    public Mono<Product> findById(Integer id) {
        return productRepository.findById(id);
    }

    public Mono<Product> save(Product product) {
        return productRepository.save(product);
    }

    public Mono<Product> update(int id, Product product) {
        return productRepository.findById(id).map(Optional::of).defaultIfEmpty(Optional.empty())
                .flatMap(optionalProduct -> {
                    if (optionalProduct.isPresent()) {
                        product.setId(id);
                        return productRepository.save(product);
                    }

                    return Mono.empty();
                });
    }

    public Mono<Void> deleteById(int id) {
        return productRepository.deleteById(id);
    }

    public Mono<Void> deleteAll() {
        return productRepository.deleteAll();
    }

}