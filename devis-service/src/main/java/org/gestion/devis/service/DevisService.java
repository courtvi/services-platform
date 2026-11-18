package org.gestion.devis.service;

import org.gestion.devis.dto.DevisRequest;
import org.gestion.devis.dto.DevisResponse;
import org.gestion.devis.dto.EnvoiRequest;
import org.gestion.devis.dto.LigneDevisRequest;
import org.gestion.devis.dto.LigneDevisResponse;
import org.gestion.devis.dto.VersionDevisResponse;
import org.gestion.devis.model.Devis;
import org.gestion.devis.model.LigneDevis;
import org.gestion.devis.model.VersionDevis;
import org.gestion.devis.repository.DevisRepository;
import org.gestion.devis.repository.LigneDevisRepository;
import org.gestion.devis.repository.VersionDevisRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;

/**
 * Orchestration : charge les versions et les lignes, applique les règles du modèle, enregistre.
 * Un Mono vide signifie « devis (ou ligne) introuvable » : le handler renvoie 404.
 * IllegalArgumentException = données invalides (400), IllegalStateException = action interdite dans cet état (409).
 */
@Service
public class DevisService {

    private static final int VALIDITE_PAR_DEFAUT_JOURS = 30;

    private final DevisRepository devisRepository;
    private final VersionDevisRepository versionRepository;
    private final LigneDevisRepository ligneRepository;
    private final ProduitClient produitClient;

    public DevisService(DevisRepository devisRepository,
                        VersionDevisRepository versionRepository,
                        LigneDevisRepository ligneRepository,
                        ProduitClient produitClient) {
        this.devisRepository = devisRepository;
        this.versionRepository = versionRepository;
        this.ligneRepository = ligneRepository;
        this.produitClient = produitClient;
    }

    // ------------------------------------------------------------------ lecture

    public Flux<DevisResponse> lister() {
        return devisRepository.findAll().concatMap(this::chargerDetail);
    }

    public Mono<DevisResponse> detail(Long devisId) {
        return devisRepository.findById(devisId).flatMap(this::chargerDetail);
    }

    // ------------------------------------------------------------------ écriture

    /** Crée le devis, sa référence DEV-année-id et une version 1 vide en brouillon. */
    @Transactional
    public Mono<DevisResponse> creer(DevisRequest request) {
        if (request == null || request.clientId() == null) {
            return Mono.error(new IllegalArgumentException("clientId obligatoire"));
        }
        Devis devis = new Devis(request.clientId(), LocalDateTime.now());
        return devisRepository.save(devis)
                .flatMap(d -> {
                    d.setReference(String.format("DEV-%d-%05d", d.getDateCreation().getYear(), d.getId()));
                    return devisRepository.save(d);
                })
                .flatMap(d -> versionRepository.save(VersionDevis.premiere(d.getId())).thenReturn(d))
                .flatMap(d -> detail(d.getId()));
    }

    /** Nouvelle version copiée de la dernière (qui passe en REMPLACEE si elle était envoyée). */
    @Transactional
    public Mono<DevisResponse> nouvelleVersion(Long devisId) {
        return derniereVersion(devisId)
                .flatMap(precedente -> {
                    VersionDevis suivante = VersionDevis.suivante(precedente);
                    return versionRepository.save(precedente)
                            .then(Mono.defer(() -> versionRepository.save(suivante)))
                            .flatMap(saved -> Flux.fromIterable(suivante.getLignes())
                                    .doOnNext(ligne -> ligne.setVersionId(saved.getId()))
                                    .concatMap(ligneRepository::save)
                                    .then(Mono.defer(() -> detail(devisId))));
                });
    }

    /** Ajoute un produit à la version brouillon. Prix, nom et TVA sont lus dans produit-service. */
    @Transactional
    public Mono<DevisResponse> ajouterLigne(Long devisId, LigneDevisRequest request) {
        if (request == null || request.produitId() == null) {
            return Mono.error(new IllegalArgumentException("produitId obligatoire"));
        }
        return derniereVersion(devisId)
                .flatMap(version -> {
                    version.verifierModifiable();
                    return produitClient.getProduit(request.produitId())
                            .map(produit -> {
                                LigneDevis ligne = new LigneDevis(produit, request.quantite());
                                version.ajouterLigne(ligne);
                                ligne.setVersionId(version.getId());
                                return ligne;
                            })
                            .flatMap(ligneRepository::save)
                            .then(Mono.defer(() -> detail(devisId)));
                });
    }

    /** Retire une ligne de la version brouillon. Vide si la ligne n'appartient pas à cette version. */
    @Transactional
    public Mono<DevisResponse> supprimerLigne(Long devisId, Long ligneId) {
        return derniereVersion(devisId)
                .flatMap(version -> {
                    version.verifierModifiable();
                    return Mono.justOrEmpty(version.getLignes().stream()
                                    .filter(ligne -> ligne.getId().equals(ligneId))
                                    .findFirst())
                            .flatMap(ligne -> ligneRepository.delete(ligne)
                                    .then(Mono.defer(() -> detail(devisId))));
                });
    }

    /** Envoie la version brouillon : fige les dates et les totaux. */
    @Transactional
    public Mono<DevisResponse> envoyer(Long devisId, EnvoiRequest request) {
        int jours = request == null || request.joursValidite() == null
                ? VALIDITE_PAR_DEFAUT_JOURS
                : request.joursValidite();
        String commentaire = request == null ? null : request.commentaire();
        return modifierDerniereVersion(devisId, version -> version.envoyer(jours, commentaire));
    }

    @Transactional
    public Mono<DevisResponse> accepter(Long devisId) {
        return modifierDerniereVersion(devisId, VersionDevis::accepter);
    }

    @Transactional
    public Mono<DevisResponse> refuser(Long devisId) {
        return modifierDerniereVersion(devisId, VersionDevis::refuser);
    }

    // ------------------------------------------------------------------ interne

    private Mono<DevisResponse> modifierDerniereVersion(Long devisId, Consumer<VersionDevis> action) {
        return derniereVersion(devisId)
                .flatMap(version -> {
                    action.accept(version);
                    return versionRepository.save(version);
                })
                .flatMap(version -> detail(devisId));
    }

    private Mono<VersionDevis> derniereVersion(Long devisId) {
        return versionRepository.findFirstByDevisIdOrderByNumeroVersionDesc(devisId)
                .flatMap(this::chargerLignes);
    }

    private Mono<VersionDevis> chargerLignes(VersionDevis version) {
        return ligneRepository.findByVersionId(version.getId())
                .collectList()
                .map(lignes -> {
                    version.setLignes(lignes);
                    return version;
                });
    }

    private Mono<DevisResponse> chargerDetail(Devis devis) {
        return versionRepository.findByDevisIdOrderByNumeroVersionAsc(devis.getId())
                .concatMap(this::chargerLignes)
                .map(this::toVersionResponse)
                .collectList()
                .map(versions -> new DevisResponse(
                        devis.getId(),
                        devis.getReference(),
                        devis.getClientId(),
                        devis.getDateCreation(),
                        versions));
    }

    /** Totaux figés si la version a été envoyée, sinon calculés à la volée sur les lignes. */
    private VersionDevisResponse toVersionResponse(VersionDevis version) {
        VersionDevis.Totaux totaux = version.getTotalHT() != null
                ? new VersionDevis.Totaux(version.getTotalHT(), version.getTotalTVA(), version.getTotalTTC())
                : version.calculerTotaux();
        List<LigneDevisResponse> lignes = version.getLignes().stream()
                .map(ligne -> new LigneDevisResponse(
                        ligne.getId(),
                        ligne.getProduitId(),
                        ligne.getName(),
                        ligne.getQuantite(),
                        ligne.getPrixUnitaireHT(),
                        ligne.getTauxTVA(),
                        ligne.getTotalLigneHT(),
                        ligne.getTotalLigneTTC()))
                .toList();
        return new VersionDevisResponse(
                version.getId(),
                version.getNumeroVersion(),
                version.getStatut(),
                version.getDateEmission(),
                version.getDateValidite(),
                version.getCommentaire(),
                totaux.ht(),
                totaux.tva(),
                totaux.ttc(),
                lignes);
    }
}
