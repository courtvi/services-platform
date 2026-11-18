package org.gestion.produit.mapper;

import org.gestion.produit.dto.ProduitRequest;
import org.gestion.produit.dto.ProduitResponse;
import org.gestion.produit.model.Produit;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ProduitMapperTest {

    private final ProduitMapper mapper = new ProduitMapper();
    private final ProduitRequest request = new ProduitRequest("miel", "miel de fleurs",
            new BigDecimal("8.50"), new BigDecimal("5.50"));

    @Test
    void toEntity_laisseLIdNull() {
        Produit produit = mapper.toEntity(request);

        assertNull(produit.getId());
        assertEquals("miel", produit.getName());
        assertEquals(new BigDecimal("8.50"), produit.getPrice());
        assertEquals(new BigDecimal("5.50"), produit.getTauxTVA());
    }

    @Test
    void toResponse_copieTousLesChamps() {
        Produit produit = new Produit(7L, "vin", "rouge", new BigDecimal("12.00"), new BigDecimal("20.00"));

        ProduitResponse response = mapper.toResponse(produit);

        assertEquals(7L, response.id());
        assertEquals("vin", response.name());
        assertEquals("rouge", response.description());
        assertEquals(new BigDecimal("12.00"), response.price());
        assertEquals(new BigDecimal("20.00"), response.tauxTVA());
    }

    @Test
    void updateEntity_neTouchePasALId() {
        Produit existant = new Produit(7L, "ancien", "ancienne desc", BigDecimal.ONE, BigDecimal.TEN);

        mapper.updateEntity(request, existant);

        assertEquals(7L, existant.getId());
        assertEquals("miel", existant.getName());
        assertEquals(new BigDecimal("8.50"), existant.getPrice());
    }

    @Test
    void prixTTC_retombeSurLePrixAfficheParLeFront() {
        assertEquals(new BigDecimal("17.00"), ProduitMapper.prixTTC(
                new Produit(1L, "miel_printemps", null, new BigDecimal("16.11"), new BigDecimal("5.50"))));
        assertEquals(new BigDecimal("8.00"), ProduitMapper.prixTTC(
                new Produit(103L, "cremant", null, new BigDecimal("6.67"), new BigDecimal("20.00"))));
    }

    @Test
    void toProduitBoutique_ruptureNullDevientFalse() {
        Produit produit = new Produit(2L, "miel_ete", null, new BigDecimal("9.00"), new BigDecimal("5.50"));
        org.gestion.produit.model.FicheBoutique fiche = new org.gestion.produit.model.FicheBoutique();
        fiche.setBoutiqueId("miel");

        assertEquals(false, mapper.toProduitBoutique(produit, fiche).rupture());
        assertEquals(new BigDecimal("9.50"), mapper.toProduitBoutique(produit, fiche).prix());
    }
}
