package org.gestion.commande.dto;

public record LigneCommandeRequest(
        String article,
        Integer quantite
) {}