package org.gestion.produit.dto;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProduitRequestTest {

    @Test
    void recordExposeSesChamps() {
        ProduitRequest request = new ProduitRequest("baguette", "pas trop cuite",
                new BigDecimal("1.30"), new BigDecimal("5.50"));

        assertEquals("baguette", request.name());
        assertEquals("pas trop cuite", request.description());
        assertEquals(new BigDecimal("1.30"), request.price());
        assertEquals(new BigDecimal("5.50"), request.tauxTVA());
    }
}
