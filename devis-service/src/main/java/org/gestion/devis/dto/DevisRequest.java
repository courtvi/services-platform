package org.gestion.devis.dto;

/** Création d'un devis : seul le client est fourni. Référence et date sont fixées par le serveur. */
public record DevisRequest(Long clientId) {
}
