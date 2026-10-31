package org.gestion.produit.reactivewebservice;

import org.gestion.produit.dto.ProduitResponse;
import org.gestion.produit.model.Produit;
import org.gestion.produit.service.ProduitService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import reactor.core.publisher.Mono;

import java.net.URI;

@Component
public class ProduitHandler {

    private final ProduitService produitService;

    public ProduitHandler(ProduitService produitService) {
        this.produitService = produitService;
    }

    public Mono<ServerResponse> getAllProduits(ServerRequest request) {

        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(produitService.findAll(), ProduitResponse.class);
    }

    public Mono<ServerResponse> createProduit(ServerRequest request) {
        return request.bodyToMono(Produit.class)
                .flatMap(produitService::save)
                .flatMap(savedProduit ->
                        ServerResponse
                                .created(URI.create("/api/produits/" + savedProduit.getId()))
                                .bodyValue(savedProduit)
                );
    }
}
