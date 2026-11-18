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
     * Routes vers les microservices, découverts via Eureka.
     * - pas de rewritePath : chaque service expose déjà /api/...
     * - en profil mtls, les instances sont déclarées "secure" dans Eureka : la gateway les appelle
     *   en https en présentant son certificat (spring.cloud.gateway...httpclient.ssl.ssl-bundle)
     * - l'en-tête Authorization (JWT) est transmis tel quel : chaque service vérifie les rôles
     */
    @Bean
    public RouteLocator gatewayRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("product-service", r -> r.path("/api/products", "/api/products/**")
                        .filters(f -> f.retry(2))        // GET uniquement : rejoue en cas d'erreur réseau / 5xx
                        .uri("lb://product-service"))
                .route("commande-service", r -> r.path("/api/commandes", "/api/commandes/**")
                        .uri("lb://commande-service"))
                .route("client-service", r -> r.path("/api/clients", "/api/clients/**")
                        .uri("lb://client-service"))
                .route("devis-service", r -> r.path("/api/devis", "/api/devis/**")
                        .uri("lb://devis-service"))
                .build();
    }
}
