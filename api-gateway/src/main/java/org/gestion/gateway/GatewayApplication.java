package org.gestion.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }

    /**
     * Une seule route : product-service, découvert via Eureka.
     * - pas de rewritePath : product-service expose déjà /api/products
     * - retry(2) : sur GET, rejoue en cas d'erreur réseau / 5xx
     * - https choisi automatiquement : l'instance Eureka est déclarée "secure"
     */
    @Bean
    public RouteLocator gatewayRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("product-service", r -> r.path("/api/products", "/api/products/**")
                        .filters(f -> f.retry(2))
                        .uri("lb://product-service"))
                .build();
    }
}
