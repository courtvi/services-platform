package org.gestion.product.dto;

public record ProductRequest(
        String name,
        String description,
        String price
) {}
