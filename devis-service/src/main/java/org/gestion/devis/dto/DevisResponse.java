package org.gestion.devis.dto;

import java.time.LocalDateTime;
import java.util.List;

public record DevisResponse(
        Long id,
        String reference,
        Long clientId,
        LocalDateTime dateCreation,
        List<VersionDevisResponse> versions
) {
}
