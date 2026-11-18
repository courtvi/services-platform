package org.gestion.devis.model;

import org.gestion.devis.dto.ProduitInfo;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VersionDevisTest {

    private static final ProduitInfo EMPLACEMENT =
            new ProduitInfo(1L, "Emplacement", "nuit", new BigDecimal("10.00"), new BigDecimal("20.00"));
    private static final ProduitInfo MIEL =
            new ProduitInfo(2L, "Miel", "pot 500 g", new BigDecimal("8.50"), new BigDecimal("5.50"));

    private static void assertMontant(String attendu, BigDecimal reel) {
        assertEquals(0, new BigDecimal(attendu).compareTo(reel), "attendu " + attendu + " mais " + reel);
    }

    private static VersionDevis brouillonAvecDeuxLignes() {
        VersionDevis v = VersionDevis.premiere(5L);
        v.ajouterLigne(new LigneDevis(EMPLACEMENT, 3));   // HT 30.00, TVA 6.00
        v.ajouterLigne(new LigneDevis(MIEL, 2));          // HT 17.00, TVA 0.935 -> 0.94
        return v;
    }

    @Test
    void premiere_estUnBrouillonNumero1() {
        VersionDevis v = VersionDevis.premiere(5L);

        assertEquals(5L, v.getDevisId());
        assertEquals(1, v.getNumeroVersion());
        assertEquals(StatutVersion.BROUILLON, v.getStatut());
    }

    @Test
    void calculerTotaux_additionneLesLignes() {
        VersionDevis.Totaux totaux = brouillonAvecDeuxLignes().calculerTotaux();

        assertMontant("47.00", totaux.ht());
        assertMontant("6.94", totaux.tva());
        assertMontant("53.94", totaux.ttc());
    }

    @Test
    void envoyer_figeDatesEtTotaux() {
        VersionDevis v = brouillonAvecDeuxLignes();

        v.envoyer(30, "Tarif haute saison");

        assertEquals(StatutVersion.ENVOYEE, v.getStatut());
        assertEquals(LocalDate.now(), v.getDateEmission());
        assertEquals(LocalDate.now().plusDays(30), v.getDateValidite());
        assertEquals("Tarif haute saison", v.getCommentaire());
        assertMontant("47.00", v.getTotalHT());
        assertMontant("6.94", v.getTotalTVA());
        assertMontant("53.94", v.getTotalTTC());
    }

    @Test
    void envoyer_refuseUneVersionSansLigne() {
        VersionDevis v = VersionDevis.premiere(5L);

        assertThrows(IllegalStateException.class, () -> v.envoyer(30, null));
    }

    @Test
    void versionEnvoyee_nEstPlusModifiable() {
        VersionDevis v = brouillonAvecDeuxLignes();
        v.envoyer(30, null);

        assertThrows(IllegalStateException.class, () -> v.ajouterLigne(new LigneDevis(MIEL, 1)));
        assertThrows(IllegalStateException.class, () -> v.envoyer(30, null));
    }

    @Test
    void suivante_copieLesLignesEtRemplaceLaVersionEnvoyee() {
        VersionDevis v1 = brouillonAvecDeuxLignes();
        v1.envoyer(30, "v1");

        VersionDevis v2 = VersionDevis.suivante(v1);

        assertEquals(StatutVersion.REMPLACEE, v1.getStatut());
        assertEquals(StatutVersion.BROUILLON, v2.getStatut());
        assertEquals(2, v2.getNumeroVersion());
        assertEquals(5L, v2.getDevisId());
        assertEquals(2, v2.getLignes().size());
        assertNull(v2.getLignes().get(0).getId(), "les lignes copiées sont de nouvelles lignes");
        assertNull(v2.getTotalHT(), "les totaux ne sont figés qu'à l'envoi");
    }

    @Test
    void suivante_refuseSiUnBrouillonExisteDeja() {
        VersionDevis brouillon = VersionDevis.premiere(5L);

        assertThrows(IllegalStateException.class, () -> VersionDevis.suivante(brouillon));
    }

    @Test
    void suivante_refuseSiDevisAccepte() {
        VersionDevis v = brouillonAvecDeuxLignes();
        v.envoyer(30, null);
        v.accepter();

        assertThrows(IllegalStateException.class, () -> VersionDevis.suivante(v));
    }

    @Test
    void accepter_exigeUneVersionEnvoyee() {
        VersionDevis brouillon = brouillonAvecDeuxLignes();

        assertThrows(IllegalStateException.class, brouillon::accepter);
    }

    @Test
    void refuser_passeEnRefusee() {
        VersionDevis v = brouillonAvecDeuxLignes();
        v.envoyer(30, null);

        v.refuser();

        assertEquals(StatutVersion.REFUSEE, v.getStatut());
    }
}
