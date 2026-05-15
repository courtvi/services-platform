package org.gestion.product.dto;


public record ProductResponse(
        Integer id,
        String name,
        String description,
        String price
) {}
