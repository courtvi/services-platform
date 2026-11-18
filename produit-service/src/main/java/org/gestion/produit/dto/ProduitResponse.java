package org.gestion.produit.dto;

import java.math.BigDecimal;

public record ProduitResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        BigDecimal tauxTVA
) {
}
