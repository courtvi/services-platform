package org.gestion.commande.dto;

// ✅ Record Java 21 — immutable, concis, zéro boilerplate
public record CommandeRequest(

        String reference    // seul champ que le client fournit

        // ❌ pas d'id      — généré par la base
        // ❌ pas de statut  — imposé CREEE côté serveur
        // ❌ pas de date    — imposée LocalDateTime.now() côté serveur
        // ❌ pas de userId  — extrait du JWT côté serveur
) {}