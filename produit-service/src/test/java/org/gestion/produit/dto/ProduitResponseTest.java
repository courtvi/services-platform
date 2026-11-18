package org.gestion.produit.dto;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProduitResponseTest {

    @Test
    void recordExposeSesChamps() {
        ProduitResponse response = new ProduitResponse(3L, "baguette", "MyDescription",
                new BigDecimal("99.99"), new BigDecimal("20.00"));

        assertEquals(3L, response.id());
        assertEquals("baguette", response.name());
        assertEquals("MyDescription", response.description());
        assertEquals(new BigDecimal("99.99"), response.price());
        assertEquals(new BigDecimal("20.00"), response.tauxTVA());
    }
}
