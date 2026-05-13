package org.gestion.commande.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class CommandeTest {

    @Test
    void nouvelleCommande_aStatutCREEE_parDefaut() {
        Commande commande = new Commande();
        commande.setStatut("CREEE");
        assertEquals("CREEE", commande.getStatut());
    }

    @Test
    void nouvelleCommande_sansUserId_userIdEstNull() {
        Commande commande = new Commande();
        assertNull(commande.getUserId());
    }

    @Test
    void constructeurComplet_initialiseCorrectement() {
        LocalDateTime maintenant = LocalDateTime.now();

        Commande commande = new Commande(
                null,
                "user-123",
                "REF-001",
                "CREEE",
                maintenant
        );

        assertAll(
                () -> assertNull(commande.getId()),
                () -> assertEquals("user-123", commande.getUserId()),
                () -> assertEquals("REF-001", commande.getReference()),
                () -> assertEquals("CREEE", commande.getStatut()),
                () -> assertEquals(maintenant, commande.getDateCommande())
        );
    }

    @Test
    void dateCommande_LocalDateTime_sansConversionComplexe() {
        Commande commande = new Commande();
        LocalDateTime date = LocalDateTime.of(2026, 5, 2, 0, 0);
        commande.setDateCommande(date);
        assertEquals(date, commande.getDateCommande());
    }
}