package org.gestion.commande.service;

import org.gestion.commande.dto.CommandeAvecLignes;
import org.gestion.commande.dto.CommandeRequest;
import org.gestion.commande.dto.CommandeResponse;
import org.gestion.commande.dto.LigneCommandeRequest;
import org.gestion.commande.dto.ProduitInfo;
import org.gestion.commande.model.ClientRef;
import org.gestion.commande.model.Commande;
import org.gestion.commande.model.LigneCommande;
import org.gestion.commande.repository.ClientRefRepository;
import org.gestion.commande.repository.CommandeRepository;
import org.gestion.commande.repository.LigneCommandeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import static org.gestion.commande.constants.CommandeStatut.*;

import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;



@Service
public class CommandeService {

    private static final Logger log = LoggerFactory.getLogger(CommandeService.class);

    private final CommandeRepository commandeRepository;
    private final LigneCommandeRepository ligneCommandeRepository;
    private final TransactionalOperator transactionalOperator;
    private final MailService mailService;

    @Value("${spring.mail.username}")
    private String adminEmail;

    private final ClientRefRepository clientRefRepository;
    private final ProduitClient produitClient;

    public CommandeService(CommandeRepository commandeRepository,
                           LigneCommandeRepository ligneCommandeRepository,
                           TransactionalOperator transactionalOperator,
                           MailService mailService,
                           ClientRefRepository clientRefRepository,
                           ProduitClient produitClient) {
        this.commandeRepository = commandeRepository;
        this.ligneCommandeRepository = ligneCommandeRepository;
        this.transactionalOperator = transactionalOperator;
        this.mailService = mailService;
        this.clientRefRepository = clientRefRepository;
        this.produitClient = produitClient;
    }

    public Mono<CommandeResponse> createCommande(CommandeRequest request, String userId, String userEmail, String numeroClient) {
        return clientRefRepository.findById(userId)
                .switchIfEmpty(Mono.defer(() -> clientRefRepository.save(new ClientRef(userId, numeroClient))))
                // Les lignes sont construites AVANT tout enregistrement : un produit inconnu ou une
                // quantité invalide fait échouer la commande sans rien écrire en base.
                .flatMap(clientRef -> construireLignes(request))
                .flatMap(lignes -> {

                    Commande commande = new Commande();
                    commande.setUserId(userId);
                    commande.setReference(request.reference());
                    commande.setStatut(CREEE);
                    commande.setDateCommande(LocalDateTime.now());
                    commande.setDateLivraison(request.dateLivraison());

                    return transactionalOperator.execute(status ->
                                    commandeRepository.save(commande)
                                            .flatMap(saved -> {
                                                if (lignes.isEmpty()) {
                                                    return Mono.just(saved);
                                                }
                                                lignes.forEach(l -> l.setCommandeId(saved.getId()));
                                                return ligneCommandeRepository.saveAll(lignes)
                                                        .collectList()
                                                        .map(savedLignes -> saved);
                                            })
                            )
                            .single()
                            .flatMap(saved ->
                                    ligneCommandeRepository.findByCommandeId(saved.getId())
                                            .collectList()
                                            .flatMap(lignesEnregistrees ->
                                                    toResponseWithTotal(saved)
                                                            .flatMap(response ->
                                                                    mailService.sendConfirmationCommande(userEmail, response, lignesEnregistrees)
                                                                            .thenReturn(response)
                                                                            .onErrorResume(e -> {
                                                                                log.warn("Email de confirmation non envoyé : {}", e.getMessage());
                                                                                return Mono.just(response);
                                                                            })
                                                            )
                                            )
                            );
                });
    }

    /** Transforme les lignes demandées (produitId + quantité) en lignes avec nom et prix lus dans produit-service. */
    private Mono<List<LigneCommande>> construireLignes(CommandeRequest request) {
        List<LigneCommandeRequest> demandes = request.lignes() == null ? List.of() : request.lignes();
        return Flux.fromIterable(demandes)
                .concatMap(this::construireLigne)
                .collectList();
    }

    private Mono<LigneCommande> construireLigne(LigneCommandeRequest demande) {
        if (demande == null || demande.produitId() == null) {
            return Mono.error(new IllegalArgumentException("produitId obligatoire"));
        }
        if (demande.quantite() == null || demande.quantite() <= 0) {
            return Mono.error(new IllegalArgumentException("Quantité invalide pour le produit " + demande.produitId()));
        }
        return produitClient.getProduit(demande.produitId())
                .map(produit -> {
                    BigDecimal prixUnitaireTTC = prixTTC(produit);
                    LigneCommande ligne = new LigneCommande();
                    ligne.setProduitId(produit.id());
                    ligne.setArticle(produit.name());
                    ligne.setQuantite(demande.quantite());
                    ligne.setPrixUnitaire(prixUnitaireTTC);
                    ligne.setTauxTVA(produit.tauxTVA());
                    ligne.setTotal(prixUnitaireTTC.multiply(BigDecimal.valueOf(demande.quantite())));
                    return ligne;
                });
    }

    /** Prix TTC unitaire = HT x (1 + TVA/100), arrondi au centime. */
    static BigDecimal prixTTC(ProduitInfo produit) {
        return produit.price()
                .multiply(BigDecimal.ONE.add(produit.tauxTVA().movePointLeft(2)))
                .setScale(2, RoundingMode.HALF_UP);
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
                    if (ANNULEE.equals(commande.getStatut()) ||
                            LIVREE.equals(commande.getStatut())) {
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
                    if (!CREEE.equals(commande.getStatut())) {
                        return Mono.error(
                                new RuntimeException("Impossible d'annuler une commande " + commande.getStatut())
                        );
                    }
                    commande.setStatut(ANNULEE);
                    return commandeRepository.save(commande).then();
                });
    }


    private Mono<CommandeResponse> toResponseWithTotal(Commande commande) {
        return Mono.zip(
                ligneCommandeRepository.findByCommandeId(commande.getId())
                        .map(LigneCommande::getTotal)
                        .reduce(BigDecimal.ZERO, BigDecimal::add),
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
        Mono<List<LigneCommande>> lignesMono =
                ligneCommandeRepository.findByCommandeId(id)
                        .collectList();

        return commandeRepository.findById(id)
                .flatMap(commande -> Mono.zip(
                        toResponseWithTotal(commande),
                        lignesMono
                ))
                .map(tuple -> new CommandeAvecLignes(
                        tuple.getT1(),
                        tuple.getT2()
                ));
    }

    /** Détail d'une commande, seulement si elle appartient à l'utilisateur (vide sinon). */
    public Mono<CommandeAvecLignes> getCommandeAvecLignesPourUtilisateur(Long id, String userId) {
        return commandeRepository.findByIdAndUserId(id, userId)
                .flatMap(commande -> Mono.zip(
                        toResponseWithTotal(commande),
                        ligneCommandeRepository.findByCommandeId(commande.getId()).collectList()))
                .map(tuple -> new CommandeAvecLignes(tuple.getT1(), tuple.getT2()));
    }

    public Mono<CommandeResponse> passerEnCours(Long id) {
        return commandeRepository.findById(id)
                .switchIfEmpty(Mono.error(new RuntimeException("Commande introuvable")))
                .flatMap(commande -> {
                    if (!CREEE.equals(commande.getStatut())) {
                        return Mono.error(new RuntimeException("Impossible de passer en cours une commande " + commande.getStatut()));
                    }
                    commande.setStatut(EN_COURS);
                    return commandeRepository.save(commande);
                })
                .flatMap(this::toResponseWithTotal);
    }
}