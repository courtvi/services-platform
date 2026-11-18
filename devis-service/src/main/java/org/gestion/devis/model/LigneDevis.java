package org.gestion.devis.model;

import org.gestion.devis.dto.ProduitInfo;
import org.springframework.data.annotation.Id;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Ligne d'une version de devis. Le nom, le prix et la TVA sont copiés depuis le produit
 * au moment de l'ajout : si le catalogue change ensuite, le devis ne bouge pas.
 */
public class LigneDevis {

    @Id
    private Long id;
    private Long versionId;
    private Long produitId;
    private String name;
    private Integer quantite;
    private BigDecimal prixUnitaireHT;
    private BigDecimal tauxTVA;

    public LigneDevis() {
    }

    public LigneDevis(ProduitInfo produit, Integer quantite) {
        if (produit == null) throw new IllegalArgumentException("Produit obligatoire");
        if (quantite == null || quantite <= 0) throw new IllegalArgumentException("Quantité invalide");

        this.produitId = produit.id();
        this.quantite = quantite;
        this.name = produit.name();
        this.prixUnitaireHT = produit.price();
        this.tauxTVA = produit.tauxTVA();
    }

    /** Copie pour une nouvelle version : sans id ni version, ils seront attribués à l'enregistrement. */
    LigneDevis copie() {
        LigneDevis c = new LigneDevis();
        c.produitId = produitId;
        c.name = name;
        c.quantite = quantite;
        c.prixUnitaireHT = prixUnitaireHT;
        c.tauxTVA = tauxTVA;
        return c;
    }

    public BigDecimal getTotalLigneHT() {
        return prixUnitaireHT
                .multiply(BigDecimal.valueOf(quantite))
                .setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal getMontantTVA() {
        return getTotalLigneHT()
                .multiply(tauxTVA)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    public BigDecimal getTotalLigneTTC() {
        return getTotalLigneHT().add(getMontantTVA());
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getVersionId() { return versionId; }
    public void setVersionId(Long versionId) { this.versionId = versionId; }

    public Long getProduitId() { return produitId; }
    public void setProduitId(Long produitId) { this.produitId = produitId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getQuantite() { return quantite; }
    public void setQuantite(Integer quantite) { this.quantite = quantite; }

    public BigDecimal getPrixUnitaireHT() { return prixUnitaireHT; }
    public void setPrixUnitaireHT(BigDecimal prixUnitaireHT) { this.prixUnitaireHT = prixUnitaireHT; }

    public BigDecimal getTauxTVA() { return tauxTVA; }
    public void setTauxTVA(BigDecimal tauxTVA) { this.tauxTVA = tauxTVA; }
}
