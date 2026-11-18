package org.gestion.devis.handler;

import org.gestion.devis.dto.DevisRequest;
import org.gestion.devis.dto.DevisResponse;
import org.gestion.devis.dto.EnvoiRequest;
import org.gestion.devis.dto.LigneDevisRequest;
import org.gestion.devis.service.DevisService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.net.URI;

/**
 * Traduction HTTP : vide → 404, IllegalArgumentException → 400, IllegalStateException → 409.
 */
@Component
public class DevisHandler {

    private final DevisService devisService;

    public DevisHandler(DevisService devisService) {
        this.devisService = devisService;
    }

    public Mono<ServerResponse> getDevis(ServerRequest request) {
        return ServerResponse.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(devisService.lister(), DevisResponse.class);
    }

    public Mono<ServerResponse> getDevisById(ServerRequest request) {
        return repondre(lireId(request, "id").flatMap(devisService::detail));
    }

    public Mono<ServerResponse> createDevis(ServerRequest request) {
        return request.bodyToMono(DevisRequest.class)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Corps de requête manquant")))
                .flatMap(devisService::creer)
                .flatMap(devis -> ServerResponse
                        .created(URI.create("/api/devis/" + devis.id()))
                        .bodyValue(devis))
                .onErrorResume(IllegalArgumentException.class, e -> erreur(HttpStatus.BAD_REQUEST, e))
                .onErrorResume(IllegalStateException.class, e -> erreur(HttpStatus.CONFLICT, e));
    }

    public Mono<ServerResponse> nouvelleVersion(ServerRequest request) {
        return repondre(lireId(request, "id").flatMap(devisService::nouvelleVersion));
    }

    public Mono<ServerResponse> ajouterLigne(ServerRequest request) {
        return repondre(lireId(request, "id")
                .flatMap(id -> request.bodyToMono(LigneDevisRequest.class)
                        .switchIfEmpty(Mono.error(new IllegalArgumentException("Corps de requête manquant")))
                        .flatMap(body -> devisService.ajouterLigne(id, body))));
    }

    public Mono<ServerResponse> supprimerLigne(ServerRequest request) {
        return repondre(lireId(request, "id")
                .zipWith(lireId(request, "ligneId"))
                .flatMap(ids -> devisService.supprimerLigne(ids.getT1(), ids.getT2())));
    }

    public Mono<ServerResponse> envoyer(ServerRequest request) {
        return repondre(lireId(request, "id")
                .flatMap(id -> request.bodyToMono(EnvoiRequest.class)
                        .defaultIfEmpty(new EnvoiRequest(null, null))
                        .flatMap(body -> devisService.envoyer(id, body))));
    }

    public Mono<ServerResponse> accepter(ServerRequest request) {
        return repondre(lireId(request, "id").flatMap(devisService::accepter));
    }

    public Mono<ServerResponse> refuser(ServerRequest request) {
        return repondre(lireId(request, "id").flatMap(devisService::refuser));
    }

    private Mono<ServerResponse> repondre(Mono<DevisResponse> resultat) {
        return resultat
                .flatMap(devis -> ServerResponse.ok().bodyValue(devis))
                .switchIfEmpty(ServerResponse.notFound().build())
                .onErrorResume(IllegalArgumentException.class, e -> erreur(HttpStatus.BAD_REQUEST, e))
                .onErrorResume(IllegalStateException.class, e -> erreur(HttpStatus.CONFLICT, e));
    }

    /** NumberFormatException hérite d'IllegalArgumentException : un id non numérique donne 400. */
    private Mono<Long> lireId(ServerRequest request, String nom) {
        return Mono.fromCallable(() -> Long.parseLong(request.pathVariable(nom)));
    }

    private Mono<ServerResponse> erreur(HttpStatus status, RuntimeException e) {
        return ServerResponse.status(status).bodyValue(e.getMessage());
    }
}
