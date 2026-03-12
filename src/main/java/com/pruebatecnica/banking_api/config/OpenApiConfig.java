package com.pruebatecnica.banking_api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Banking API")
                        .description("API REST para gestión de cuentas bancarias y movimientos financieros.")
                        .version("1.0.0"));
    }
}
