package libsys.comp41720.apigateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;

@Configuration
public class GatewayRoutes {
    
    @Bean
    public RouterFunction<ServerResponse> bookServiceFallback() {
        return route("bookServiceFallback")
                .GET("/fallback/book-service", request -> ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body("Book Service is temporarily unavailable. Please try again later."))
                .build();
    }
    
    @Bean
    public RouterFunction<ServerResponse> borrowServiceFallback() {
        return route("borrowServiceFallback")
                .GET("/fallback/borrow-service", request -> ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body("Borrow Service is temporarily unavailable. Please try again later."))
                .build();
    }
}

