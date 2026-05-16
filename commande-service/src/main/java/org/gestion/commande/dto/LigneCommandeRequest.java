package org.gestion.commande.dto;

import java.time.LocalDateTime;

public record LigneCommandeRequest(
        String article,
        Integer quantite,
        Double prixUnitaire,
        Double total
) {}