package org.gestion.produit.mapper;

import org.gestion.produit.dto.ProduitRequest;
import org.gestion.produit.dto.ProduitResponse;
import org.gestion.produit.model.Produit;
import org.springframework.stereotype.Component;

@Component
public class ProduitMapper {

    public ProduitResponse toResponse(Produit product) {
        return new ProduitResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice()
        );
    }

    public Produit toEntity(ProduitRequest request) {
        return new Produit(
                request.id(),
                request.name(),
                request.description(),
                request.price()
        );
    }

}
