package org.gestion.devis.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DevisRequestTest {

    @Test
    void devisRequest() {
        assertEquals(1L, new DevisRequest(1L).clientId());
    }

    @Test
    void ligneDevisRequest() {
        LigneDevisRequest request = new LigneDevisRequest(3L, 2);

        assertEquals(3L, request.produitId());
        assertEquals(2, request.quantite());
    }
}
