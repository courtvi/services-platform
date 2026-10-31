package org.gestion.devis.model;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.gestion.client.model.Client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

@SpringBootTest
public class DevisTest {

    @Test
    public void g7DevisIdTest() {
        Devis devis = new Devis();
        devis.setId(1L);
        assertEquals(1L, devis.getId());
    }

    @Test
    public void g7DevisAddresseTest() {
        Devis devis = new Devis();
        devis.setAdresse("1 clos du chateau");
        assertEquals("1 clos du chateau", devis.getAdresse());
    }

    @Test
    public void g7DevisNomTest() {
        Devis devis = new Devis();
        devis.setNom("WEBBER");
        assertEquals("WEBBER", devis.getNom());
    }

    @Test
    public void g7DevisReservationTest() {
        Devis devis = new Devis();
        devis.setReference("05B");
        assertEquals("05B", devis.getReference());
    }

    @Test
    public void g7DevisClientTest() {

        Client client = new Client();
        client.setFirstName("Ben");
        client.setLastName("WEBER");
        client.setId(1L);

        Devis devis = new Devis();
        devis.setClient(client);

        assertSame(client, devis.getClient());
        assertEquals("Ben", devis.getClient().getFirstName());
        assertEquals("WEBER", devis.getClient().getLastName());
        assertEquals(1L, devis.getClient().getId());
    }


}
