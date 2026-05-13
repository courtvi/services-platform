package org.gestion.commande.service;

import org.gestion.commande.dto.CommandeRequest;
import org.gestion.commande.dto.CommandeResponse;
import org.gestion.commande.model.Commande;
import org.gestion.commande.repository.CommandeRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
public class CommandeService {

    private final CommandeRepository commandeRepository;

    public CommandeService(CommandeRepository commandeRepository) {
        this.commandeRepository = commandeRepository;
    }

    // ✅ CREATE — userId extrait du JWT dans le Controller
    public Mono<CommandeResponse> createCommande(CommandeRequest request, String userId) {
        Commande commande = new Commande();
        commande.setUserId(userId);
        commande.setReference(request.reference());
        commande.setStatut("CREEE");                    // imposé côté serveur
        commande.setDateCommande(LocalDateTime.now());  // imposé côté serveur

        return commandeRepository.save(commande)
                .map(this::toResponse);
    }

    // ✅ READ ONE — ROLE_CLIENT : seulement sa commande
    public Mono<CommandeResponse> getCommandeByIdForUser(Long id, String userId) {
        return commandeRepository.findByIdAndUserId(id, userId)
                .map(this::toResponse)
                .switchIfEmpty(Mono.error(
                        new RuntimeException("Commande introuvable ou accès refusé")
                ));
    }

    // ✅ READ ALL — ROLE_CLIENT : seulement ses commandes
    public Flux<CommandeResponse> getCommandesByUser(String userId) {
        return commandeRepository.findByUserId(userId)
                .map(this::toResponse);
    }

    // ✅ READ ALL — ROLE_ADMIN : toutes les commandes
    public Flux<CommandeResponse> getAllCommandes() {
        return commandeRepository.findAll()
                .map(this::toResponse);
    }

    // ✅ UPDATE — modification de la référence uniquement (statut géré séparément)
    public Mono<CommandeResponse> updateCommande(Long id, CommandeRequest request, String userId) {
        return commandeRepository.findByIdAndUserId(id, userId)
                .switchIfEmpty(Mono.error(
                        new RuntimeException("Commande introuvable ou accès refusé")
                ))
                .flatMap(commande -> {
                    if ("ANNULEE".equals(commande.getStatut()) ||
                            "LIVREE".equals(commande.getStatut())) {
                        return Mono.error(
                                new RuntimeException("Impossible de modifier une commande " + commande.getStatut())
                        );
                    }
                    commande.setReference(request.reference());
                    return commandeRepository.save(commande);
                })
                .map(this::toResponse);
    }

    // ✅ DELETE (annulation) — seulement si statut CREEE
    public Mono<Void> annulerCommande(Long id, String userId) {
        return commandeRepository.findByIdAndUserId(id, userId)
                .switchIfEmpty(Mono.error(
                        new RuntimeException("Commande introuvable ou accès refusé")
                ))
                .flatMap(commande -> {
                    if (!"CREEE".equals(commande.getStatut())) {
                        return Mono.error(
                                new RuntimeException("Impossible d'annuler une commande " + commande.getStatut())
                        );
                    }
                    commande.setStatut("ANNULEE");
                    return commandeRepository.save(commande).then();
                });
    }

    // ✅ Mapping entité → record DTO
    private CommandeResponse toResponse(Commande commande) {
        return new CommandeResponse(
                commande.getId(),
                commande.getUserId(),
                commande.getReference(),
                commande.getStatut(),
                commande.getDateCommande()
        );
    }
}