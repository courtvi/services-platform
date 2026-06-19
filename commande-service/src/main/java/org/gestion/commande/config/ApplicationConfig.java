package org.gestion.commande.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

@Configuration
@EnableR2dbcRepositories
@EnableConfigurationProperties(ApplicationConfig.RouteProperties.class)
@SuppressWarnings("java:S1118") // Spring nécessite un constructeur public pour @Configuration
public class ApplicationConfig {

    @ConfigurationProperties(prefix = "app.routes")
    public static class RouteProperties {
        private String basePath;
        private String pathId;

        public String getBasePath() { return basePath; }
        public void setBasePath(String basePath) { this.basePath = basePath; }
        public String getPathId() { return pathId; }
        public void setPathId(String pathId) { this.pathId = pathId; }
    }
}
