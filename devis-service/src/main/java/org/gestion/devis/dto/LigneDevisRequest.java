package org.gestion.devis.dto;

/** Ajout d'une ligne : le prix, le nom et la TVA sont lus dans produit-service, jamais envoyés par l'appelant. */
public record LigneDevisRequest(Long produitId, Integer quantite) {
}
