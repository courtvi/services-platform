package org.gestion.produit.dto;

import java.math.BigDecimal;

/**
 * Produit tel qu'affiché dans une boutique : composition du produit (prix, TVA)
 * et de sa fiche boutique. Les noms de champs reprennent ceux du modèle Produit du front.
 */
public record ProduitBoutiqueResponse(
        Long id,               // id du produit (produit-service)
        String boutiqueId,
        String cle,            // clé i18n = name du produit
        String origine,
        BigDecimal prix,       // prix TTC = HT x (1 + TVA)
        BigDecimal prixHT,
        BigDecimal tauxTVA,
        String unite,
        String categorie,
        String tagCle,
        String image,
        boolean rupture,
        String annee
) {
}
