package br.com.itau.recuperacao.renegociacao.infrastructure.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Indicador de saúde do cluster Kafka (listagem de tópicos com timeout curto).
 */
@Component
@Slf4j
public class KafkaHealthIndicator implements HealthIndicator {

    private final String bootstrapServers;

    public KafkaHealthIndicator(
            @Value("${spring.kafka.bootstrap-servers:localhost:9092}") String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
    }

    /**
     * Verifica conectividade ao broker Kafka.
     *
     * @return saúde UP se a operação administrativa responder a tempo
     */
    @Override
    public Health health() {
        Map<String, Object> config = Map.of(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 5_000);
        try (AdminClient admin = AdminClient.create(config)) {
            admin.listTopics().names().get(4, TimeUnit.SECONDS);
            return Health.up()
                    .withDetail("bootstrapServers", bootstrapServers)
                    .build();
        } catch (Exception ex) {
            log.warn("Health check Kafka falhou: {}", ex.getMessage());
            return Health.down(ex)
                    .withDetail("bootstrapServers", bootstrapServers)
                    .build();
        }
    }
}
