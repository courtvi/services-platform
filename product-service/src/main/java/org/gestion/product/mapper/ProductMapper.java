package org.gestion.product.mapper;

import org.gestion.product.dto.ProductRequest;
import org.gestion.product.dto.ProductResponse;
import org.gestion.product.model.Product;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    public ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice()
        );
    }

    public Product toEntity(ProductRequest request) {
        return new Product(
                request.name(),
                request.description(),
                request.price()
        );
    }

}
