package org.gestion.devis.model;

import org.springframework.data.annotation.Id;

import java.util.Date;
import org.gestion.client.model.Client;

public class Devis {

    @Id

    private Long id;
    private String reference;
    private String nom;
    private String raisonSociale;
    private String adresse;
    private Date date;
    private Client client;
    public Devis() {
    }

    public Devis(Long id, String reference, String nom, String raisonSociale,
                 String adresse, Date date, Client client) {
        this.id = id;
        this.reference = reference;
        this.nom = nom;
        this.raisonSociale = raisonSociale;
        this.adresse = adresse;
        this.date = date;
        this.client = client;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public void setRaisonSociale(String raisonSociale) {
        this.raisonSociale = raisonSociale;
    }

    public void setAdresse(String adresse) {
        this.adresse = adresse;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public String getReference() {
        return reference;
    }

    public String getNom() {
        return nom;
    }

    public String getRaisonSociale() {
        return raisonSociale;
    }

    public String getAdresse() {
        return adresse;
    }

    public Date getDate() {
        return date;
    }

    public void setClient(Client client) {
        this.client = client;
    }

    public Client getClient() {
        return client;
    }
}
