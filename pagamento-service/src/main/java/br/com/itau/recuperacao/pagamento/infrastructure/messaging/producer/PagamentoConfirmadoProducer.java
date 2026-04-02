package br.com.itau.recuperacao.pagamento.infrastructure.messaging.producer;

import br.com.itau.recuperacao.pagamento.infrastructure.messaging.KafkaTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Produtor Kafka responsável por publicar eventos de boletos gerados,
 * notificando outros serviços sobre a disponibilidade de pagamento.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PagamentoConfirmadoProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Publica um evento no tópico Kafka informando que um boleto foi gerado
     * para uma proposta de renegociação.
     *
     * @param propostaId identificador da proposta vinculada
     * @param boletoId   identificador do boleto gerado
     */
    public void publicarBoletoGerado(UUID propostaId, UUID boletoId) {
        log.info("Publicando evento de boleto gerado. PropostaId: {}, BoletoId: {}", propostaId, boletoId);

        Map<String, Object> evento = new HashMap<>();
        evento.put("propostaId", propostaId.toString());
        evento.put("boletoId", boletoId.toString());
        evento.put("timestamp", LocalDateTime.now().toString());
        evento.put("eventType", "BOLETO_GERADO");

        kafkaTemplate.send(KafkaTopics.PAGAMENTO_BOLETO_GERADO, propostaId.toString(), evento)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Falha ao publicar evento de boleto gerado: {}", ex.getMessage(), ex);
                    } else {
                        log.info("Evento de boleto gerado publicado com sucesso. Offset: {}",
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
