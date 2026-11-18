package org.gestion.devis.dto;

import org.gestion.devis.model.StatutVersion;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DevisResponseTest {

    @Test
    void devisResponseContientSesVersions() {
        VersionDevisResponse v1 = new VersionDevisResponse(10L, 1, StatutVersion.BROUILLON, null, null, null,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, List.of());
        LocalDateTime date = LocalDateTime.of(2026, 11, 18, 22, 0);

        DevisResponse response = new DevisResponse(1L, "DEV-2026-00001", 7L, date, List.of(v1));

        assertEquals("DEV-2026-00001", response.reference());
        assertEquals(7L, response.clientId());
        assertEquals(1, response.versions().size());
        assertEquals(StatutVersion.BROUILLON, response.versions().get(0).statut());
    }
}
