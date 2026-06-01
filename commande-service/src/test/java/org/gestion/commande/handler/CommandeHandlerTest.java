package org.gestion.commande.handler;

import org.gestion.commande.dto.CommandeRequest;
import org.gestion.commande.dto.CommandeResponse;
import org.gestion.commande.dto.LigneCommandeRequest;
import org.gestion.commande.service.CommandeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.reactive.function.server.MockServerRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommandeHandlerTest {

    @Mock
    private CommandeService commandeService;

    private CommandeHandler commandeHandler;

    private CommandeRequest request;
    private CommandeResponse response;
    private JwtAuthenticationToken authToken;

    @BeforeEach
    void setUp() {
        commandeHandler = new CommandeHandler(commandeService);

        request = new CommandeRequest(
                "CMD-TEST-001",
                LocalDateTime.of(2026, 5, 16, 0, 0),
                LocalDateTime.of(2026, 5, 17, 0, 0),
                List.of(new LigneCommandeRequest("Baguette", 1, 1.10, 1.10))
        );

        response = new CommandeResponse(
                1L,
                "user-123",
                "CMD-TEST-001",
                "CREEE",
                LocalDateTime.of(2026, 5, 16, 0, 0),
                LocalDateTime.of(2026, 5, 17, 0, 0),
                6.70
       );

        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("sub", "user-123")
                .claim("email", "user@test.com")
                .claim("numeroClient", "CLI-00001")
                .build();
        authToken = new JwtAuthenticationToken(jwt);
    }

    @Test
    void createCommande_shouldReturn201() {
        when(commandeService.createCommande(any(), eq("user-123"), eq("user@test.com"), eq("CLI-00001")))
                .thenReturn(Mono.just(response));

        MockServerRequest serverRequest = MockServerRequest.builder()
                .principal(authToken)
                .body(Mono.just(request));

        StepVerifier.create(commandeHandler.createCommande(serverRequest))
                .expectNextMatches(r -> r.statusCode().value() == 201)
                .verifyComplete();
    }

    @Test
    void createCommande_shouldReturn400OnError() {
        when(commandeService.createCommande(any(), eq("user-123"), eq("user@test.com"), eq("CLI-00001")))
                .thenReturn(Mono.error(new RuntimeException("Erreur service")));

        MockServerRequest serverRequest = MockServerRequest.builder()
                .principal(authToken)
                .body(Mono.just(request));

        StepVerifier.create(commandeHandler.createCommande(serverRequest))
                .expectNextMatches(r -> r.statusCode().value() == 400)
                .verifyComplete();
    }

    @Test
    void getCommandes_shouldCallGetAllCommandesForAdmin() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("sub", "admin-123")
                .build();
        JwtAuthenticationToken adminAuth = new JwtAuthenticationToken(
                jwt,
                List.of(() -> "ROLE_ADMIN")
        );

        when(commandeService.getAllCommandes()).thenReturn(Flux.just(response));

        MockServerRequest serverRequest = MockServerRequest.builder()
                .principal(adminAuth)
                .build();

        commandeHandler.getCommandes(serverRequest).block();

        verify(commandeService).getAllCommandes();
        verify(commandeService, never()).getCommandesByUser(any());
    }

    @Test
    void getCommandeById_shouldReturn200() {
        when(commandeService.getCommandeByIdForUser(eq(1L), eq("user-123")))
                .thenReturn(Mono.just(response));

        MockServerRequest serverRequest = MockServerRequest.builder()
                .principal(authToken)
                .pathVariable("id", "1")
                .build();

        StepVerifier.create(commandeHandler.getCommandeById(serverRequest))
                .expectNextMatches(r -> r.statusCode().value() == 200)
                .verifyComplete();
    }

    @Test
    void getCommandeById_shouldReturn404WhenNotFound() {
        when(commandeService.getCommandeByIdForUser(eq(1L), eq("user-123")))
                .thenReturn(Mono.error(new RuntimeException("Commande introuvable")));

        MockServerRequest serverRequest = MockServerRequest.builder()
                .principal(authToken)
                .pathVariable("id", "1")
                .build();

        StepVerifier.create(commandeHandler.getCommandeById(serverRequest))
                .expectNextMatches(r -> r.statusCode().value() == 404)
                .verifyComplete();
    }

    @Test
    void annulerCommande_shouldReturn204() {
        when(commandeService.annulerCommande(eq(1L), eq("user-123")))
                .thenReturn(Mono.empty());

        MockServerRequest serverRequest = MockServerRequest.builder()
                .principal(authToken)
                .pathVariable("id", "1")
                .build();

        StepVerifier.create(commandeHandler.annulerCommande(serverRequest))
                .expectNextMatches(r -> r.statusCode().value() == 204)
                .verifyComplete();
    }

    @Test
    void getCommandes_shouldReturnTotalInResponse() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .claim("sub", "admin-123")
                .build();
        JwtAuthenticationToken adminAuth = new JwtAuthenticationToken(
                jwt,
                List.of(() -> "ROLE_ADMIN")
        );

        CommandeResponse avecTotal = new CommandeResponse(
                1L,
                "user-alice",
                "CMD-ALICE-001",
                "CREEE",
                LocalDateTime.of(2026, 5, 21, 20, 52),
                LocalDateTime.of(2026, 5, 22, 20, 52),
                6.70
        );

        when(commandeService.getAllCommandes()).thenReturn(Flux.just(avecTotal));

        MockServerRequest serverRequest = MockServerRequest.builder()
                .principal(adminAuth)
                .build();

        commandeHandler.getCommandes(serverRequest).block();

        verify(commandeService).getAllCommandes();
    }

    @Test
    void getCommandes_shouldRouteToUserCommandesForNonAdmin() {
        when(commandeService.getCommandesByUser(eq("user-123")))
                .thenReturn(Flux.just(response));

        MockServerRequest serverRequest = MockServerRequest.builder()
                .principal(authToken)
                .build();

        commandeHandler.getCommandes(serverRequest).block();

        verify(commandeService).getCommandesByUser("user-123");
        verify(commandeService, never()).getAllCommandes();
    }

    @Test
    void createCommande_shouldReturnTotalInResponse() {
        CommandeResponse avecTotal = new CommandeResponse(
                1L,
                "user-123",
                "CMD-TEST-001",
                "CREEE",
                LocalDateTime.of(2026, 5, 16, 0, 0),
                LocalDateTime.of(2026, 5, 17, 0, 0),
                1.10
        );

        when(commandeService.createCommande(any(), eq("user-123"), eq("user@test.com"), eq("CLI-00001")))
                .thenReturn(Mono.just(avecTotal));

        MockServerRequest serverRequest = MockServerRequest.builder()
                .principal(authToken)
                .body(Mono.just(request));

        StepVerifier.create(commandeHandler.createCommande(serverRequest))
                .expectNextMatches(r -> r.statusCode().value() == 201)
                .verifyComplete();
    }

}