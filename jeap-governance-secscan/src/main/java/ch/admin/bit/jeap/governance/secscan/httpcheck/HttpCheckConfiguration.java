package ch.admin.bit.jeap.governance.secscan.httpcheck;

import ch.admin.bit.jeap.governance.secscan.domain.HttpEndpointSecurityChecker;
import ch.admin.bit.jeap.rest.tracing.AddSenderSystemHeaderToRestClient;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
class HttpCheckConfiguration {

    @Bean
    HttpEndpointSecurityChecker httpEndpointSecurityChecker(RestClient.Builder restClientBuilder, HttpCheckConfigurationProperties httpCheckConfigurationProperties) {
        ClientHttpRequestFactory requestFactory = ClientHttpRequestFactoryBuilder
                .simple()
                .withCustomizer(c -> {
                    c.setConnectTimeout(httpCheckConfigurationProperties.getConnectTimeout());
                    c.setReadTimeout(httpCheckConfigurationProperties.getReadTimeout());
                })
                .build();
        RestClient restClient = restClientBuilder
                // Explicitly remove the caller name header to prevent the architecture repository from creating extra relations
                .defaultHeaders(headers -> headers.remove(AddSenderSystemHeaderToRestClient.APPLICATION_NAME_HEADER))
                .requestFactory(requestFactory)
                .build();
        return new DefaultHttpEndpointSecurityChecker(restClient);
    }
}
