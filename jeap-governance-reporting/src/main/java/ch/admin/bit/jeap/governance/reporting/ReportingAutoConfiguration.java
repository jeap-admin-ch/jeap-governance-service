package ch.admin.bit.jeap.governance.reporting;

import org.sahli.asciidoc.confluence.publisher.client.http.ConfluenceClient;
import org.sahli.asciidoc.confluence.publisher.client.http.ConfluenceRestV1Client;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.templatemode.TemplateMode;

import java.nio.charset.StandardCharsets;

@AutoConfiguration
@PropertySource("classpath:reportingDefaultProperties.properties")
@ComponentScan
@ConditionalOnProperty(name = "jeap.governance.reporting.enabled", havingValue = "true", matchIfMissing = true)
@SuppressWarnings("java:S1075")
public class ReportingAutoConfiguration {

    private static final String CONFLUENCE_TEMPLATE_PATH = "/templates/confluence/";

    @Bean
    ConfluenceClient confluenceClient(ReportingProperties properties) {
        return new ConfluenceRestV1Client(properties.getConfluenceUrl(),
                false,
                false,
                10.0,
                null,
                properties.getConfluenceUsername(),
                properties.getConfluencePassword());
    }

    @Bean
    SpringResourceTemplateResolver templateResolver(ApplicationContext applicationContext) {
        SpringResourceTemplateResolver templateResolver = new SpringResourceTemplateResolver();
        templateResolver.setApplicationContext(applicationContext);
        templateResolver.setPrefix("classpath:" + CONFLUENCE_TEMPLATE_PATH);
        templateResolver.setSuffix(".html");
        templateResolver.setCharacterEncoding(StandardCharsets.UTF_8.displayName());
        templateResolver.setTemplateMode(TemplateMode.HTML);
        return templateResolver;
    }

    @Bean
    SpringTemplateEngine templateEngine(ApplicationContext applicationContext) {
        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(templateResolver(applicationContext));
        templateEngine.setEnableSpringELCompiler(true);
        return templateEngine;
    }
}
