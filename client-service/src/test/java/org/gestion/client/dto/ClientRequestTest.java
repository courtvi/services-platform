package org.gestion.client.dto;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class ClientRequestTest {

    @Test
    public void getter_setterClientRequestFirstNameTest() {
        ClientRequest clientRequest = new ClientRequest();
        clientRequest.setFirstName("Ben");
        assertEquals("Ben", clientRequest.getFirstName());
    }

    @Test
    public void getter_setterClientRequestLastNameTest() {
        ClientRequest clientRequest = new ClientRequest();
        clientRequest.setLastName("WEBBER");
        assertEquals("WEBBER", clientRequest.getLastName());
    }

    @Test
    public void getter_setterClientRequestReservationTest() {
        ClientRequest clientRequest = new ClientRequest();
        clientRequest.setReservation("room1");
        assertEquals("room1", clientRequest.getReservation());
    }
}
