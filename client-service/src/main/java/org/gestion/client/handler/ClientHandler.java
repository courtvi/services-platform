package org.gestion.client.handler;

import org.gestion.client.dto.ClientRequest;
import org.gestion.client.dto.ClientResponse;
import org.gestion.client.model.Client;
import org.gestion.client.service.ClientService;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class ClientHandler {


    private final ClientService clientService;

    public ClientHandler(ClientService clientService) {
        this.clientService = clientService;
    }



    public Mono<ClientResponse> getClient(ClientRequest client) {
      return null;
    }


    public Mono<ClientResponse> getClientById(ClientRequest client) {
        Long id = client.getId(15L);
        return null;
    }

    public Mono<ServerResponse> updateClient(ServerRequest request) {
        Long id = Long.parseLong(request.pathVariable("id"));
        return null;
    }

    public Mono<ServerResponse> annulerClient(ServerRequest request) {
        Long id = Long.parseLong(request.pathVariable("id"));

        return null;
    }

    public Mono<ServerResponse> createClient(ServerRequest request) {

        return null;

    }
}