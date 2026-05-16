package org.gestion.commande.service;

import org.gestion.commande.dto.CommandeRequest;
import org.gestion.commande.dto.LigneCommandeRequest;
import org.gestion.commande.model.Commande;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommandeServiceTest {

    @Mock CommandeRepository commandeRepository;
    @Mock LigneCommandeRepository ligneCommandeRepository;

    private TransactionalOperator transactionalOperator;
    private CommandeService commandeService;

    private CommandeRequest request;
    private Commande savedCommande;

    @BeforeEach
    void setUp() {
        ReactiveTransactionManager txManager = new ReactiveTransactionManager() {
            @Override
            public Mono<org.springframework.transaction.ReactiveTransaction> getReactiveTransaction(
                    TransactionDefinition definition) {
                return Mono.just(new GenericReactiveTransaction(
                        "test-transaction",  // transactionName
                        null,                // transaction object
                        false,               // newTransaction
                        false,               // newSynchronization
                        false,               // nested
                        false,               // readOnly
                        true,                // debug
                        null                 // suspendedResources
                ));
            }

            @Override
            public Mono<Void> commit(org.springframework.transaction.ReactiveTransaction tx) {
                return Mono.empty();
            }

            @Override
            public Mono<Void> rollback(org.springframework.transaction.ReactiveTransaction tx) {
                return Mono.empty();
            }
        };

        transactionalOperator = TransactionalOperator.create(txManager);

        commandeService = new CommandeService(
                commandeRepository,
                ligneCommandeRepository,
                transactionalOperator
        );

        request = new CommandeRequest(
                "CMD-TEST-001",
                LocalDateTime.of(2026, 5, 16, 0, 0),
                LocalDateTime.of(2026, 5, 17, 0, 0),
                List.of(new LigneCommandeRequest("Baguette", 1, 1.10, 1.10))
        );

        savedCommande = new Commande();
        savedCommande.setId(1L);
        savedCommande.setUserId("user-123");
        savedCommande.setReference("CMD-TEST-001");
        savedCommande.setStatut("CREEE");
        savedCommande.setDateCommande(LocalDateTime.of(2026, 5, 16, 0, 0));
        savedCommande.setDateLivraison(LocalDateTime.of(2026, 5, 17, 0, 0));
    }

    @Test
    void createCommande_shouldReturnResponse() {
        when(commandeRepository.save(any())).thenReturn(Mono.just(savedCommande));
        when(ligneCommandeRepository.saveAll(any(Iterable.class))).thenReturn(Flux.empty());

        StepVerifier.create(commandeService.createCommande(request, "user-123"))
                .expectNextMatches(response ->
                        response.reference().equals("CMD-TEST-001") &&
                                response.statut().equals("CREEE") &&
                                response.userId().equals("user-123")
                )
                .verifyComplete();
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
}