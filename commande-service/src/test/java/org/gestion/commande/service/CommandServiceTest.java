package org.gestion.commande.service;

import org.gestion.commande.dto.CommandeRequest;
import org.gestion.commande.dto.LigneCommandeRequest;
import org.gestion.commande.model.Commande;
import org.gestion.commande.model.LigneCommande;
import org.gestion.commande.repository.CommandeRepository;
import org.gestion.commande.repository.LigneCommandeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.reactive.TransactionCallback;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommandServiceTest {

    @Mock
    private CommandeRepository commandeRepository;

    @Mock
    private LigneCommandeRepository ligneCommandeRepository;

    @Mock
    private TransactionalOperator transactionalOperator;

    @InjectMocks
    private CommandeService commandeService;

    @Test
    void shouldCreateCommande() {

        // -------- GIVEN --------
        CommandeRequest request = new CommandeRequest("REF-001", List.of(
                new LigneCommandeRequest("baguette", 1, 1.30F, 1.30F)
        ));

        Commande saved = new Commande();
        saved.setId(1L);
        saved.setUserId("user-123");
        saved.setReference("REF-001");
        saved.setStatut("CREEE");
        saved.setDateCommande(LocalDateTime.now());

        // mock transaction wrapper
        when(transactionalOperator.execute(any()))
                .thenAnswer(invocation -> {

                    TransactionCallback<?> callback = invocation.getArgument(0);

                    return Flux.defer(() -> {
                        Mono<?> result = (Mono<?>) callback.doInTransaction(null);
                        return result.flux();
                    });
                });
        // mock repository
        when(commandeRepository.save(any(Commande.class)))
                .thenReturn(Mono.just(saved));

        when(ligneCommandeRepository.saveAll((Iterable<LigneCommande>) any()))
                .thenReturn(Flux.empty());

        // -------- WHEN / THEN --------
        StepVerifier.create(
                        commandeService.createCommande(request, "user-123")
                )
                .expectNextMatches(response ->
                        response.reference().equals("REF-001")
                                && response.userId().equals("user-123")
                )
                .verifyComplete();
    }
}