package org.gestion.commande.repository;

import org.gestion.commande.model.ClientRef;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.r2dbc.test.autoconfigure.DataR2dbcTest;

import org.springframework.test.context.ActiveProfiles;
import reactor.test.StepVerifier;

@ActiveProfiles("test")
@DataR2dbcTest(properties = {
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=classpath:schema.sql"
})
class ClientRefRepositoryTest {

    @Autowired
    private ClientRefRepository repository;

    @BeforeEach
    void setUp() {
        repository.deleteAll().block();
    }

    @Test
    void shouldSaveAndFindById() {
        ClientRef clientRef = new ClientRef("keycloak-123", "CLI-00001");

        StepVerifier.create(repository.save(clientRef)
                        .then(repository.findById("keycloak-123")))
                .expectNextMatches(c -> c.getNumeroClient().equals("CLI-00001"))
                .verifyComplete();
    }

    @Test
    void shouldFindByNumeroClient() {
        ClientRef clientRef = new ClientRef("keycloak-123", "CLI-00001");

        StepVerifier.create(repository.save(clientRef)
                        .then(repository.findByNumeroClient("CLI-00001")))
                .expectNextMatches(c -> c.getKeycloakId().equals("keycloak-123"))
                .verifyComplete();
    }

    @Test
    void shouldReturnEmptyWhenNotFound() {
        StepVerifier.create(repository.findByNumeroClient("CLI-99999"))
                .verifyComplete();
    }
}