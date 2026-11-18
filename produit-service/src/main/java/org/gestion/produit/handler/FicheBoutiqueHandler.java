package org.gestion.produit.handler;

import org.gestion.produit.dto.ProduitBoutiqueResponse;
import org.gestion.produit.service.FicheBoutiqueService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class FicheBoutiqueHandler {

    private final FicheBoutiqueService ficheBoutiqueService;

    public FicheBoutiqueHandler(FicheBoutiqueService ficheBoutiqueService) {
        this.ficheBoutiqueService = ficheBoutiqueService;
    }

    /** GET /api/products/boutique/{boutiqueId} */
    public Mono<ServerResponse> getCatalogue(ServerRequest request) {
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(ficheBoutiqueService.catalogue(request.pathVariable("boutiqueId")), ProduitBoutiqueResponse.class);
    }

    /** GET /api/products/boutique/{boutiqueId}/{produitId} */
    public Mono<ServerResponse> getProduitBoutique(ServerRequest request) {
        String boutiqueId = request.pathVariable("boutiqueId");
        return Mono.fromCallable(() -> Long.parseLong(request.pathVariable("produitId")))
                .flatMap(produitId -> ficheBoutiqueService.produit(boutiqueId, produitId))
                .flatMap(produit -> ServerResponse.ok().bodyValue(produit))
                .switchIfEmpty(ServerResponse.notFound().build())
                .onErrorResume(NumberFormatException.class,
                        e -> ServerResponse.badRequest().bodyValue("Identifiant invalide"));
    }
}
