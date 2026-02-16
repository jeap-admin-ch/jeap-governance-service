package ch.admin.bit.jeap.governance.secscan.apidiscovery;

import ch.admin.bit.jeap.governance.secscan.domain.SystemComponentHttpApiDiscoveryClient;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
class ApiDiscoveryConfiguration {

    @Bean
    SystemComponentHttpApiDiscoveryClient apiDiscoveryClient(RestClient.Builder restClientBuilder, ApiDiscoveryClientConfigurationProperties apiDiscoveryClientConfigProperties) {
        ClientHttpRequestFactory requestFactory = ClientHttpRequestFactoryBuilder
                .simple()
                .withCustomizer(c -> {
                    c.setConnectTimeout(apiDiscoveryClientConfigProperties.getTimeout());
                    c.setReadTimeout(apiDiscoveryClientConfigProperties.getTimeout());
                })
                .build();
        RestClient restClient = restClientBuilder
                .requestFactory(requestFactory)
                .build();
        return new ApiDiscoveryClient(restClient, apiDiscoveryClientConfigProperties);
    }
}
