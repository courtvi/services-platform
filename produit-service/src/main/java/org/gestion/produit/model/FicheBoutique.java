package org.gestion.produit.model;

import org.springframework.data.annotation.Id;

/**
 * Fiche d'un produit dans une boutique (composition) : tout ce qui est propre à l'affichage
 * en boutique. Le prix et la TVA restent dans {@link Produit}, référencé par produitId.
 * Un même produit peut avoir une fiche dans plusieurs boutiques.
 */
public class FicheBoutique {

    @Id
    private Long id;
    private Long produitId;
    private String boutiqueId;
    private String origine;
    private String unite;
    private String categorie;
    private String tagCle;
    private String image;
    private Boolean rupture;
    private String annee;
    private Boolean actif;      // false = fiche masquée du catalogue

    public FicheBoutique() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getProduitId() { return produitId; }
    public void setProduitId(Long produitId) { this.produitId = produitId; }

    public String getBoutiqueId() { return boutiqueId; }
    public void setBoutiqueId(String boutiqueId) { this.boutiqueId = boutiqueId; }

    public String getOrigine() { return origine; }
    public void setOrigine(String origine) { this.origine = origine; }

    public String getUnite() { return unite; }
    public void setUnite(String unite) { this.unite = unite; }

    public String getCategorie() { return categorie; }
    public void setCategorie(String categorie) { this.categorie = categorie; }

    public String getTagCle() { return tagCle; }
    public void setTagCle(String tagCle) { this.tagCle = tagCle; }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    public Boolean getRupture() { return rupture; }
    public void setRupture(Boolean rupture) { this.rupture = rupture; }

    public String getAnnee() { return annee; }
    public void setAnnee(String annee) { this.annee = annee; }

    public Boolean getActif() { return actif; }
    public void setActif(Boolean actif) { this.actif = actif; }
}
