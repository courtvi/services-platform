package org.gestion.client.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClientRequestTest {

    @Test
    void constructeurEtGetters() {
        LocalDate naissance = LocalDate.of(1990, 1, 15);
        ClientRequest request = new ClientRequest("Ben", "Webber", "+352111",
                "ben@example.lu", naissance, "Haller");

        assertEquals("Ben", request.getFirstName());
        assertEquals("Webber", request.getLastName());
        assertEquals("+352111", request.getTelephone());
        assertEquals("ben@example.lu", request.getMail());
        assertEquals(naissance, request.getDateNaissance());
        assertEquals("Haller", request.getAdresse());
    }
}
