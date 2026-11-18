package org.gestion.devis.model;

import org.gestion.devis.dto.ProduitInfo;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LigneDevisTest {

    private static final ProduitInfo PRODUIT =
            new ProduitInfo(3L, "Pain", "baguette", new BigDecimal("1.30"), new BigDecimal("5.50"));

    @Test
    void constructeur_copieLeProduit() {
        LigneDevis ligne = new LigneDevis(PRODUIT, 4);

        assertEquals(3L, ligne.getProduitId());
        assertEquals("Pain", ligne.getName());
        assertEquals(new BigDecimal("1.30"), ligne.getPrixUnitaireHT());
        assertEquals(new BigDecimal("5.50"), ligne.getTauxTVA());
    }

    @Test
    void totaux() {
        LigneDevis ligne = new LigneDevis(PRODUIT, 4);   // 4 x 1.30 = 5.20 ; TVA 5.5 % = 0.286 -> 0.29

        assertEquals(new BigDecimal("5.20"), ligne.getTotalLigneHT());
        assertEquals(new BigDecimal("0.29"), ligne.getMontantTVA());
        assertEquals(new BigDecimal("5.49"), ligne.getTotalLigneTTC());
    }

    @Test
    void refuseUnProduitAbsent() {
        assertThrows(IllegalArgumentException.class, () -> new LigneDevis(null, 1));
    }

    @Test
    void refuseUneQuantiteNulleOuNegative() {
        assertThrows(IllegalArgumentException.class, () -> new LigneDevis(PRODUIT, 0));
        assertThrows(IllegalArgumentException.class, () -> new LigneDevis(PRODUIT, -2));
        assertThrows(IllegalArgumentException.class, () -> new LigneDevis(PRODUIT, null));
    }
}
