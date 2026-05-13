package org.gestion.commande.repository;

import org.gestion.commande.model.LigneCommande;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;

public interface LigneCommandeRepository extends R2dbcRepository<LigneCommande, Long> {

    Flux<LigneCommande> findByCommandeId(Long commandeId);
}
