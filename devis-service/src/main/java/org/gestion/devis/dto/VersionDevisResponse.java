package org.gestion.devis.dto;

import org.gestion.devis.model.StatutVersion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record VersionDevisResponse(
        Long id,
        Integer numeroVersion,
        StatutVersion statut,
        LocalDate dateEmission,
        LocalDate dateValidite,
        String commentaire,
        BigDecimal totalHT,
        BigDecimal totalTVA,
        BigDecimal totalTTC,
        List<LigneDevisResponse> lignes
) {
}
