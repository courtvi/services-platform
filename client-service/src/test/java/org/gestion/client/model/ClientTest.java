package org.gestion.client.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClientTest {

    @Test
    void constructeurRemplitTousLesChamps() {
        LocalDate naissance = LocalDate.of(1986, 10, 30);
        Client client = new Client(1L, "Ben", "Weber", "+352334568",
                "ben.weber@gmail.com", naissance, "Haller Luxembourg");

        assertEquals(1L, client.getId());
        assertEquals("Ben", client.getFirstName());
        assertEquals("Weber", client.getLastName());
        assertEquals("+352334568", client.getTelephone());
        assertEquals("ben.weber@gmail.com", client.getMail());
        assertEquals(naissance, client.getDateNaissance());
        assertEquals("Haller Luxembourg", client.getAdresse());
    }

    @Test
    void settersModifientLesChamps() {
        Client client = new Client();
        client.setId(2L);
        client.setFirstName("Anna");
        client.setLastName("Muller");
        client.setMail("anna@example.lu");

        assertEquals(2L, client.getId());
        assertEquals("Anna", client.getFirstName());
        assertEquals("Muller", client.getLastName());
        assertEquals("anna@example.lu", client.getMail());
    }
}
