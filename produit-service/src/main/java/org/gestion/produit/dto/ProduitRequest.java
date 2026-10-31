package org.gestion.produit.dto;

public record ProduitRequest(
        Long id,
        String name,
        String description,
        String price
) {

}
