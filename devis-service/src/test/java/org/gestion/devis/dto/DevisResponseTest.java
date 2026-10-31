package org.gestion.devis.dto;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.Date;
import org.gestion.client.model.Client;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class DevisResponseTest {

    @Test
    public void G7DevisResponseNomTest() {
        DevisResponse devisResponse = new DevisResponse();
        devisResponse.setNom("Ben");
        assertEquals("Ben", devisResponse.getNom());
    }

    @Test
    public void G7DevisResponseAdresseTest() {
        DevisResponse devisResponse = new DevisResponse();
        devisResponse.setAdresse("1clos du chateau");
        assertEquals("1clos du chateau", devisResponse.getAdresse());
    }

    @Test
    public void G7DevisResponseDateTest() {
        Date expected = Date.from(Instant.parse("2026-10-30T10:00:00Z"));
        DevisResponse devisResponse = new DevisResponse();
        devisResponse.setDate(expected);
        assertEquals(expected, devisResponse.getDate());
    }

    @Test
    public void G7DevisResponseClientTest() {

        Client client = new Client();
        client.setId(1L);
        client.setFirstName("Ben");
        client.setLastName("WEBER");

        DevisResponse devisResponse = new DevisResponse();
        devisResponse.setClient(client);
        assertEquals(client, devisResponse.getClient());
    }
}
