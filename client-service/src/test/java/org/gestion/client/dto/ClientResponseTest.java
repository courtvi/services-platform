package org.gestion.client.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClientResponseTest {

    @Test
    void constructeurEtGetters() {
        LocalDate naissance = LocalDate.of(1990, 1, 15);
        ClientResponse response = new ClientResponse(5L, "Ben", "Webber", "+352111",
                "ben@example.lu", naissance, "Haller");

        assertEquals(5L, response.getId());
        assertEquals("Ben", response.getFirstName());
        assertEquals("Webber", response.getLastName());
        assertEquals("+352111", response.getTelephone());
        assertEquals("ben@example.lu", response.getMail());
        assertEquals(naissance, response.getDateNaissance());
        assertEquals("Haller", response.getAdresse());
    }
}
