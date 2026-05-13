package org.gestion.commande.repository;

import org.gestion.commande.model.Commande;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CommandeRepository extends R2dbcRepository<Commande, Long> {

    // ✅ Trouve toutes les commandes d'un utilisateur spécifique
    Flux<Commande> findByUserId(String userId);

    // ✅ Trouve une commande spécifique d'un utilisateur (sécurité : évite qu'un user voie celle d'un autre)
    Mono<Commande> findByIdAndUserId(Long id, String userId);

}
// ✅ Trouve les co