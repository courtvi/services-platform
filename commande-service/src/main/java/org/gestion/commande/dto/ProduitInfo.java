package org.gestion.commande.dto;

import java.math.BigDecimal;

/** Produit tel que renvoyé par produit-service (GET /api/products/{id}) : prix HT + taux de TVA. */
public record ProduitInfo(
        Long id,
        String name,
        String description,
        BigDecimal price,
        BigDecimal tauxTVA
) {}
