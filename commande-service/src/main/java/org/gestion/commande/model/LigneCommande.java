package org.gestion.commande.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table(name = "LIGNECOMMANDES")
public class LigneCommande {

    @Id
    private Long id;

    private Long commandeId;

    @Column("ARTICLE")
    private String article;

    @Column("QUANTITE")
    private Integer quantite;

    public LigneCommande(Long id, String article, Integer quantite) {
        this.id = id;
        this.article = article;
        this.quantite = quantite;
    }
    public LigneCommande() {
    }

    public Long getCommandeId() {
        return commandeId;
    }

    public void setCommandeId(Long commandeId) {
        this.commandeId = commandeId;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getArticle() { return article; }
    public void setArticle(String article) { this.article = article; }

    public Integer getQuantite() { return quantite; }
    public void setQuantite(Integer quantite) { this.quantite = quantite; }

}