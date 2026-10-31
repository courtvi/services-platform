package org.gestion.devis.repository;


import org.gestion.devis.model.Devis;
import org.springframework.data.r2dbc.repository.R2dbcRepository;


public interface DevisRepository extends R2dbcRepository<Devis, Long> {

}
