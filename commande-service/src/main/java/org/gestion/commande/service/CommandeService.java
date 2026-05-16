package org.gestion.commande.service;

import org.gestion.commande.dto.CommandeAvecLignes;
import org.gestion.commande.dto.CommandeRequest;
import org.gestion.commande.dto.CommandeResponse;
import org.gestion.commande.model.Commande;
import org.gestion.commande.model.LigneCommande;
import org.gestion.commande.repository.CommandeRepository;
import org.gestion.commande.repository.LigneCommandeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class CommandeService {

    private final CommandeRepository commandeRepository;
    private final LigneCommandeRepository ligneCommandeRepository;
    private final TransactionalOperator transactionalOperator;

    public CommandeService(CommandeRepository commandeRepository,
                           LigneCommandeRepository ligneCommandeRepository,
                           TransactionalOperator transactionalOperator) {
        this.commandeRepository = commandeRepository;
        this.ligneCommandeRepository = ligneCommandeRepository;
        this.transactionalOperator = transactionalOperator;
    }

    public Mono<CommandeResponse> createCommande(CommandeRequest request, String userId) {

        Commande commande = new Commande();
        commande.setUserId(userId);
        commande.setReference(request.reference());
        commande.setStatut("CREEE");
        commande.setDateCommande(LocalDateTime.now());
        commande.setDateLivraison(request.dateLivraison());
        return transactionalOperator.execute(status ->
                        commandeRepository.save(commande)
                                .flatMap(saved -> {
                                    // ✅ Lignes optionnelles
                                    if (request.lignes() == null || request.lignes().isEmpty()) {
                                        return Mono.just(saved);
                                    }

                                    List<LigneCommande> lignes = request.lignes().stream()
                                            .map(dto -> {
                                                LigneCommande l = new LigneCommande();
                                                l.setCommandeId(saved.getId());
                                                l.setArticle(dto.article());
                                                l.setQuantite(dto.quantite());
                                                l.setPrixUnitaire(dto.prixUnitaire());
                                                l.setTotal(dto.total());
                                                return l;
                                            })
                                            .toList();

                                    return ligneCommandeRepository.saveAll(lignes)
                                            .collectList()
                                            .map(l -> saved);
                                })
                )
                .single()
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


    private CommandeResponse toResponse(Commande commande) {
        return new CommandeResponse(
                commande.getId(),
                commande.getUserId(),
                commande.getReference(),
                commande.getStatut(),
                commande.getDateCommande(),
                commande.getDateLivraison()
        );
    }


    public Mono<CommandeAvecLignes> getCommande(Long id) {

        Mono<Commande> commandeMono = commandeRepository.findById(id);

        Mono<List<LigneCommande>> lignesMono =
                ligneCommandeRepository.findByCommandeId(id)
                        .collectList();

        return Mono.zip(commandeMono, lignesMono)
                .map(tuple -> new CommandeAvecLignes(
                        tuple.getT1(),
                        tuple.getT2()
                ));
    }
}