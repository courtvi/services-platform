package org.gestion.devis.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Une version du contenu d'un devis. Toutes les règles métier sont ici :
 * une version n'est modifiable qu'en BROUILLON, l'envoi fige les dates et les totaux, etc.
 */
public class VersionDevis {

    @Id
    private Long id;
    private Long devisId;
    private Integer numeroVersion;
    private StatutVersion statut;
    private LocalDate dateEmission;
    private LocalDate dateValidite;
    private String commentaire;
    private BigDecimal totalHT;
    private BigDecimal totalTVA;
    private BigDecimal totalTTC;

    /** Pas de colonne : les lignes sont dans la table ligne_devis et chargées à part. */
    @Transient
    private List<LigneDevis> lignes = new ArrayList<>();

    public record Totaux(BigDecimal ht, BigDecimal tva, BigDecimal ttc) {
    }

    public VersionDevis() {
    }

    /** Version 1, vide, en brouillon. */
    public static VersionDevis premiere(Long devisId) {
        VersionDevis v = new VersionDevis();
        v.devisId = devisId;
        v.numeroVersion = 1;
        v.statut = StatutVersion.BROUILLON;
        return v;
    }

    /**
     * Nouvelle version en brouillon, copie de la précédente.
     * Si la précédente était envoyée, elle passe en REMPLACEE.
     */
    public static VersionDevis suivante(VersionDevis precedente) {
        switch (precedente.statut) {
            case BROUILLON -> throw new IllegalStateException("Une version brouillon existe déjà");
            case ACCEPTEE -> throw new IllegalStateException("Devis déjà accepté");
            case ENVOYEE -> precedente.statut = StatutVersion.REMPLACEE;
            default -> { }
        }
        VersionDevis v = new VersionDevis();
        v.devisId = precedente.devisId;
        v.numeroVersion = precedente.numeroVersion + 1;
        v.statut = StatutVersion.BROUILLON;
        v.commentaire = precedente.commentaire;
        precedente.lignes.forEach(l -> v.lignes.add(l.copie()));
        return v;
    }

    public void verifierModifiable() {
        if (statut != StatutVersion.BROUILLON) {
            throw new IllegalStateException("Version " + numeroVersion + " verrouillée (" + statut + ")");
        }
    }

    public void ajouterLigne(LigneDevis ligne) {
        verifierModifiable();
        lignes.add(ligne);
    }

    public void envoyer(int joursValidite, String commentaire) {
        verifierModifiable();
        if (lignes.isEmpty()) throw new IllegalStateException("Impossible d'envoyer un devis sans ligne");
        if (joursValidite <= 0) throw new IllegalArgumentException("La validité doit être d'au moins 1 jour");

        this.dateEmission = LocalDate.now();
        this.dateValidite = dateEmission.plusDays(joursValidite);
        if (commentaire != null) this.commentaire = commentaire;
        Totaux totaux = calculerTotaux();
        this.totalHT = totaux.ht();
        this.totalTVA = totaux.tva();
        this.totalTTC = totaux.ttc();
        this.statut = StatutVersion.ENVOYEE;
    }

    public void accepter() {
        verifierEnvoyeeEtValide();
        this.statut = StatutVersion.ACCEPTEE;
    }

    public void refuser() {
        verifierEnvoyeeEtValide();
        this.statut = StatutVersion.REFUSEE;
    }

    private void verifierEnvoyeeEtValide() {
        if (statut != StatutVersion.ENVOYEE) {
            throw new IllegalStateException("Seule une version envoyée peut être acceptée ou refusée");
        }
        if (dateValidite != null && LocalDate.now().isAfter(dateValidite)) {
            throw new IllegalStateException("Devis expiré depuis le " + dateValidite);
        }
    }

    public Totaux calculerTotaux() {
        BigDecimal ht = BigDecimal.ZERO.setScale(2);
        BigDecimal tva = BigDecimal.ZERO.setScale(2);
        for (LigneDevis ligne : lignes) {
            ht = ht.add(ligne.getTotalLigneHT());
            tva = tva.add(ligne.getMontantTVA());
        }
        return new Totaux(ht, tva, ht.add(tva));
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getDevisId() { return devisId; }
    public void setDevisId(Long devisId) { this.devisId = devisId; }

    public Integer getNumeroVersion() { return numeroVersion; }
    public void setNumeroVersion(Integer numeroVersion) { this.numeroVersion = numeroVersion; }

    public StatutVersion getStatut() { return statut; }
    public void setStatut(StatutVersion statut) { this.statut = statut; }

    public LocalDate getDateEmission() { return dateEmission; }
    public void setDateEmission(LocalDate dateEmission) { this.dateEmission = dateEmission; }

    public LocalDate getDateValidite() { return dateValidite; }
    public void setDateValidite(LocalDate dateValidite) { this.dateValidite = dateValidite; }

    public String getCommentaire() { return commentaire; }
    public void setCommentaire(String commentaire) { this.commentaire = commentaire; }

    public BigDecimal getTotalHT() { return totalHT; }
    public void setTotalHT(BigDecimal totalHT) { this.totalHT = totalHT; }

    public BigDecimal getTotalTVA() { return totalTVA; }
    public void setTotalTVA(BigDecimal totalTVA) { this.totalTVA = totalTVA; }

    public BigDecimal getTotalTTC() { return totalTTC; }
    public void setTotalTTC(BigDecimal totalTTC) { this.totalTTC = totalTTC; }

    public List<LigneDevis> getLignes() { return lignes; }
    public void setLignes(List<LigneDevis> lignes) { this.lignes = new ArrayList<>(lignes); }
}
