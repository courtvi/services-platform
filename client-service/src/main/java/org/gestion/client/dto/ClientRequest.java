package org.gestion.client.dto;

public class ClientRequest {

    private String reservation;
    private String firstName;
    private String lastName;

    public ClientRequest() {

    }

    public ClientRequest(String reservation, String firstName, String lastName) {
    this.reservation = reservation;
    this.firstName = firstName;
    this.lastName = lastName;
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