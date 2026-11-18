package org.gestion.devis.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class DevisTest {

    @Test
    void constructeur() {
        LocalDateTime maintenant = LocalDateTime.of(2026, 11, 18, 22, 0);
        Devis devis = new Devis(7L, maintenant);

        assertNull(devis.getId());
        assertNull(devis.getReference(), "la référence est attribuée après le premier enregistrement");
        assertEquals(7L, devis.getClientId());
        assertEquals(maintenant, devis.getDateCreation());
    }

    @Test
    void reference() {
        Devis devis = new Devis();
        devis.setReference("DEV-2026-00042");

        assertEquals("DEV-2026-00042", devis.getReference());
    }
}
