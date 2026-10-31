package org.gestion.devis.dto;

import org.gestion.client.model.Client;
import org.gestion.devis.model.DetailsDevis;

import java.util.Date;

public class DevisResponse {

    private Long id;
    private String reference;
    private String nom;
    private String adresse;
    private Date date;
    private String raisonSociale;
    private Client client;
    private DetailsDevis detailsDevis;

    public DevisResponse() {

    }

    public DevisResponse(long id,String reference, String adresse, String nom, String raisonSociale, Date date, Client client, DetailsDevis detailsDevis) {
        this.id = id;
        this.reference = reference;
        this.nom = nom;
        this.raisonSociale = raisonSociale;
        this.adresse = adresse;
        this.date = date;
        this.client = client;
        this.detailsDevis = detailsDevis;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public void setAdresse(String adresse) { this.adresse = adresse; }

    public void setRaisonSociale(String raisonSociale) {
        this.raisonSociale = raisonSociale;
    }

    public void setDate(Date date) { this.date = date;}

    public void setClient(Client client) { this.client = client;}

    public void setDetailsDevis(DetailsDevis detailsDevis) { this.detailsDevis = detailsDevis;}

    public Long getId() {
        return id;
    }

    public String getRaisonSociale() {
        return raisonSociale;
    }

    public String getNom() {
        return nom;
    }

    public String getAdresse() { return adresse; }

    public Date getDate() { return date; }

    public Client getClient() { return client;
    }

    public DetailsDevis getDetailsDevis() { return detailsDevis;}
}

