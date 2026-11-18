package org.gestion.produit.model;

import org.springframework.data.annotation.Id;

import java.math.BigDecimal;

public class Produit {

    @Id
    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private BigDecimal tauxTVA;

    public Produit() {}

    public Produit(Long id, String name, String description, BigDecimal price, BigDecimal tauxTVA) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.tauxTVA = tauxTVA;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public BigDecimal getTauxTVA() { return tauxTVA; }
    public void setTauxTVA(BigDecimal tauxTVA) { this.tauxTVA = tauxTVA; }

    @Override
    public String toString() {
        return "Produit [id=" + id + ", name=" + name + ", description=" + description
                + ", price=" + price + ", tauxTVA=" + tauxTVA + "]";
    }
}
