package org.gestion.client.dto;

public class ClientResponse {

    private Long id;
    private String firstName;
    private String lastName;
    private String reservation;

    public ClientResponse() {

    }

    public ClientResponse(long id,String firstName, String lastName,String reservation) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.reservation = reservation;
    }

    public void setId(Long id) {
        this.id = id;
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

