package org.gestion.commande.service;

import org.gestion.commande.dto.CommandeRequest;
import org.gestion.commande.dto.LigneCommandeRequest;
import org.gestion.commande.model.Commande;
import org.gestion.commande.model.LigneCommande;
import org.gestion.commande.repository.CommandeRepository;
import org.gestion.commande.repository.LigneCommandeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.reactive.GenericReactiveTransaction;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CommandeServiceTest {

    @Mock
    CommandeRepository commandeRepository;
    @Mock
    LigneCommandeRepository ligneCommandeRepository;

    private TransactionalOperator transactionalOperator;
    private CommandeService commandeService;

    private CommandeRequest request;
    private CommandeRequest requestSansLignes;
    private Commande savedCommande;
    private Commande commandeAnnulee;
    private Commande commandeLivree;

    @BeforeEach
    void setUp() {
        ReactiveTransactionManager txManager = new ReactiveTransactionManager() {
            @Override
            public Mono<org.springframework.transaction.ReactiveTransaction> getReactiveTransaction(
                    TransactionDefinition definition) {
                return Mono.just(new GenericReactiveTransaction(
                        "test-transaction", null, false, false, false, false, true, null
                ));
            }
            @Override public Mono<Void> commit(org.springframework.transaction.ReactiveTransaction tx) { return Mono.empty(); }
            @Override public Mono<Void> rollback(org.springframework.transaction.ReactiveTransaction tx) { return Mono.empty(); }
        };

        transactionalOperator = TransactionalOperator.create(txManager);
        commandeService = new CommandeService(commandeRepository, ligneCommandeRepository, transactionalOperator);

        request = new CommandeRequest(
                "CMD-TEST-001",
                LocalDateTime.of(2026, 5, 16, 0, 0),
                LocalDateTime.of(2026, 5, 17, 0, 0),
                List.of(new LigneCommandeRequest("Baguette", 1, 1.10, 1.10))
        );

        requestSansLignes = new CommandeRequest(
                "CMD-TEST-002",
                LocalDateTime.of(2026, 5, 16, 0, 0),
                LocalDateTime.of(2026, 5, 17, 0, 0),
                null
        );

        savedCommande   = buildCommande(1L, "user-123", "CMD-TEST-001", "CREEE");
        commandeAnnulee = buildCommande(2L, "user-123", "CMD-TEST-002", "ANNULEE");
        commandeLivree  = buildCommande(3L, "user-123", "CMD-TEST-003", "LIVREE");
    }

    // helper — mock lignes pour toResponseWithTotal
    private void mockLignes(Long commandeId, double... totaux) {
        Flux<LigneCommande> flux = Flux.fromArray(
                java.util.Arrays.stream(totaux)
                        .mapToObj(t -> {
                            LigneCommande l = new LigneCommande();
                            l.setCommandeId(commandeId);
                            l.setTotal(t);
                            return l;
                        })
                        .toArray(LigneCommande[]::new)
        );
        when(ligneCommandeRepository.findByCommandeId(commandeId)).thenReturn(flux);
    }

    private void mockLignesVides(Long commandeId) {
        when(ligneCommandeRepository.findByCommandeId(commandeId)).thenReturn(Flux.empty());
    }

    // ─────────────────────────────────────────────
    // createCommande
    // ─────────────────────────────────────────────

    @Test
    void createCommande_shouldReturnResponseWithTotal() {
        when(commandeRepository.save(any())).thenReturn(Mono.just(savedCommande));
        when(ligneCommandeRepository.saveAll(any(Iterable.class))).thenReturn(Flux.empty());
        mockLignes(1L, 1.10);

        StepVerifier.create(commandeService.createCommande(request, "user-123"))
                .expectNextMatches(r ->
                        r.reference().equals("CMD-TEST-001") &&
                                r.statut().equals("CREEE") &&
                                r.userId().equals("user-123") &&
                                r.total() == 1.10
                )
                .verifyComplete();
    }

    @Test
    void createCommande_sansLignes_shouldReturnTotalZero() {
        Commande commande = buildCommande(1L, "user-123", "CMD-TEST-002", "CREEE");
        when(commandeRepository.save(any())).thenReturn(Mono.just(commande));
        mockLignesVides(1L);

        StepVerifier.create(commandeService.createCommande(requestSansLignes, "user-123"))
                .expectNextMatches(r ->
                        r.reference().equals("CMD-TEST-002") &&
                                r.total() == 0.0
                )
                .verifyComplete();

        verify(ligneCommandeRepository, never()).saveAll(any(Iterable.class));
    }

    @Test
    void createCommande_shouldFailWithNullUserId() {
        when(commandeRepository.save(any())).thenReturn(
                Mono.error(new IllegalArgumentException("userId ne peut pas être null"))
        );

        StepVerifier.create(commandeService.createCommande(request, null))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    // ─────────────────────────────────────────────
    // getCommandeByIdForUser
    // ─────────────────────────────────────────────

    @Test
    void getCommandeByIdForUser_shouldReturnCommandeWithTotal() {
        when(commandeRepository.findByIdAndUserId(1L, "user-123"))
                .thenReturn(Mono.just(savedCommande));
        mockLignes(1L, 2.20, 4.50);

        StepVerifier.create(commandeService.getCommandeByIdForUser(1L, "user-123"))
                .expectNextMatches(r ->
                        r.id().equals(1L) &&
                                r.total() == 6.70
                )
                .verifyComplete();
    }

    @Test
    void getCommandeByIdForUser_notFound_shouldReturnError() {
        when(commandeRepository.findByIdAndUserId(99L, "user-123"))
                .thenReturn(Mono.empty());

        StepVerifier.create(commandeService.getCommandeByIdForUser(99L, "user-123"))
                .expectErrorMatches(e -> e instanceof RuntimeException &&
                        e.getMessage().contains("accès refusé"))
                .verify();
    }

    // ─────────────────────────────────────────────
    // getCommandesByUser
    // ─────────────────────────────────────────────

    @Test
    void getCommandesByUser_shouldReturnAllWithTotals() {
        Commande c2 = buildCommande(2L, "user-123", "CMD-002", "CREEE");
        when(commandeRepository.findByUserId("user-123"))
                .thenReturn(Flux.just(savedCommande, c2));
        mockLignes(1L, 2.20, 4.50);
        mockLignes(2L, 7.00);

        StepVerifier.create(commandeService.getCommandesByUser("user-123"))
                .expectNextMatches(r -> r.id().equals(1L) && r.total() == 6.70)
                .expectNextMatches(r -> r.id().equals(2L) && r.total() == 7.00)
                .verifyComplete();
    }

    @Test
    void getCommandesByUser_noCommandes_shouldReturnEmpty() {
        when(commandeRepository.findByUserId("user-456")).thenReturn(Flux.empty());

        StepVerifier.create(commandeService.getCommandesByUser("user-456"))
                .verifyComplete();
    }

    // ─────────────────────────────────────────────
    // getAllCommandes
    // ─────────────────────────────────────────────

    @Test
    void getAllCommandes_shouldReturnAllWithTotals() {
        Commande c2 = buildCommande(2L, "user-456", "CMD-002", "CREEE");
        when(commandeRepository.findAll()).thenReturn(Flux.just(savedCommande, c2));
        mockLignes(1L, 1.10);
        mockLignes(2L, 3.50, 7.00);

        StepVerifier.create(commandeService.getAllCommandes())
                .expectNextMatches(r -> r.id().equals(1L) && r.total() == 1.10)
                .expectNextMatches(r -> r.id().equals(2L) && r.total() == 10.50)
                .verifyComplete();
    }

    // ─────────────────────────────────────────────
    // updateCommande
    // ─────────────────────────────────────────────

    @Test
    void updateCommande_shouldUpdateReference() {
        CommandeRequest updateRequest = new CommandeRequest(
                "CMD-UPDATED", LocalDateTime.of(2026, 5, 16, 0, 0),
                LocalDateTime.of(2026, 5, 17, 0, 0), null
        );
        Commande updated = buildCommande(1L, "user-123", "CMD-UPDATED", "CREEE");

        when(commandeRepository.findByIdAndUserId(1L, "user-123")).thenReturn(Mono.just(savedCommande));
        when(commandeRepository.save(any())).thenReturn(Mono.just(updated));
        mockLignes(1L, 2.20);

        StepVerifier.create(commandeService.updateCommande(1L, updateRequest, "user-123"))
                .expectNextMatches(r ->
                        r.reference().equals("CMD-UPDATED") &&
                                r.total() == 2.20
                )
                .verifyComplete();
    }

    @Test
    void updateCommande_commandeAnnulee_shouldFail() {
        CommandeRequest updateRequest = new CommandeRequest(
                "CMD-UPDATED", LocalDateTime.now(), LocalDateTime.now(), null
        );
        when(commandeRepository.findByIdAndUserId(2L, "user-123")).thenReturn(Mono.just(commandeAnnulee));

        StepVerifier.create(commandeService.updateCommande(2L, updateRequest, "user-123"))
                .expectErrorMatches(e -> e.getMessage().contains("ANNULEE"))
                .verify();
    }

    @Test
    void updateCommande_commandeLivree_shouldFail() {
        CommandeRequest updateRequest = new CommandeRequest(
                "CMD-UPDATED", LocalDateTime.now(), LocalDateTime.now(), null
        );
        when(commandeRepository.findByIdAndUserId(3L, "user-123")).thenReturn(Mono.just(commandeLivree));

        StepVerifier.create(commandeService.updateCommande(3L, updateRequest, "user-123"))
                .expectErrorMatches(e -> e.getMessage().contains("LIVREE"))
                .verify();
    }

    @Test
    void updateCommande_notFound_shouldFail() {
        CommandeRequest updateRequest = new CommandeRequest(
                "CMD-UPDATED", LocalDateTime.now(), LocalDateTime.now(), null
        );
        when(commandeRepository.findByIdAndUserId(99L, "user-123")).thenReturn(Mono.empty());

        StepVerifier.create(commandeService.updateCommande(99L, updateRequest, "user-123"))
                .expectErrorMatches(e -> e.getMessage().contains("accès refusé"))
                .verify();
    }

    // ─────────────────────────────────────────────
    // annulerCommande
    // ─────────────────────────────────────────────

    @Test
    void annulerCommande_shouldSetStatutAnnulee() {
        Commande annulee = buildCommande(1L, "user-123", "CMD-TEST-001", "ANNULEE");
        when(commandeRepository.findByIdAndUserId(1L, "user-123")).thenReturn(Mono.just(savedCommande));
        when(commandeRepository.save(any())).thenReturn(Mono.just(annulee));

        StepVerifier.create(commandeService.annulerCommande(1L, "user-123"))
                .verifyComplete();

        verify(commandeRepository).save(any());
    }

    @Test
    void annulerCommande_dejaAnnulee_shouldFail() {
        when(commandeRepository.findByIdAndUserId(2L, "user-123")).thenReturn(Mono.just(commandeAnnulee));

        StepVerifier.create(commandeService.annulerCommande(2L, "user-123"))
                .expectErrorMatches(e -> e.getMessage().contains("ANNULEE"))
                .verify();
    }

    @Test
    void annulerCommande_notFound_shouldFail() {
        when(commandeRepository.findByIdAndUserId(99L, "user-123")).thenReturn(Mono.empty());

        StepVerifier.create(commandeService.annulerCommande(99L, "user-123"))
                .expectErrorMatches(e -> e.getMessage().contains("accès refusé"))
                .verify();
    }

    // ─────────────────────────────────────────────
    // passerEnCours
    // ─────────────────────────────────────────────

    @Test
    void passerEnCours_shouldSetStatutEnCours() {
        Commande enCours = buildCommande(1L, "user-123", "CMD-TEST-001", "EN_COURS");
        when(commandeRepository.findById(1L)).thenReturn(Mono.just(savedCommande));
        when(commandeRepository.save(any())).thenReturn(Mono.just(enCours));
        mockLignes(1L, 2.20, 4.50);

        StepVerifier.create(commandeService.passerEnCours(1L))
                .expectNextMatches(r ->
                        r.statut().equals("EN_COURS") &&
                                r.total() == 6.70
                )
                .verifyComplete();
    }

    @Test
    void passerEnCours_commandeDejaEnCours_shouldFail() {
        Commande enCours = buildCommande(1L, "user-123", "CMD-TEST-001", "EN_COURS");
        when(commandeRepository.findById(1L)).thenReturn(Mono.just(enCours));

        StepVerifier.create(commandeService.passerEnCours(1L))
                .expectErrorMatches(e -> e.getMessage().contains("EN_COURS"))
                .verify();
    }

    @Test
    void passerEnCours_notFound_shouldFail() {
        when(commandeRepository.findById(99L)).thenReturn(Mono.empty());

        StepVerifier.create(commandeService.passerEnCours(99L))
                .expectErrorMatches(e -> e.getMessage().contains("introuvable"))
                .verify();
    }

    // ─────────────────────────────────────────────
    // getCommande (avec lignes)
    // ─────────────────────────────────────────────

    @Test
    void getCommande_shouldReturnCommandeAvecLignes() {
        LigneCommande ligne = new LigneCommande();
        ligne.setCommandeId(1L);
        ligne.setArticle("Baguette");
        ligne.setQuantite(1);
        ligne.setPrixUnitaire(1.10);
        ligne.setTotal(1.10);

        when(commandeRepository.findById(1L)).thenReturn(Mono.just(savedCommande));
        when(ligneCommandeRepository.findByCommandeId(1L)).thenReturn(Flux.just(ligne));

        StepVerifier.create(commandeService.getCommande(1L))
                .expectNextMatches(result ->
                        result.commande().getId().equals(1L) &&
                                result.lignes().size() == 1 &&
                                result.lignes().get(0).getArticle().equals("Baguette")
                )
                .verifyComplete();
    }

    @Test
    void getCommande_notFound_shouldReturnEmpty() {
        when(commandeRepository.findById(99L)).thenReturn(Mono.empty());
        when(ligneCommandeRepository.findByCommandeId(99L)).thenReturn(Flux.empty());

        StepVerifier.create(commandeService.getCommande(99L))
                .verifyComplete();
    }

    // ─────────────────────────────────────────────
    // Helper
    // ─────────────────────────────────────────────

    private Commande buildCommande(Long id, String userId, String reference, String statut) {
        Commande c = new Commande();
        c.setId(id);
        c.setUserId(userId);
        c.setReference(reference);
        c.setStatut(statut);
        c.setDateCommande(LocalDateTime.of(2026, 5, 16, 0, 0));
        c.setDateLivraison(LocalDateTime.of(2026, 5, 17, 0, 0));
        return c;
    }
}