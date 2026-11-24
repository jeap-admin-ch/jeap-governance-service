package ch.admin.bit.jeap.governance.web.api;

import ch.admin.bit.jeap.governance.web.api.docs.DocsApiPaths;
import ch.admin.bit.jeap.governance.web.api.docs.SystemsController;
import ch.admin.bit.jeap.governance.web.api.management.ManagementController;
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
                contact = @Contact(name = "Margun", url = "https://github.com/jeap-admin-ch/jeap-governance-service")
        )
)
@Configuration
public class OpenApiConfig {

    @Bean
    GroupedOpenApi externalApi() {
        return GroupedOpenApi.builder()
                .group("Governance-Service-Management-API")
                .pathsToMatch("/api/**")
                .packagesToScan(ManagementController.class.getPackageName())
                .build();
    }

    @Bean
    GroupedOpenApi docsApi() {
        return GroupedOpenApi.builder()
                .group("Governance-Service-Docs-API")
                .pathsToMatch(DocsApiPaths.DOCS_API + "/**")
                .packagesToScan(SystemsController.class.getPackageName())
                .build();
    }
}
