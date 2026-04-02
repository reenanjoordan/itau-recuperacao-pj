package br.com.itau.recuperacao.renegociacao.infrastructure.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração do OpenAPI/Swagger para documentação da API.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Renegociação Service API",
                version = "1.0.0",
                description = "API de Renegociação de Dívidas PJ - Itaú Unibanco",
                contact = @Contact(
                        name = "Equipe Recuperação PJ",
                        email = "recuperacao-pj@itau-unibanco.com.br"
                )
        ),
        servers = {
                @Server(url = "http://localhost:8082", description = "Ambiente local de desenvolvimento")
        }
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Token JWT obtido via API Gateway"
)
public class OpenApiConfig {
}
