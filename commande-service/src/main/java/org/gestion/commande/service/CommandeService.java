package org.gestion.commande.service;

import org.gestion.commande.dto.CommandeAvecLignes;
import org.gestion.commande.dto.CommandeRequest;
import org.gestion.commande.dto.CommandeResponse;
import org.gestion.commande.model.ClientRef;
import org.gestion.commande.model.Commande;
import org.gestion.commande.model.LigneCommande;
import org.gestion.commande.repository.ClientRefRepository;
import org.gestion.commande.repository.CommandeRepository;
import org.gestion.commande.repository.LigneCommandeRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CommandeService {

    private final CommandeRepository commandeRepository;
    private final LigneCommandeRepository ligneCommandeRepository;
    private final TransactionalOperator transactionalOperator;
    private final MailService mailService;

    @Value("${spring.mail.username}")
    private String adminEmail;

    private final ClientRefRepository clientRefRepository;

    public CommandeService(CommandeRepository commandeRepository,
                           LigneCommandeRepository ligneCommandeRepository,
                           TransactionalOperator transactionalOperator,
                           MailService mailService,
                           ClientRefRepository clientRefRepository) {
        this.commandeRepository = commandeRepository;
        this.ligneCommandeRepository = ligneCommandeRepository;
        this.transactionalOperator = transactionalOperator;
        this.mailService = mailService;
        this.clientRefRepository = clientRefRepository;
    }

    public Mono<CommandeResponse> createCommande(CommandeRequest request, String userId, String userEmail, String numeroClient) {
        return clientRefRepository.findById(userId)
                .switchIfEmpty(clientRefRepository.save(new ClientRef(userId, numeroClient)))
                .flatMap(clientRef -> {

        Commande commande = new Commande();
        commande.setUserId(userId);
        commande.setReference(request.reference());
        commande.setStatut("CREEE");
        commande.setDateCommande(LocalDateTime.now());
        commande.setDateLivraison(request.dateLivraison());

        return transactionalOperator.execute(status ->
                        commandeRepository.save(commande)
                                .flatMap(saved -> {

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
                                            .map(savedLignes -> saved);
                                })
                )
                .single()
                .flatMap(saved ->
                        ligneCommandeRepository.findByCommandeId(saved.getId())
                                .collectList()
                                .flatMap(lignes -> {
                                    System.out.println(">>> Lignes récupérées pour mail: " + lignes.size());
                                    return toResponseWithTotal(saved)
                                            .flatMap(response ->
                                                    mailService.sendConfirmationCommande(userEmail, response, lignes)
                                                            .thenReturn(response)
                                                            .onErrorResume(e -> {
                                                                System.err.println("⚠️ Email non envoyé : " + e.getMessage());
                                                                return Mono.just(response);
                                                            })
                                            );
                                })
                );
    });
    }

    public Mono<CommandeResponse> getCommandeByIdForUser(Long id, String userId) {
        return commandeRepository.findByIdAndUserId(id, userId)
                .flatMap(this::toResponseWithTotal)
                .switchIfEmpty(Mono.error(
                        new RuntimeException("Commande introuvable ou accès refusé")
                ));
    }


    public Flux<CommandeResponse> getCommandesByUser(String userId) {
        return commandeRepository.findByUserId(userId)
                .flatMap(this::toResponseWithTotal);
    }


    public Flux<CommandeResponse> getAllCommandes() {
        return commandeRepository.findAll()
                .flatMap(this::toResponseWithTotal);
    }


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
                .flatMap(this::toResponseWithTotal);
    }


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


    private Mono<CommandeResponse> toResponseWithTotal(Commande commande) {
        return Mono.zip(
                ligneCommandeRepository.findByCommandeId(commande.getId())
                        .map(LigneCommande::getTotal)
                        .reduce(0.0, Double::sum),
                clientRefRepository.findById(commande.getUserId())
                        .map(ClientRef::getNumeroClient)
                        .defaultIfEmpty("N/A")
        ).map(tuple -> new CommandeResponse(
                commande.getId(),
                commande.getUserId(),
                tuple.getT2(),
                commande.getReference(),
                commande.getStatut(),
                commande.getDateCommande(),
                commande.getDateLivraison(),
                tuple.getT1()
        ));
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

    public Mono<CommandeResponse> passerEnCours(Long id) {
        return commandeRepository.findById(id)
                .switchIfEmpty(Mono.error(new RuntimeException("Commande introuvable")))
                .flatMap(commande -> {
                    if (!"CREEE".equals(commande.getStatut())) {
                        return Mono.error(new RuntimeException("Impossible de passer en cours une commande " + commande.getStatut()));
                    }
                    commande.setStatut("EN_COURS");
                    return commandeRepository.save(commande);
                })
                .flatMap(this::toResponseWithTotal);
    }
}