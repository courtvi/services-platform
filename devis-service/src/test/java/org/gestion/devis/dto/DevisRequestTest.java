package org.gestion.devis.dto;


import org.gestion.client.model.Client;
import org.gestion.produit.model.Produit;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class DevisRequestTest {

    @Test
    public void G7DevisNomTest() {
        DevisRequest devisRequest = new DevisRequest();
        devisRequest.setNom("Ben");
        assertEquals("Ben", devisRequest.getNom());
    }

    @Test
    public void G7DevisAdresseTest() {
        DevisRequest devisRequest = new DevisRequest();
        devisRequest.setAdresse("1clos du chateau");
        assertEquals("1clos du chateau", devisRequest.getAdresse());
    }

    @Test
    public void G7DevisRequestRaisonSocialeTest() {
        DevisRequest devisRequest = new DevisRequest();
        devisRequest.setRaisonSociale("MyFirstCompagny");
        assertEquals("MyFirstCompagny", devisRequest.getRaisonSociale());
    }

    @Test
    public void G7DevisRequestClientTest() {
        DevisRequest devisRequest = new DevisRequest();
        Client client = new Client(1L,"Ben", "Weber");
        Produit produit = new Produit(1L, "monProduit","myDescription", "15.50");
        devisRequest.setClient(client);
        assertEquals(1L, devisRequest.getClient().getId());
        assertEquals("Ben", devisRequest.getClient().getFirstName());
        assertEquals("Weber", devisRequest.getClient().getLastName());
    }
}
