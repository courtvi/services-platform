package org.gestion.commande.dto;

import java.time.LocalDateTime;

public record CommandeResponse(
        Long id,
        String userId,
        String numeroClient,
        String reference,
        String statut,
        LocalDateTime dateCommande,
        LocalDateTime dateLivraison,
        Double total
) {}