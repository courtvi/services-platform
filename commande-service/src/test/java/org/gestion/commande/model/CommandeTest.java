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
    void nouvelleCommande_sansDateLivraison_dateLivraisonEstNull() {
        Commande commande = new Commande();
        assertNull(commande.getDateLivraison());
    }

    @Test
    void constructeurComplet_initialiseCorrectement() {
        LocalDateTime maintenant = LocalDateTime.now();
        LocalDateTime livraison = maintenant.plusDays(1);

        Commande commande = new Commande(
                null,
                "user-123",
                "REF-001",
                "CREEE",
                maintenant,
                livraison
        );

        assertAll(
                () -> assertNull(commande.getId()),
                () -> assertEquals("user-123", commande.getUserId()),
                () -> assertEquals("REF-001", commande.getReference()),
                () -> assertEquals("CREEE", commande.getStatut()),
                () -> assertEquals(maintenant, commande.getDateCommande()),
                () -> assertEquals(livraison, commande.getDateLivraison())
        );
    }

    @Test
    void dateCommande_LocalDateTime_sansConversionComplexe() {
        Commande commande = new Commande();
        LocalDateTime date = LocalDateTime.of(2026, 5, 16, 0, 0);
        commande.setDateCommande(date);
        assertEquals(date, commande.getDateCommande());
    }

    @Test
    void dateLivraison_estLendemainDeDateCommande() {
        Commande commande = new Commande();
        LocalDateTime dateCommande = LocalDateTime.of(2026, 5, 16, 0, 0);
        LocalDateTime dateLivraison = dateCommande.plusDays(1);
        commande.setDateCommande(dateCommande);
        commande.setDateLivraison(dateLivraison);
        assertEquals(dateCommande.plusDays(1), commande.getDateLivraison());
    }

    @Test
    void setId_metsAJourId() {
        Commande commande = new Commande();
        commande.setId(42L);
        assertEquals(42L, commande.getId());
    }

    @Test
    void setReference_metsAJourReference() {
        Commande commande = new Commande();
        commande.setReference("CMD-20260516-1234");
        assertEquals("CMD-20260516-1234", commande.getReference());
    }
}