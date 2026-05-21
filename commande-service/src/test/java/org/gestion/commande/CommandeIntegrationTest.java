package org.gestion.commande;

import org.gestion.commande.config.SecurityTestConfig;
import org.gestion.commande.dto.CommandeResponse;
import org.gestion.commande.model.Commande;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
@Import(SecurityTestConfig.class)
class CommandeIntegrationTest {

    @Autowired
    WebTestClient webTestClient;

/*
    @Test
    void getCommandes_shouldReturnTotalForCMD_ALICE_001() {
        webTestClient
                .get()
                .uri("/api/commandes")
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(CommandeResponse.class)
                .value(commandes -> {
                    CommandeResponse alice001 = commandes.stream()
                            .filter(c -> c.reference().equals("CMD-ALICE-001"))
                            .findFirst()
                            .orElseThrow(() -> new AssertionError("CMD-ALICE-001 non trouvée"));

                    System.out.println(">>> total CMD-ALICE-001: " + alice001.total());
                    assertThat(alice001.total())
                            .as("total CMD-ALICE-001 doit être 6.70")
                            .isEqualTo(6.70);
                });
    }
    */
}
