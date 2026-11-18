package org.gestion.produit.service;

import org.gestion.produit.dto.ProduitBoutiqueResponse;
import org.gestion.produit.mapper.ProduitMapper;
import org.gestion.produit.repository.FicheBoutiqueRepository;
import org.gestion.produit.repository.ProduitRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** Catalogue d'une boutique : fiches actives + produit correspondant (composition). */
@Service
public class FicheBoutiqueService {

    private final FicheBoutiqueRepository ficheRepository;
    private final ProduitRepository produitRepository;
    private final ProduitMapper mapper;

    public FicheBoutiqueService(FicheBoutiqueRepository ficheRepository,
                                ProduitRepository produitRepository,
                                ProduitMapper mapper) {
        this.ficheRepository = ficheRepository;
        this.produitRepository = produitRepository;
        this.mapper = mapper;
    }

    public Flux<ProduitBoutiqueResponse> catalogue(String boutiqueId) {
        return ficheRepository.findByBoutiqueIdAndActifTrueOrderByProduitIdAsc(boutiqueId)
                .concatMap(fiche -> produitRepository.findById(fiche.getProduitId())
                        .map(produit -> mapper.toProduitBoutique(produit, fiche)));
    }

    /** Vide si le produit n'a pas de fiche active dans cette boutique (le handler renvoie 404). */
    public Mono<ProduitBoutiqueResponse> produit(String boutiqueId, Long produitId) {
        return ficheRepository.findByBoutiqueIdAndProduitIdAndActifTrue(boutiqueId, produitId)
                .flatMap(fiche -> produitRepository.findById(produitId)
                        .map(produit -> mapper.toProduitBoutique(produit, fiche)));
    }
}
