package org.gestion.client.router;

import org.gestion.client.handler.ClientHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.path;

@Configuration
public class ClientRouter {

    @Bean
    public RouterFunction<ServerResponse> clientRoutes(ClientHandler handler) {
        return RouterFunctions.route()
                .nest(path("/api/clients"), builder -> builder
                        .GET("", handler::getClients)
                        .POST("", handler::createClient)
                        .GET("/{id}", handler::getClientById)
                        .PUT("/{id}", handler::updateClient)
                        .DELETE("/{id}", handler::deleteClient))
                .build();
    }
}
