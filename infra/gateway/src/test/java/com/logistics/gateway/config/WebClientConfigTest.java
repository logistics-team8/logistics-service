package com.logistics.gateway.config;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.webclient.WebClientCustomizer;
import org.springframework.web.reactive.function.client.WebClient;

@ExtendWith(MockitoExtension.class)
class WebClientConfigTest {

    @Mock private ObjectProvider<WebClientCustomizer> customizerProvider;

    @Mock private WebClientCustomizer customizer;

    @Test
    void appliesAutoConfiguredCustomizersToLoadBalancedBuilder() {
        given(customizerProvider.orderedStream()).willReturn(Stream.of(customizer));

        WebClient.Builder builder = new WebClientConfig().webClientBuilder(customizerProvider);

        verify(customizer).customize(builder);
    }
}
