package org.gestion.produit.service;


import org.gestion.produit.model.Produit;
import org.gestion.produit.repository.ProduitRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Service
public class ProduitService {

   @Autowired
   ProduitRepository produitRepository;

    public Flux<Produit> findAll() {
        return produitRepository.findAll();
    }

    public Flux<Produit> findByTitleContaining(String name) {
        return produitRepository.findByNameContaining(name);
    }

    public Mono<Produit> findById(Long id) {
        return produitRepository.findById(id);
    }

    public Mono<Produit> save(Produit product) {
        return produitRepository.save(product);
    }

    public Mono<Produit> update(long id, Produit produit) {
        return produitRepository.findById(id).map(Optional::of).defaultIfEmpty(Optional.empty())
                .flatMap(optionalProduit -> {
                    if (optionalProduit.isPresent()) {
                        produit.setId(id);
                        return produitRepository.save(produit);
                    }

                    return Mono.empty();
                });
    }

    public Mono<Void> deleteById(long id) {
        return produitRepository.deleteById(id);
    }

    public Mono<Void> deleteAll() {
        return produitRepository.deleteAll();
    }

}