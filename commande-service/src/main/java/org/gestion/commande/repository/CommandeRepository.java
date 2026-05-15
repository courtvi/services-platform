package org.gestion.commande.repository;

import org.gestion.commande.model.Commande;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface CommandeRepository extends R2dbcRepository<Commande, Long> {


    Flux<Commande> findByUserId(String userId);


    Mono<Commande> findByIdAndUserId(Long id, String userId);

}
