package org.gestion.commande.handler;

import org.gestion.commande.dto.CommandeRequest;
import org.gestion.commande.dto.CommandeResponse;
import org.gestion.commande.service.CommandeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class CommandeHandler {

    @Autowired
    private final CommandeService commandeService;

    public CommandeHandler(CommandeService commandeService) {
        this.commandeService = commandeService;
    }


    private Mono<String> extractUserId(ServerRequest request) {
        return request.principal()
                .map(principal -> (JwtAuthenticationToken) principal)
                .map(token -> (Jwt) token.getCredentials())
                .map(Jwt::getSubject);
    }


    private Mono<Authentication> extractAuthentication(ServerRequest request) {
        return request.principal()
                .map(principal -> (Authentication) principal);
    }

    /*
    public Mono<ServerResponse> createCommande(ServerRequest request) {
        return extractUserId(request)
                .flatMap(userId ->
                        request.bodyToMono(CommandeRequest.class)
                                .flatMap(commandeRequest ->
                                        commandeService.createCommande(commandeRequest, userId)
                                )
                )
                .flatMap(response ->
                        ServerResponse.status(201).bodyValue(response)
                )
                .onErrorResume(e -> {
                    e.printStackTrace(); // ← ajoute ça
                    return ServerResponse.badRequest().bodyValue(e.getMessage());
                });
    }
    */
    public Mono<ServerResponse> createCommande(ServerRequest request) {
        return request.principal()
                .cast(JwtAuthenticationToken.class)
                .flatMap(auth -> {
                    String userId = auth.getToken().getSubject();
                    String userEmail = auth.getToken().getClaimAsString("email"); // ✅ email extrait

                    return request.bodyToMono(CommandeRequest.class)
                            .flatMap(commandeRequest ->
                                    commandeService.createCommande(commandeRequest, userId, userEmail)
                            );
                })
                .flatMap(response ->
                        ServerResponse.status(201).bodyValue(response)
                )
                .onErrorResume(e -> {
                    e.printStackTrace();
                    return ServerResponse.badRequest().bodyValue(e.getMessage());
                });
    }

    public Mono<ServerResponse> getCommandes(ServerRequest request) {
        return extractAuthentication(request)
                .flatMap(authentication -> {


                    boolean isAdmin = authentication.getAuthorities().stream()
                            .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

                    if (isAdmin) {
                        return ServerResponse.ok()
                                .body(commandeService.getAllCommandes(),
                                        CommandeResponse.class);
                    }


                    Jwt jwt = (Jwt) ((JwtAuthenticationToken) authentication).getCredentials();
                    String userId = jwt.getSubject();

                    return ServerResponse.ok()
                            .body(commandeService.getCommandesByUser(userId),
                                    CommandeResponse.class);
                })
                .onErrorResume(e ->
                        ServerResponse.status(500).bodyValue(e.getMessage())
                );
    }

    // GET /api/commandes/{id}/detail — avec lignes
    public Mono<ServerResponse> getCommandeAvecLignes(ServerRequest request) {
        Long id = Long.parseLong(request.pathVariable("id"));

        return commandeService.getCommande(id)
                .flatMap(response ->
                        ServerResponse.ok().bodyValue(response)
                )
                .onErrorResume(e ->
                        ServerResponse.status(404).bodyValue(e.getMessage())
                );
    }

    public Mono<ServerResponse> getCommandeById(ServerRequest request) {
        Long id = Long.parseLong(request.pathVariable("id"));

        return extractUserId(request)
                .flatMap(userId ->
                        commandeService.getCommandeByIdForUser(id, userId)
                )
                .flatMap(response ->
                        ServerResponse.ok().bodyValue(response)
                )
                .onErrorResume(e ->
                        ServerResponse.status(404).bodyValue(e.getMessage())
                );
    }

    public Mono<ServerResponse> updateCommande(ServerRequest request) {
        Long id = Long.parseLong(request.pathVariable("id"));

        return extractUserId(request)
                .flatMap(userId ->
                        request.bodyToMono(CommandeRequest.class)
                                .flatMap(commandeRequest ->
                                        commandeService.updateCommande(id, commandeRequest, userId)
                                )
                )
                .flatMap(response ->
                        ServerResponse.ok().bodyValue(response)
                )
                .onErrorResume(e ->
                        ServerResponse.badRequest().bodyValue(e.getMessage())
                );
    }

    public Mono<ServerResponse> annulerCommande(ServerRequest request) {
        Long id = Long.parseLong(request.pathVariable("id"));

        return extractUserId(request)
                .flatMap(userId ->
                        commandeService.annulerCommande(id, userId)
                )
                .then(ServerResponse.noContent().build())
                .onErrorResume(e ->
                        ServerResponse.badRequest().bodyValue(e.getMessage())
                );
    }

    public Mono<ServerResponse> passerEnCours(ServerRequest request) {
        Long id = Long.parseLong(request.pathVariable("id"));
        return commandeService.passerEnCours(id)
                .flatMap(response -> ServerResponse.ok().bodyValue(response))
                .onErrorResume(e -> ServerResponse.status(400).bodyValue(e.getMessage()));
    }
}