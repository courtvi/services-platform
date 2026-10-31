package org.gestion.produit.dto;


public record ProduitResponse(
        Long id,
        String name,
        String description,
        String price
) {}
