package org.gestion.commande.dto;

import java.time.LocalDateTime;

// ✅ Record — ce que le serveur retourne au client
public record CommandeResponse(
        Long id,
        String userId,
        String reference,
        String statut,
        LocalDateTime dateCommande
) {}