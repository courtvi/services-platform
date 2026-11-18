package org.gestion.devis.service;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.gestion.devis.dto.ProduitInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.SSLException;

/**
 * Lecture d'un produit dans produit-service (GET /api/products/{id}).
 * Si app.produit-service.ssl-bundle est renseigné (profil mtls), l'appel se fait en HTTPS
 * en présentant le certificat de ce service : produit-service exige un certificat client.
 */
@Component
public class ProduitClient {

    private final WebClient webClient;

    public ProduitClient(@Value("${app.produit-service.url}") String baseUrl,
                         @Value("${app.produit-service.ssl-bundle:}") String sslBundleName,
                         SslBundles sslBundles) {
        WebClient.Builder builder = WebClient.builder().baseUrl(baseUrl);
        if (!sslBundleName.isBlank()) {
            SslContext sslContext = sslContext(sslBundles.getBundle(sslBundleName));
            HttpClient httpClient = HttpClient.create().secure(spec -> spec.sslContext(sslContext));
            builder.clientConnector(new ReactorClientHttpConnector(httpClient));
        }
        this.webClient = builder.build();
    }

    private static SslContext sslContext(SslBundle bundle) {
        try {
            return SslContextBuilder.forClient()
                    .keyManager(bundle.getManagers().getKeyManagerFactory())      // notre certificat
                    .trustManager(bundle.getManagers().getTrustManagerFactory())  // la CA commune
                    .build();
        } catch (SSLException e) {
            throw new IllegalStateException("Configuration mTLS invalide pour produit-service", e);
        }
    }

    public Mono<ProduitInfo> getProduit(Long produitId) {
        return webClient.get()
                .uri("/api/products/{id}", produitId)
                .retrieve()
                .onStatus(status -> status.value() == HttpStatus.NOT_FOUND.value(),
                        response -> Mono.error(new IllegalArgumentException("Produit inconnu : " + produitId)))
                .bodyToMono(ProduitInfo.class);
    }
}
