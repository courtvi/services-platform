package org.gestion.devis.dto;

/** Envoi de la version en cours. Les deux champs sont facultatifs (validité par défaut : 30 jours). */
public record EnvoiRequest(Integer joursValidite, String commentaire) {
}
