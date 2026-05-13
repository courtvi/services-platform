package org.gestion.client.model;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class ClientTest {

    @Test
    public void getter_setterClientIdTest() {
        Client client = new Client();
        client.setId(1L);
        assertEquals(1L, client.getId());
    }

    @Test
    public void getter_setterClientFirstNameTest() {
        Client client = new Client();
        client.setFirstName("Ben");
        assertEquals("Ben", client.getFirstName());
    }

    @Test
    public void getter_setterClientLastNameTest() {
        Client client = new Client();
        client.setLastName("WEBBER");
        assertEquals("WEBBER", client.getLastName());
    }

    @Test
    public void getter_setterClientReservationTest() {
        Client client = new Client();
        client.setReservation("05B");
        assertEquals("05B", client.getReservation());
    }
}
