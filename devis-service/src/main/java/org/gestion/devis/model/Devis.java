package org.gestion.devis.model;

import org.springframework.data.annotation.Id;

import java.time.LocalDateTime;

/**
 * En-tête du devis : stable d'une version à l'autre.
 * Le contenu (lignes, statut, dates, totaux) est dans {@link VersionDevis}.
 * Le client est référencé par son id : il vit dans client-service.
 */
public class Devis {

    @Id
    private Long id;
    private String reference;
    private Long clientId;
    private LocalDateTime dateCreation;

    public Devis() {
    }

    public Devis(Long clientId, LocalDateTime dateCreation) {
        this.clientId = clientId;
        this.dateCreation = dateCreation;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }

    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }

    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }
}
