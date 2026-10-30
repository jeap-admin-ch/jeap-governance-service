package ch.admin.bit.jeap.governance.docservice.webhook;

import ch.admin.bit.jeap.security.restclient.JeapOAuth2RestClientBuilderFactory;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DocServiceWebhookConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(JeapOAuth2RestClientBuilderFactory.class, DocServiceWebhookConfigurationTest::restClientBuilderFactory)
            .withUserConfiguration(DocServiceWebhookConfiguration.class);

    @Test
    void offByDefault_noWebhookIsCalled() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(DocServiceWebhookClient.class);
            assertThat(context).doesNotHaveBean(ScoringRunCompletedWebhookListener.class);
        });
    }

    @Test
    void onWithUrlAndClientRegistration_callsTheWebhook() {
        contextRunner
                .withPropertyValues("jeap.governance.doc-service.webhook.enabled=true",
                        "jeap.governance.doc-service.webhook.url=https://doc.example.com/api/governance/webhook",
                        "jeap.governance.doc-service.webhook.client-registration=doc-service-webhook")
                .run(context -> {
                    assertThat(context).hasSingleBean(DocServiceWebhookClient.class);
                    assertThat(context).hasSingleBean(ScoringRunCompletedWebhookListener.class);
                });
    }

    @Test
    void onWithoutUrl_refusesToStartNamingTheSetting() {
        contextRunner
                .withPropertyValues("jeap.governance.doc-service.webhook.enabled=true",
                        "jeap.governance.doc-service.webhook.client-registration=doc-service-webhook")
                .run(context -> assertThat(context).getFailure()
                        .rootCause()
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("jeap.governance.doc-service.webhook.url")
                        .hasMessageNotContaining("client-registration is not set"));
    }

    @Test
    void onWithoutClientRegistration_refusesToStartNamingTheSetting() {
        contextRunner
                .withPropertyValues("jeap.governance.doc-service.webhook.enabled=true",
                        "jeap.governance.doc-service.webhook.url=https://doc.example.com/api/governance/webhook")
                .run(context -> assertThat(context).getFailure()
                        .rootCause()
                        .hasMessageContaining("jeap.governance.doc-service.webhook.client-registration"));
    }

    @Test
    void onWithoutEither_namesBoth() {
        contextRunner
                .withPropertyValues("jeap.governance.doc-service.webhook.enabled=true")
                .run(context -> assertThat(context).getFailure()
                        .rootCause()
                        .hasMessageContaining("jeap.governance.doc-service.webhook.url and "
                                              + "jeap.governance.doc-service.webhook.client-registration"));
    }

    private static JeapOAuth2RestClientBuilderFactory restClientBuilderFactory() {
        JeapOAuth2RestClientBuilderFactory factory = mock(JeapOAuth2RestClientBuilderFactory.class);
        when(factory.createForClientRegistryId(anyString())).thenReturn(RestClient.builder());
        return factory;
    }
}
