package pe.joedayz.microservicios.catalog.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Catalog API",
                version = "1.0.0",
                description = """
                        Microservicio Catalog (Modulo 3 — JoeDayz.pe).
                        Code-first con springdoc OpenAPI 3.1.
                        Todas las operaciones de negocio requieren header X-Tenant-ID.
                        """,
                contact = @Contact(name = "JoeDayz.pe", url = "https://joedayz.pe")
        ),
        servers = @Server(url = "http://localhost:8081", description = "Local")
)
public class OpenApiConfig {
}
