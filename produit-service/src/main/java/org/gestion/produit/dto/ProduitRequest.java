package org.gestion.produit.dto;

import java.math.BigDecimal;

/** Données envoyées pour créer ou modifier un produit (pas d'id : c'est le serveur qui le gère). */
public record ProduitRequest(
        String name,
        String description,
        BigDecimal price,
        BigDecimal tauxTVA
) {
}
