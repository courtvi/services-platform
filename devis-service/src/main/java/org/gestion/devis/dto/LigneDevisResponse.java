package org.gestion.devis.dto;

import java.math.BigDecimal;

public record LigneDevisResponse(
        Long id,
        Long produitId,
        String name,
        Integer quantite,
        BigDecimal prixUnitaireHT,
        BigDecimal tauxTVA,
        BigDecimal totalLigneHT,
        BigDecimal totalLigneTTC
) {
}
