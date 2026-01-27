package ch.admin.bit.jeap.governance.web.api;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@OpenAPIDefinition(
        info = @Info(
                title = "Governance service API",
                description = "Governance service API",
                contact = @Contact(
                        name = "Margun",
                        url = "https://confluence.eap.bit.admin.ch/display/BLUE"
                )
        )
)
@Configuration
public class OpenApiConfig {

    @Bean
    GroupedOpenApi externalApi() {
        return GroupedOpenApi.builder()
                .group("Governance-Service-API")
                .pathsToMatch("/api/**")
                .packagesToScan(this.getClass().getPackageName())
                .build();
    }
}
