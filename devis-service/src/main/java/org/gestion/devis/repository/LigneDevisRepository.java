package org.gestion.devis.repository;

import org.gestion.devis.model.LigneDevis;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;

public interface LigneDevisRepository extends R2dbcRepository<LigneDevis, Long> {

    Flux<LigneDevis> findByVersionId(Long versionId);
}
