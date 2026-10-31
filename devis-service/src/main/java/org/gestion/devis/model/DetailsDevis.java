package org.gestion.devis.model;

import org.gestion.client.model.Client;
import org.gestion.produit.model.Produit;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.Date;

public class DetailsDevis {

    private Long devisId;
    private Produit produit;
    private Integer quantite;
    private Double total;
    private Double prixUnitaire;

    public DetailsDevis(Long devisId, Produit produit, Integer quantite, Double prixUnitaire, Double total) {
        this.devisId = devisId;
        this.produit = produit;
        this.quantite = quantite;
        this.prixUnitaire = prixUnitaire;
        this.total = total;
    }

    public Long getDevisId() {
        return devisId;
    }
    public void setDevisId(Long devisId) { this.devisId = devisId;}

    public Produit getProduit() { return produit; }

    public void setProduit(Produit produit) {this.produit = produit;}

    public Integer getQuantite() { return quantite; }

    public void setQuantite(Integer quantite) {this.quantite = quantite; }

    public Double getPrixUnitaire()  { return prixUnitaire; }
    public void setPrixUnitaire(Double prixUnitaire) { this.prixUnitaire = prixUnitaire; }

    public Double getTotal() { return total; }
    public void setTotal(Double total) { this.total = total; }
}

