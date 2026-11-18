package org.gestion.client.handler;

import org.gestion.client.dto.ClientRequest;
import org.gestion.client.dto.ClientResponse;
import org.gestion.client.service.ClientService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.net.URI;

@Component
public class ClientHandler {

    private final ClientService clientService;

    public ClientHandler(ClientService clientService) {
        this.clientService = clientService;
    }

    public Mono<ServerResponse> getClients(ServerRequest request) {
        return ServerResponse.ok().body(clientService.getAllClients(), ClientResponse.class);
    }

    public Mono<ServerResponse> getClientById(ServerRequest request) {
        return lireId(request)
                .flatMap(clientService::getClientById)
                .flatMap(client -> ServerResponse.ok().bodyValue(client))
                .switchIfEmpty(ServerResponse.notFound().build())
                .onErrorResume(IllegalArgumentException.class, this::badRequest);
    }

    public Mono<ServerResponse> createClient(ServerRequest request) {
        return request.bodyToMono(ClientRequest.class)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Corps de requête manquant")))
                .flatMap(clientService::createClient)
                .flatMap(client -> ServerResponse
                        .created(URI.create("/api/clients/" + client.getId()))
                        .bodyValue(client))
                .onErrorResume(IllegalArgumentException.class, this::badRequest);
    }

    public Mono<ServerResponse> updateClient(ServerRequest request) {
        return lireId(request)
                .flatMap(id -> request.bodyToMono(ClientRequest.class)
                        .switchIfEmpty(Mono.error(new IllegalArgumentException("Corps de requête manquant")))
                        .flatMap(body -> clientService.updateClient(id, body)))
                .flatMap(client -> ServerResponse.ok().bodyValue(client))
                .switchIfEmpty(ServerResponse.notFound().build())
                .onErrorResume(IllegalArgumentException.class, this::badRequest);
    }

    public Mono<ServerResponse> deleteClient(ServerRequest request) {
        return lireId(request)
                .flatMap(clientService::deleteClient)
                .flatMap(supprime -> supprime
                        ? ServerResponse.noContent().build()
                        : ServerResponse.notFound().build())
                .onErrorResume(IllegalArgumentException.class, this::badRequest);
    }

    private Mono<Long> lireId(ServerRequest request) {
        try {
            return Mono.just(Long.parseLong(request.pathVariable("id")));
        } catch (NumberFormatException e) {
            return Mono.error(new IllegalArgumentException("Identifiant invalide"));
        }
    }

    private Mono<ServerResponse> badRequest(IllegalArgumentException e) {
        return ServerResponse.status(HttpStatus.BAD_REQUEST).bodyValue(e.getMessage());
    }
}
