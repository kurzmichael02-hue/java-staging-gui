package de.logicalis.java_staging_gui;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class Config {

    @Bean
    public RestTemplate restTemplate() {
        // set timeouts so a hanging ZTP server does not block the GUI -
        // without them the start request would wait forever and the interface "freezes"
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofSeconds(5))
                .setConnectionRequestTimeout(Timeout.ofSeconds(5))
                .setResponseTimeout(Timeout.ofSeconds(10))
                .build();

        CloseableHttpClient client = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build();

        HttpComponentsClientHttpRequestFactory factory =
            new HttpComponentsClientHttpRequestFactory(client);
        return new RestTemplate(factory);
    }
}
