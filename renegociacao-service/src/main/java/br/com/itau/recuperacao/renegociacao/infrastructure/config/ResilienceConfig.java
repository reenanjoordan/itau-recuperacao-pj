package br.com.itau.recuperacao.renegociacao.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Configuração de resiliência e RestTemplate com timeouts.
 * Circuit Breaker e Retry são configurados via application.yml.
 */
@Configuration
public class ResilienceConfig {

    /**
     * Configura o RestTemplate com timeouts de conexão e leitura de 5 segundos.
     *
     * @return RestTemplate configurado
     */
    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(5000);
        return new RestTemplate(factory);
    }
}
