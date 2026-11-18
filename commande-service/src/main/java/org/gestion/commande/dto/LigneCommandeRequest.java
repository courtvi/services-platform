package org.gestion.commande.dto;

/**
 * Ligne demandée par le front : uniquement le produit et la quantité.
 * Le nom, le prix et le total sont lus et calculés côté serveur à partir de produit-service.
 */
public record LigneCommandeRequest(
        Long produitId,
        Integer quantite
) {}
