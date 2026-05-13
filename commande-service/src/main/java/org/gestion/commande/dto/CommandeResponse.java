package org.gestion.commande.dto;

import java.time.LocalDateTime;

public record CommandeResponse(
        Long id,
        String userId,
        String reference,
        String statut,
        LocalDateTime dateCommande
) {}