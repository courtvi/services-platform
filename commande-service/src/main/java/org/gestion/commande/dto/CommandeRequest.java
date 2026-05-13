package org.gestion.commande.dto;

import java.util.List;

public record CommandeRequest(
        String reference,
        List<LigneCommandeRequest> lignes
) {}