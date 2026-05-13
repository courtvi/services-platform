package org.gestion.client.dto;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class ClientResponseTest {

    @Test
    public void getter_setterClientResponseFirstNameTest() {
        ClientResponse clientResponse = new ClientResponse();
        clientResponse.setFirstName("Ben");
        assertEquals("Ben", clientResponse.getFirstName());
    }

    @Test
    public void getter_setterClientResponseLastNameTest() {
        ClientResponse clientResponse = new ClientResponse();
        clientResponse.setLastName("WEBBER");
        assertEquals("WEBBER", clientResponse.getLastName());
    }

    @Test
    public void getter_setterClientResponseReservationTest() {
        ClientResponse clientResponse = new ClientResponse();
        clientResponse.setReservation("room1");
        assertEquals("room1", clientResponse.getReservation());
    }
}
