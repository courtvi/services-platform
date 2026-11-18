package org.gestion.commande.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.gestion.commande.dto.ProduitInfo;
import org.mockito.ArgumentCaptor;
import java.math.BigDecimal;
import org.gestion.commande.dto.CommandeRequest;
import org.gestion.commande.dto.LigneCommandeRequest;
import org.gestion.commande.model.ClientRef;
import org.gestion.commande.model.Commande;
import org.gestion.commande.model.LigneCommande;
import org.gestion.commande.repository.ClientRefRepository;
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

    @Mock
    ClientRefRepository clientRefRepository;

    @Mock
    MailService mailService;

    @Mock
    ProduitClient produitClient;

    private TransactionalOperator transactionalOperator;
    private CommandeService commandeService;

    private CommandeRequest request;
    private CommandeRequest requestSansLignes;
    private Commande savedCommande;
    private Commande commandeAnnulee;
    private Commande commandeLivree;

    /** 16,11 € HT à 5,5 % = 17,00 € TTC (prix affiché par le front). */
    private static final ProduitInfo MIEL_PRINTEMPS =
            new ProduitInfo(1L, "miel_printemps", "Saulnes - 1kg", new BigDecimal("16.11"), new BigDecimal("5.50"));

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
        commandeService = new CommandeService(
                commandeRepository,
                ligneCommandeRepository,
                transactionalOperator,
                mailService,
                clientRefRepository,
                produitClient);

        request = new CommandeRequest(
                "CMD-TEST-001",
                LocalDateTime.of(2026, 5, 16, 0, 0),
                LocalDateTime.of(2026, 5, 17, 0, 0),
                List.of(new LigneCommandeRequest(1L, 2))
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
                            l.setTotal(BigDecimal.valueOf(t));
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
        when(clientRefRepository.findById("user-123")).thenReturn(Mono.just(new ClientRef("user-123", "CLI-00001")));
        when(commandeRepository.save(any())).thenReturn(Mono.just(savedCommande));
        when(produitClient.getProduit(1L)).thenReturn(Mono.just(MIEL_PRINTEMPS));
        when(ligneCommandeRepository.saveAll(any(Iterable.class))).thenReturn(Flux.empty());
        mockLignes(1L, 1.10);
        when(mailService.sendConfirmationCommande(any(), any(), any())).thenReturn(Mono.empty());
        StepVerifier.create(commandeService.createCommande(request, "user-123", "user@test.com", "CLI-00001"))
                .expectNextMatches(r ->
                        r.reference().equals("CMD-TEST-001") &&
                                r.statut().equals("CREEE") &&
                                r.userId().equals("user-123") &&
                                r.total().compareTo(new BigDecimal("1.10")) == 0
                )
                .verifyComplete();
    }

    @Test
    void createCommande_sansLignes_shouldReturnTotalZero() {
        Commande commande = buildCommande(1L, "user-123", "CMD-TEST-002", "CREEE");
        when(clientRefRepository.findById("user-123")).thenReturn(Mono.just(new ClientRef("user-123", "CLI-00001")));
        when(commandeRepository.save(any())).thenReturn(Mono.just(commande));
        mockLignesVides(1L);
        when(mailService.sendConfirmationCommande(any(), any(), any())).thenReturn(Mono.empty());
        StepVerifier.create(commandeService.createCommande(requestSansLignes, "user-123", "user@test.com", "CLI-00001"))
                .expectNextMatches(r ->
                        r.reference().equals("CMD-TEST-002") &&
                                r.total().compareTo(new BigDecimal("0.0")) == 0
                )
                .verifyComplete();

        verify(ligneCommandeRepository, never()).saveAll(any(Iterable.class));
    }

    @Test
    void createCommande_shouldFailWithNullUserId() {
        when(clientRefRepository.findById((String) null)).thenReturn(Mono.empty());
        when(clientRefRepository.save(any())).thenReturn(
                Mono.error(new IllegalArgumentException("userId ne peut pas être null"))
        );

        StepVerifier.create(commandeService.createCommande(request, null, "user@test.com", "CLI-00001"))
                .expectError(IllegalArgumentException.class)
                .verify();
    }

    // ─────────────────────────────────────────────
    // createCommande : prix lus dans produit-service
    // ─────────────────────────────────────────────

    @Test
    @SuppressWarnings("unchecked")
    void createCommande_calculeLePrixDepuisProduitService() {
        when(clientRefRepository.findById("user-123")).thenReturn(Mono.just(new ClientRef("user-123", "CLI-00001")));
        when(produitClient.getProduit(1L)).thenReturn(Mono.just(MIEL_PRINTEMPS));
        when(commandeRepository.save(any())).thenReturn(Mono.just(savedCommande));
        when(ligneCommandeRepository.saveAll(any(Iterable.class))).thenReturn(Flux.empty());
        mockLignesVides(1L);
        when(mailService.sendConfirmationCommande(any(), any(), any())).thenReturn(Mono.empty());

        StepVerifier.create(commandeService.createCommande(request, "user-123", "user@test.com", "CLI-00001"))
                .expectNextCount(1)
                .verifyComplete();

        ArgumentCaptor<Iterable<LigneCommande>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(ligneCommandeRepository).saveAll(captor.capture());
        LigneCommande ligne = captor.getValue().iterator().next();
        assertEquals(1L, ligne.getProduitId());
        assertEquals("miel_printemps", ligne.getArticle());
        assertEquals(new BigDecimal("17.00"), ligne.getPrixUnitaire());
        assertEquals(new BigDecimal("34.00"), ligne.getTotal());
        assertEquals(1L, ligne.getCommandeId());
        assertEquals(new BigDecimal("5.50"), ligne.getTauxTVA());
    }

    @Test
    void createCommande_refuseUnProduitInconnu_sansRienEnregistrer() {
        when(clientRefRepository.findById("user-123")).thenReturn(Mono.just(new ClientRef("user-123", "CLI-00001")));
        when(produitClient.getProduit(1L)).thenReturn(Mono.error(new IllegalArgumentException("Produit inconnu : 1")));

        StepVerifier.create(commandeService.createCommande(request, "user-123", "user@test.com", "CLI-00001"))
                .expectError(IllegalArgumentException.class)
                .verify();

        verify(commandeRepository, never()).save(any());
    }

    @Test
    void createCommande_refuseUneQuantiteInvalide() {
        CommandeRequest quantiteNulle = new CommandeRequest(
                "CMD-TEST-003",
                LocalDateTime.of(2026, 5, 16, 0, 0),
                LocalDateTime.of(2026, 5, 17, 0, 0),
                List.of(new LigneCommandeRequest(1L, 0)));
        when(clientRefRepository.findById("user-123")).thenReturn(Mono.just(new ClientRef("user-123", "CLI-00001")));

        StepVerifier.create(commandeService.createCommande(quantiteNulle, "user-123", "user@test.com", "CLI-00001"))
                .expectError(IllegalArgumentException.class)
                .verify();

        verify(produitClient, never()).getProduit(any());
        verify(commandeRepository, never()).save(any());
    }

    @Test
    void prixTTC_arrondiAuCentime() {
        assertEquals(new BigDecimal("17.00"), CommandeService.prixTTC(MIEL_PRINTEMPS));
        assertEquals(new BigDecimal("7.00"), CommandeService.prixTTC(
                new ProduitInfo(101L, "vin_rouge", null, new BigDecimal("5.83"), new BigDecimal("20.00"))));
    }

    // ─────────────────────────────────────────────
    // getCommandeByIdForUser
    // ─────────────────────────────────────────────

    @Test
    void getCommandeByIdForUser_shouldReturnCommandeWithTotal() {
        when(clientRefRepository.findById("user-123"))
                .thenReturn(Mono.just(new ClientRef("user-123", "CLI-00001")));
        when(commandeRepository.findByIdAndUserId(1L, "user-123"))
                .thenReturn(Mono.just(savedCommande));
        mockLignes(1L, 2.20, 4.50);

        StepVerifier.create(commandeService.getCommandeByIdForUser(1L, "user-123"))
                .expectNextMatches(r ->
                        r.id().equals(1L) &&
                                r.total().compareTo(new BigDecimal("6.70")) == 0
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
        when(clientRefRepository.findById("user-123"))
                .thenReturn(Mono.just(new ClientRef("user-123", "CLI-00001")));
        when(commandeRepository.findByUserId("user-123"))
                .thenReturn(Flux.just(savedCommande, c2));
        mockLignes(1L, 2.20, 4.50);
        mockLignes(2L, 7.00);

        StepVerifier.create(commandeService.getCommandesByUser("user-123"))
                .expectNextMatches(r -> r.id().equals(1L) && r.total().compareTo(new BigDecimal("6.70")) == 0)
                .expectNextMatches(r -> r.id().equals(2L) && r.total().compareTo(new BigDecimal("7.00")) == 0)
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
        when(clientRefRepository.findById("user-123"))
                .thenReturn(Mono.just(new ClientRef("user-123", "CLI-00001")));
        when(clientRefRepository.findById("user-456"))
                .thenReturn(Mono.just(new ClientRef("user-456", "CLI-00002")));
        when(commandeRepository.findAll()).thenReturn(Flux.just(savedCommande, c2));
        mockLignes(1L, 1.10);
        mockLignes(2L, 3.50, 7.00);

        StepVerifier.create(commandeService.getAllCommandes())
                .expectNextMatches(r -> r.id().equals(1L) && r.total().compareTo(new BigDecimal("1.10")) == 0)
                .expectNextMatches(r -> r.id().equals(2L) && r.total().compareTo(new BigDecimal("10.50")) == 0)
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
        when(clientRefRepository.findById("user-123"))
                .thenReturn(Mono.just(new ClientRef("user-123", "CLI-00001")));
        when(commandeRepository.findByIdAndUserId(1L, "user-123")).thenReturn(Mono.just(savedCommande));
        when(commandeRepository.save(any())).thenReturn(Mono.just(updated));
        mockLignes(1L, 2.20);

        StepVerifier.create(commandeService.updateCommande(1L, updateRequest, "user-123"))
                .expectNextMatches(r ->
                        r.reference().equals("CMD-UPDATED") &&
                                r.total().compareTo(new BigDecimal("2.20")) == 0
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
        when(clientRefRepository.findById("user-123"))
                .thenReturn(Mono.just(new ClientRef("user-123", "CLI-00001")));
        when(commandeRepository.findById(1L)).thenReturn(Mono.just(savedCommande));
        when(commandeRepository.save(any())).thenReturn(Mono.just(enCours));
        mockLignes(1L, 2.20, 4.50);

        StepVerifier.create(commandeService.passerEnCours(1L))
                .expectNextMatches(r ->
                        r.statut().equals("EN_COURS") &&
                                r.total().compareTo(new BigDecimal("6.70")) == 0
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
        ligne.setPrixUnitaire(new BigDecimal("1.10"));
        ligne.setTotal(new BigDecimal("1.10"));

        when(clientRefRepository.findById("user-123"))
                .thenReturn(Mono.just(new ClientRef("user-123", "CLI-00001")));
        when(commandeRepository.findById(1L)).thenReturn(Mono.just(savedCommande));
        when(ligneCommandeRepository.findByCommandeId(1L)).thenReturn(Flux.just(ligne));

        StepVerifier.create(commandeService.getCommande(1L))
                .expectNextMatches(result ->
                        result.commande().id().equals(1L) &&
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

    @Test
    void getCommandeAvecLignesPourUtilisateur_renvoieSaCommande() {
        LigneCommande ligne = new LigneCommande();
        ligne.setCommandeId(1L);
        ligne.setArticle("Baguette");
        ligne.setQuantite(1);
        ligne.setPrixUnitaire(new BigDecimal("1.10"));
        ligne.setTotal(new BigDecimal("1.10"));

        when(commandeRepository.findByIdAndUserId(1L, "user-123")).thenReturn(Mono.just(savedCommande));
        when(clientRefRepository.findById("user-123"))
                .thenReturn(Mono.just(new ClientRef("user-123", "CLI-00001")));
        when(ligneCommandeRepository.findByCommandeId(1L)).thenReturn(Flux.just(ligne));

        StepVerifier.create(commandeService.getCommandeAvecLignesPourUtilisateur(1L, "user-123"))
                .expectNextMatches(result ->
                        result.commande().id().equals(1L) && result.lignes().size() == 1)
                .verifyComplete();
    }

    @Test
    void getCommandeAvecLignesPourUtilisateur_videSiCommandeDUnAutre() {
        when(commandeRepository.findByIdAndUserId(1L, "intrus")).thenReturn(Mono.empty());

        StepVerifier.create(commandeService.getCommandeAvecLignesPourUtilisateur(1L, "intrus"))
                .verifyComplete();
        verify(ligneCommandeRepository, never()).findByCommandeId(any());
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