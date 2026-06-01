package org.gestion.commande.repository;

import org.gestion.commande.model.ClientRef;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Mono;

public interface ClientRefRepository extends R2dbcRepository<ClientRef, String> {

    @Query("SELECT * FROM CLIENT_REF WHERE NUMERO_CLIENT = :numeroClient")
    Mono<ClientRef> findByNumeroClient(String numeroClient);
}