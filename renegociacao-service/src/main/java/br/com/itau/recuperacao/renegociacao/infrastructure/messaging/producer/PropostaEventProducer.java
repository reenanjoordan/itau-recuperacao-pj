package br.com.itau.recuperacao.renegociacao.infrastructure.messaging.producer;

import br.com.itau.recuperacao.renegociacao.domain.event.DomainEvent;
import br.com.itau.recuperacao.renegociacao.infrastructure.messaging.KafkaTopicNames;
import br.com.itau.recuperacao.renegociacao.domain.event.PropostaCanceladaEvent;
import br.com.itau.recuperacao.renegociacao.domain.event.PropostaCriadaEvent;
import br.com.itau.recuperacao.renegociacao.domain.event.PropostaEfetivadaEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.KafkaException;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Produtor de eventos de proposta para o Kafka.
 * Publica eventos de domínio nos tópicos correspondentes.
 */
@Component
@Slf4j
public class PropostaEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final Counter eventosPublicadosCounter;
    private final Counter eventosErroCounter;

    public PropostaEventProducer(KafkaTemplate<String, Object> kafkaTemplate, MeterRegistry meterRegistry) {
        this.kafkaTemplate = kafkaTemplate;
        this.eventosPublicadosCounter = Counter.builder("propostas_eventos_publicados_total")
                .description("Total de eventos de proposta publicados com sucesso")
                .register(meterRegistry);
        this.eventosErroCounter = Counter.builder("propostas_eventos_erro_total")
                .description("Total de erros ao publicar eventos de proposta")
                .register(meterRegistry);
    }

    /**
     * Publica um evento de domínio no tópico Kafka correspondente.
     *
     * @param event evento de domínio a ser publicado
     */
    public void publicar(DomainEvent event) {
        String topic = determinarTopico(event);
        String aggregateId = event.getAggregateId().toString();
        String traceId = UUID.randomUUID().toString();

        MDC.put("traceId", traceId);
        try {
            kafkaTemplate.send(topic, aggregateId, event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Erro ao publicar evento no tópico={}, aggregateId={}: {}",
                                    topic, aggregateId, ex.getMessage(), ex);
                            eventosErroCounter.increment();
                        } else {
                            log.info("Evento publicado com sucesso no tópico={}, aggregateId={}, offset={}",
                                    topic, aggregateId, result.getRecordMetadata().offset());
                            eventosPublicadosCounter.increment();
                        }
                    });
        } catch (KafkaException ex) {
            log.error("Erro ao enviar evento para Kafka, tópico={}, aggregateId={}: {}",
                    topic, aggregateId, ex.getMessage(), ex);
            eventosErroCounter.increment();
        } finally {
            MDC.remove("traceId");
        }
    }

    private String determinarTopico(DomainEvent event) {
        if (event instanceof PropostaCriadaEvent) {
            return KafkaTopicNames.RENEGOCIACAO_PROPOSTA_CRIADA;
        } else if (event instanceof PropostaEfetivadaEvent) {
            return KafkaTopicNames.RENEGOCIACAO_PROPOSTA_EFETIVADA;
        } else if (event instanceof PropostaCanceladaEvent) {
            return KafkaTopicNames.RENEGOCIACAO_PROPOSTA_CANCELADA;
        }
        throw new IllegalArgumentException("Tipo de evento não suportado: " + event.getClass().getSimpleName());
    }
}
