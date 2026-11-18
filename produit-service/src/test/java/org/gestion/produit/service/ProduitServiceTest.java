package org.gestion.produit.service;

import org.gestion.produit.dto.ProduitRequest;
import org.gestion.produit.mapper.ProduitMapper;
import org.gestion.produit.model.Produit;
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
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProduitServiceTest {

    @Mock
    ProduitRepository produitRepository;

    private ProduitService produitService;

    @BeforeEach
    void setUp() {
        produitService = new ProduitService(produitRepository, new ProduitMapper());
    }

    private static ProduitRequest request(String name, String price, String tva) {
        return new ProduitRequest(name, "desc",
                price == null ? null : new BigDecimal(price),
                tva == null ? null : new BigDecimal(tva));
    }

    private static Produit produit(Long id, String name) {
        return new Produit(id, name, "desc", new BigDecimal("10.00"), new BigDecimal("20.00"));
    }

    @Test
    void create_enregistreSansIdEtRenvoieLeProduit() {
        AtomicReference<Long> idRecu = new AtomicReference<>(-1L);
        when(produitRepository.save(any(Produit.class))).thenAnswer(inv -> {
            Produit p = inv.getArgument(0);
            idRecu.set(p.getId());   // id tel que reçu par save(), avant que la "base" en attribue un
            p.setId(1L);
            return Mono.just(p);
        });

        StepVerifier.create(produitService.create(request("miel", "8.50", "5.50")))
                .assertNext(r -> {
                    assertEquals(1L, r.id());
                    assertEquals("miel", r.name());
                    assertEquals(new BigDecimal("8.50"), r.price());
                })
                .verifyComplete();

        assertNull(idRecu.get(), "un nouveau produit ne doit pas arriver avec un id");
    }

    @Test
    void create_refuseUnNomVide() {
        StepVerifier.create(produitService.create(request(" ", "8.50", "5.50")))
                .expectError(IllegalArgumentException.class)
                .verify();
        verify(produitRepository, never()).save(any());
    }

    @Test
    void create_refuseUnPrixNegatif() {
        StepVerifier.create(produitService.create(request("miel", "-1", "5.50")))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void create_refuseUneTvaAbsente() {
        StepVerifier.create(produitService.create(request("miel", "8.50", null)))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void create_refuseUneTvaSuperieureA100() {
        StepVerifier.create(produitService.create(request("miel", "8.50", "120")))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    @Test
    void findAll_renvoieTousLesProduits() {
        when(produitRepository.findAll()).thenReturn(Flux.just(produit(1L, "miel"), produit(2L, "vin")));

        StepVerifier.create(produitService.findAll())
                .assertNext(r -> assertEquals("miel", r.name()))
                .assertNext(r -> assertEquals("vin", r.name()))
                .verifyComplete();
    }

    @Test
    void findById_videSiInconnu() {
        when(produitRepository.findById(99L)).thenReturn(Mono.empty());

        StepVerifier.create(produitService.findById(99L)).verifyComplete();
    }

    @Test
    void update_modifieLeProduitExistant() {
        when(produitRepository.findById(1L)).thenReturn(Mono.just(produit(1L, "ancien")));
        when(produitRepository.save(any(Produit.class))).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(produitService.update(1L, request("nouveau", "12.00", "20.00")))
                .assertNext(r -> {
                    assertEquals(1L, r.id());
                    assertEquals("nouveau", r.name());
                    assertEquals(new BigDecimal("12.00"), r.price());
                })
                .verifyComplete();
    }

    @Test
    void update_videSiInconnu() {
        when(produitRepository.findById(99L)).thenReturn(Mono.empty());

        StepVerifier.create(produitService.update(99L, request("x", "1", "20"))).verifyComplete();
        verify(produitRepository, never()).save(any());
    }

    @Test
    void deleteById_trueSiSupprime() {
        Produit existant = produit(1L, "miel");
        when(produitRepository.findById(1L)).thenReturn(Mono.just(existant));
        when(produitRepository.delete(existant)).thenReturn(Mono.empty());

        StepVerifier.create(produitService.deleteById(1L)).expectNext(true).verifyComplete();
    }

    @Test
    void deleteById_falseSiInconnu() {
        when(produitRepository.findById(99L)).thenReturn(Mono.empty());

        StepVerifier.create(produitService.deleteById(99L)).expectNext(false).verifyComplete();
    }
}
