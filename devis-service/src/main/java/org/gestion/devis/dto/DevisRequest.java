package org.gestion.devis.dto;

import org.gestion.client.model.Client;

import java.util.Date;

public class DevisRequest {

    private String reference;
    private String nom;
    private String raisonSociale;
    private String adresse;
    private Date date;
    private Client client;

    public DevisRequest() {
    }

    public DevisRequest(String reference, String nom, String adresse, String raisonSociale, Date date, Client client) {
    this.reference = reference;
    this.adresse = adresse;
    this.nom = nom;
    this.raisonSociale = raisonSociale;
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

    public void setClient(Client client) { this.client = client; }

    public String getReference() {
        return reference;
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

    public String getNom() {
        return nom;
    }

    public Client getClient() { return client;}
}