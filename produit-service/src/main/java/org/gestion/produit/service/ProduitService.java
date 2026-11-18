package org.gestion.produit.service;

import org.gestion.produit.dto.ProduitRequest;
import org.gestion.produit.dto.ProduitResponse;
import org.gestion.produit.mapper.ProduitMapper;
import org.gestion.produit.repository.ProduitRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@Service
public class ProduitService {

    private static final BigDecimal CENT = BigDecimal.valueOf(100);

    private final ProduitRepository produitRepository;
    private final ProduitMapper mapper;

    public ProduitService(ProduitRepository produitRepository, ProduitMapper mapper) {
        this.produitRepository = produitRepository;
        this.mapper = mapper;
    }

    public Flux<ProduitResponse> findAll() {
        return produitRepository.findAll().map(mapper::toResponse);
    }

    public Flux<ProduitResponse> findByNameContaining(String name) {
        return produitRepository.findByNameContaining(name).map(mapper::toResponse);
    }

    /** Mono vide si le produit n'existe pas (le handler renvoie alors 404). */
    public Mono<ProduitResponse> findById(Long id) {
        return produitRepository.findById(id).map(mapper::toResponse);
    }

    public Mono<ProduitResponse> create(ProduitRequest request) {
        return valider(request)
                .map(mapper::toEntity)
                .flatMap(produitRepository::save)
                .map(mapper::toResponse);
    }

    /** Mono vide si le produit n'existe pas. */
    public Mono<ProduitResponse> update(Long id, ProduitRequest request) {
        return valider(request)
                .flatMap(r -> produitRepository.findById(id)
                        .map(existant -> {
                            mapper.updateEntity(r, existant);
                            return existant;
                        }))
                .flatMap(produitRepository::save)
                .map(mapper::toResponse);
    }

    /** true si le produit existait et a été supprimé, false sinon. */
    public Mono<Boolean> deleteById(Long id) {
        return produitRepository.findById(id)
                .flatMap(produit -> produitRepository.delete(produit).thenReturn(true))
                .defaultIfEmpty(false);
    }

    private Mono<ProduitRequest> valider(ProduitRequest request) {
        if (request == null) {
            return Mono.error(new IllegalArgumentException("Corps de requête manquant"));
        }
        if (request.name() == null || request.name().isBlank()) {
            return Mono.error(new IllegalArgumentException("Nom obligatoire"));
        }
        if (request.price() == null || request.price().signum() < 0) {
            return Mono.error(new IllegalArgumentException("Prix obligatoire et positif"));
        }
        if (request.tauxTVA() == null || request.tauxTVA().signum() < 0 || request.tauxTVA().compareTo(CENT) > 0) {
            return Mono.error(new IllegalArgumentException("Taux de TVA obligatoire, entre 0 et 100"));
        }
        return Mono.just(request);
    }
}
