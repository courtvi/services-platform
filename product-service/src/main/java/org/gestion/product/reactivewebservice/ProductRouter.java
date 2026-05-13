package org.gestion.product.reactivewebservice;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;



@Configuration(proxyBeanMethods = false)
public class ProductRouter {

    @Bean
    public RouterFunction<ServerResponse> routes(ProductHandler handler) {

        return RouterFunctions.route()
                .GET("api/products", handler::getAllProducts)
                .POST("api/products", handler::createProduct)
                .build();
    }
}
