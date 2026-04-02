package br.com.itau.recuperacao.cobranca.infrastructure.messaging.consumer;

import br.com.itau.recuperacao.cobranca.application.service.CobrancaService;
import br.com.itau.recuperacao.cobranca.infrastructure.messaging.KafkaTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Consumidor Kafka responsável por escutar eventos de propostas efetivadas
 * e disparar ações de cobrança correspondentes.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CobrancaEventConsumer {

    private final CobrancaService cobrancaService;

    /**
     * Recebe eventos de proposta efetivada do tópico Kafka e delega o
     * processamento para o serviço de cobrança.
     *
     * @param evento dados do evento de proposta efetivada
     */
    @KafkaListener(topics = KafkaTopics.RENEGOCIACAO_PROPOSTA_EFETIVADA, groupId = "cobranca-service")
    public void consumirEventoPropostaEfetivada(Map<String, Object> evento) {
        log.info("Evento de proposta efetivada recebido no cobranca-service: {}", evento);

        try {
            cobrancaService.processarEventoProposta(evento);
            log.info("Evento de proposta efetivada processado com sucesso");
        } catch (Exception e) {
            log.error("Erro ao processar evento de proposta efetivada: {}", e.getMessage(), e);
        }
    }
}
