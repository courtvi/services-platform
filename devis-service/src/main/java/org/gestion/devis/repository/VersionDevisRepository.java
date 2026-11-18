package org.gestion.devis.repository;

import org.gestion.devis.model.VersionDevis;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface VersionDevisRepository extends R2dbcRepository<VersionDevis, Long> {

    Flux<VersionDevis> findByDevisIdOrderByNumeroVersionAsc(Long devisId);

    Mono<VersionDevis> findFirstByDevisIdOrderByNumeroVersionDesc(Long devisId);
}
