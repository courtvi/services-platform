package org.gestion.produit.repository;

import org.gestion.produit.model.FicheBoutique;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface FicheBoutiqueRepository extends R2dbcRepository<FicheBoutique, Long> {

    Flux<FicheBoutique> findByBoutiqueIdAndActifTrueOrderByProduitIdAsc(String boutiqueId);

    Mono<FicheBoutique> findByBoutiqueIdAndProduitIdAndActifTrue(String boutiqueId, Long produitId);
}
