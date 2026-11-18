package org.gestion.produit.service;

import org.gestion.produit.mapper.ProduitMapper;
import org.gestion.produit.model.FicheBoutique;
import org.gestion.produit.model.Produit;
import org.gestion.produit.repository.FicheBoutiqueRepository;
import org.gestion.produit.repository.ProduitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FicheBoutiqueServiceTest {

    @Mock FicheBoutiqueRepository ficheRepository;
    @Mock ProduitRepository produitRepository;

    private FicheBoutiqueService service;

    private final Produit vinRouge =
            new Produit(101L, "vin_rouge", "Vin rouge", new BigDecimal("5.83"), new BigDecimal("20.00"));

    @BeforeEach
    void setUp() {
        service = new FicheBoutiqueService(ficheRepository, produitRepository, new ProduitMapper());
    }

    private static FicheBoutique fiche(Long produitId, String boutiqueId) {
        FicheBoutique f = new FicheBoutique();
        f.setProduitId(produitId);
        f.setBoutiqueId(boutiqueId);
        f.setOrigine("Saulnes");
        f.setUnite("75cl");
        f.setCategorie("rouge");
        f.setRupture(true);
        f.setActif(true);
        return f;
    }

    @Test
    void catalogue_composeProduitEtFiche() {
        when(ficheRepository.findByBoutiqueIdAndActifTrueOrderByProduitIdAsc("vin"))
                .thenReturn(Flux.just(fiche(101L, "vin")));
        when(produitRepository.findById(101L)).thenReturn(Mono.just(vinRouge));

        StepVerifier.create(service.catalogue("vin"))
                .assertNext(p -> {
                    assertEquals(101L, p.id());
                    assertEquals("vin", p.boutiqueId());
                    assertEquals("vin_rouge", p.cle());
                    assertEquals(new BigDecimal("7.00"), p.prix(), "prix TTC calculé à partir du HT");
                    assertEquals(new BigDecimal("5.83"), p.prixHT());
                    assertEquals("rouge", p.categorie());
                    assertTrue(p.rupture());
                })
                .verifyComplete();
    }

    @Test
    void produit_videSansFicheActiveDansLaBoutique() {
        when(ficheRepository.findByBoutiqueIdAndProduitIdAndActifTrue("miel", 101L)).thenReturn(Mono.empty());

        StepVerifier.create(service.produit("miel", 101L)).verifyComplete();
        verify(produitRepository, never()).findById(any(Long.class));
    }

    @Test
    void produit_renvoieLaComposition() {
        when(ficheRepository.findByBoutiqueIdAndProduitIdAndActifTrue("vin", 101L))
                .thenReturn(Mono.just(fiche(101L, "vin")));
        when(produitRepository.findById(101L)).thenReturn(Mono.just(vinRouge));

        StepVerifier.create(service.produit("vin", 101L))
                .assertNext(p -> assertEquals("Saulnes", p.origine()))
                .verifyComplete();
    }
}
