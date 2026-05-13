package org.gestion.client.model;

import org.springframework.data.annotation.Id;

public class Client {

    @Id

    private Long id;
    private String lastName;
    private String firstName;
    private String reservation;

    public Client() {
    }

    public Client(long id, String firstName, String lastName, String reservation) {
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setReservation(String reservation) {
        this.reservation = reservation;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getReservation() {
        return reservation;
    }
}
