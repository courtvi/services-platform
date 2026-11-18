package org.gestion.commande.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

@Table(name = "LIGNECOMMANDES")
public class LigneCommande {

    @Id
    @Column("ID")
    private Long id;

    @Column("COMMANDE_ID")
    private Long commandeId;

    @Column("PRODUIT_ID")
    private Long produitId;

    @Column("ARTICLE")
    private String article;

    @Column("QUANTITE")
    private Integer quantite;

    @Column("PRIX_UNITAIRE")
    private BigDecimal prixUnitaire;   // prix unitaire TTC, figé à la commande

    @Column("TOTAL")
    private BigDecimal total;          // total TTC de la ligne

    @Column("TAUX_TVA")
    private BigDecimal tauxTVA;        // taux de TVA du produit au moment de la commande



    public LigneCommande(Long id, String article, Integer quantite, BigDecimal prixUnitaire, BigDecimal total) {
        this.id = id;
        this.article = article;
        this.quantite = quantite;
        this.prixUnitaire = prixUnitaire;
        this.total = total;
    }
    public LigneCommande() {
    }

    public Long getCommandeId() {
        return commandeId;
    }

    public void setCommandeId(Long commandeId) {
        this.commandeId = commandeId;
    }

    public BigDecimal getTauxTVA() { return tauxTVA; }
    public void setTauxTVA(BigDecimal tauxTVA) { this.tauxTVA = tauxTVA; }

    public Long getProduitId() { return produitId; }
    public void setProduitId(Long produitId) { this.produitId = produitId; }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getArticle() { return article; }
    public void setArticle(String article) { this.article = article; }

    public Integer getQuantite() { return quantite; }
    public void setQuantite(Integer quantite) { this.quantite = quantite; }

    public BigDecimal getPrixUnitaire() { return prixUnitaire; }
    public void setPrixUnitaire(BigDecimal prixUnitaire) { this.prixUnitaire = prixUnitaire; }

    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }

}