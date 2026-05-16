package org.gestion.commande.repository;

import org.gestion.commande.model.Commande;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;

@DataR2dbcTest(properties = {
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=classpath:schema.sql"
})
class CommandeRepositoryTest {

    @Autowired
    CommandeRepository commandeRepository;

    private Commande commande1;
    private Commande commande2;

    @BeforeEach
    void setUp() {
        commandeRepository.deleteAll().block();

        commande1 = new Commande();
        commande1.setUserId("user-123");
        commande1.setReference("CMD-TEST-001");
        commande1.setStatut("CREEE");
        commande1.setDateCommande(LocalDateTime.of(2026, 5, 16, 0, 0));
        commande1.setDateLivraison(LocalDateTime.of(2026, 5, 17, 0, 0));

        commande2 = new Commande();
        commande2.setUserId("user-123");
        commande2.setReference("CMD-TEST-002");
        commande2.setStatut("LIVREE");
        commande2.setDateCommande(LocalDateTime.of(2026, 5, 16, 0, 0));
        commande2.setDateLivraison(LocalDateTime.of(2026, 5, 18, 0, 0));
    }

    @Test
    void save_shouldPersistAndReturnWithGeneratedId() {
        StepVerifier.create(commandeRepository.save(commande1))
                .expectNextMatches(saved ->
                        saved.getId() != null &&
                                saved.getReference().equals("CMD-TEST-001") &&
                                saved.getStatut().equals("CREEE") &&
                                saved.getUserId().equals("user-123")
                )
                .verifyComplete();
    }

    @Test
    void findById_shouldReturnCommande() {
        StepVerifier.create(
                        commandeRepository.save(commande1)
                                .flatMap(saved -> commandeRepository.findById(saved.getId()))
                )
                .expectNextMatches(found ->
                        found.getReference().equals("CMD-TEST-001")
                )
                .verifyComplete();
    }

    @Test
    void findByUserId_shouldReturnAllCommandesForUser() {
        StepVerifier.create(
                        commandeRepository.save(commande1)
                                .then(commandeRepository.save(commande2))
                                .thenMany(commandeRepository.findByUserId("user-123"))
                )
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    void findByUserId_shouldReturnEmptyForUnknownUser() {
        StepVerifier.create(commandeRepository.findByUserId("unknown-user"))
                .verifyComplete();
    }

    @Test
    void findByIdAndUserId_shouldReturnCommandeWhenMatch() {
        StepVerifier.create(
                        commandeRepository.save(commande1)
                                .flatMap(saved ->
                                        commandeRepository.findByIdAndUserId(saved.getId(), "user-123")
                                )
                )
                .expectNextMatches(found ->
                        found.getReference().equals("CMD-TEST-001") &&
                                found.getUserId().equals("user-123")
                )
                .verifyComplete();
    }

    @Test
    void findByIdAndUserId_shouldReturnEmptyWhenUserIdMismatch() {
        StepVerifier.create(
                        commandeRepository.save(commande1)
                                .flatMap(saved ->
                                        commandeRepository.findByIdAndUserId(saved.getId(), "autre-user")
                                )
                )
                .verifyComplete();
    }

    @Test
    void delete_shouldRemoveCommande() {
        StepVerifier.create(
                        commandeRepository.save(commande1)
                                .flatMap(saved ->
                                        commandeRepository.deleteById(saved.getId())
                                                .then(commandeRepository.findById(saved.getId()))
                                )
                )
                .verifyComplete();
    }

    @Test
    void dateLivraison_shouldBePersisted() {
        StepVerifier.create(
                        commandeRepository.save(commande1)
                                .flatMap(saved -> commandeRepository.findById(saved.getId()))
                )
                .expectNextMatches(found ->
                        found.getDateLivraison().equals(LocalDateTime.of(2026, 5, 17, 0, 0))
                )
                .verifyComplete();
    }
}