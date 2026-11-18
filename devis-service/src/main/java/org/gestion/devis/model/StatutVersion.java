package org.gestion.devis.model;

public enum StatutVersion {
    BROUILLON,   // en cours de rédaction, modifiable
    ENVOYEE,     // envoyée au client, verrouillée
    ACCEPTEE,    // acceptée par le client
    REFUSEE,     // refusée par le client
    EXPIREE,     // date de validité dépassée
    REMPLACEE    // remplacée par une version plus récente
}
