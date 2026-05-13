package org.gestion.product.dto;

import java.math.BigDecimal;
import org.springframework.data.annotation.Id;

public record ProductResponse(
        Integer id,
        String name,
        String description,
        String price
) {}
