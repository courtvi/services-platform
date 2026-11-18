package org.gestion.produit.handler;

import org.gestion.produit.dto.ProduitRequest;
import org.gestion.produit.dto.ProduitResponse;
import org.gestion.produit.service.ProduitService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;

@Component
public class ProduitHandler {

    private final ProduitService produitService;

    public ProduitHandler(ProduitService produitService) {
        this.produitService = produitService;
    }

    /** GET /api/products, ou /api/products?name=xxx pour filtrer par nom. */
    public Mono<ServerResponse> getAllProduits(ServerRequest request) {
        Flux<ProduitResponse> produits = request.queryParam("name")
                .filter(name -> !name.isBlank())
                .map(produitService::findByNameContaining)
                .orElseGet(produitService::findAll);
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(produits, ProduitResponse.class);
    }

    public Mono<ServerResponse> getProduitById(ServerRequest request) {
        return lireId(request)
                .flatMap(produitService::findById)
                .flatMap(produit -> ServerResponse.ok().bodyValue(produit))
                .switchIfEmpty(ServerResponse.notFound().build())
                .onErrorResume(IllegalArgumentException.class, this::badRequest);
    }

    public Mono<ServerResponse> createProduit(ServerRequest request) {
        return request.bodyToMono(ProduitRequest.class)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Corps de requête manquant")))
                .flatMap(produitService::create)
                .flatMap(produit -> ServerResponse
                        .created(URI.create("/api/products/" + produit.id()))
                        .bodyValue(produit))
                .onErrorResume(IllegalArgumentException.class, this::badRequest);
    }

    public Mono<ServerResponse> updateProduit(ServerRequest request) {
        return lireId(request)
                .flatMap(id -> request.bodyToMono(ProduitRequest.class)
                        .switchIfEmpty(Mono.error(new IllegalArgumentException("Corps de requête manquant")))
                        .flatMap(body -> produitService.update(id, body)))
                .flatMap(produit -> ServerResponse.ok().bodyValue(produit))
                .switchIfEmpty(ServerResponse.notFound().build())
                .onErrorResume(IllegalArgumentException.class, this::badRequest);
    }

    public Mono<ServerResponse> deleteProduit(ServerRequest request) {
        return lireId(request)
                .flatMap(produitService::deleteById)
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
