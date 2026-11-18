package org.gestion.client.dto;

import java.time.LocalDate;

public class ClientResponse {

    private Long id;
    private String firstName;
    private String lastName;
    private String telephone;
    private String mail;
    private LocalDate dateNaissance;
    private String adresse;

    public ClientResponse() {
    }

    public ClientResponse(Long id, String firstName, String lastName, String telephone,
                          String mail, LocalDate dateNaissance, String adresse) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.telephone = telephone;
        this.mail = mail;
        this.dateNaissance = dateNaissance;
        this.adresse = adresse;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public String getMail() { return mail; }
    public void setMail(String mail) { this.mail = mail; }

    public LocalDate getDateNaissance() { return dateNaissance; }
    public void setDateNaissance(LocalDate dateNaissance) { this.dateNaissance = dateNaissance; }

    public String getAdresse() { return adresse; }
    public void setAdresse(String adresse) { this.adresse = adresse; }
}
