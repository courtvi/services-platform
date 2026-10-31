package org.gestion.produit.repository;

import org.gestion.produit.model.Produit;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface ProduitRepository extends R2dbcRepository<Produit, Long> {
    Flux<Produit> findByNameContaining(String name);

}
