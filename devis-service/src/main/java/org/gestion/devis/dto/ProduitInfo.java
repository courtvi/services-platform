package org.gestion.devis.dto;

import java.math.BigDecimal;

/** Produit tel que renvoyé par produit-service (GET /api/products/{id}). */
public record ProduitInfo(
        Long id,
        String name,
        String description,
        BigDecimal price,
        BigDecimal tauxTVA
) {
}
