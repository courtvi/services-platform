package org.gestion.produit.mapper;

import org.gestion.produit.dto.ProduitBoutiqueResponse;
import org.gestion.produit.dto.ProduitRequest;
import org.gestion.produit.dto.ProduitResponse;
import org.gestion.produit.model.FicheBoutique;
import org.gestion.produit.model.Produit;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class ProduitMapper {

    public ProduitResponse toResponse(Produit produit) {
        return new ProduitResponse(
                produit.getId(),
                produit.getName(),
                produit.getDescription(),
                produit.getPrice(),
                produit.getTauxTVA());
    }

    /** Nouveau produit : l'id reste null, la base le génère. */
    public Produit toEntity(ProduitRequest request) {
        return new Produit(null, request.name(), request.description(), request.price(), request.tauxTVA());
    }

    /** Recopie la requête sur un produit existant (l'id n'est pas touché). */
    public void updateEntity(ProduitRequest request, Produit produit) {
        produit.setName(request.name());
        produit.setDescription(request.description());
        produit.setPrice(request.price());
        produit.setTauxTVA(request.tauxTVA());
    }

    /** Composition produit + fiche boutique, avec le prix TTC calculé. */
    public ProduitBoutiqueResponse toProduitBoutique(Produit produit, FicheBoutique fiche) {
        return new ProduitBoutiqueResponse(
                produit.getId(),
                fiche.getBoutiqueId(),
                produit.getName(),
                fiche.getOrigine(),
                prixTTC(produit),
                produit.getPrice(),
                produit.getTauxTVA(),
                fiche.getUnite(),
                fiche.getCategorie(),
                fiche.getTagCle(),
                fiche.getImage(),
                Boolean.TRUE.equals(fiche.getRupture()),
                fiche.getAnnee());
    }

    /** Prix TTC = HT x (1 + TVA/100), arrondi au centime (même calcul que commande-service). */
    public static BigDecimal prixTTC(Produit produit) {
        return produit.getPrice()
                .multiply(BigDecimal.ONE.add(produit.getTauxTVA().movePointLeft(2)))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
