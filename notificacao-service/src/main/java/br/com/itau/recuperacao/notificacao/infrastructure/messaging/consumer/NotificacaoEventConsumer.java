package br.com.itau.recuperacao.notificacao.infrastructure.messaging.consumer;

import br.com.itau.recuperacao.notificacao.application.service.NotificacaoService;
import br.com.itau.recuperacao.notificacao.infrastructure.messaging.KafkaTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Consumidor Kafka responsável por escutar eventos de propostas e disparar
 * notificações automáticas para os clientes PJ envolvidos.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificacaoEventConsumer {

    private final NotificacaoService notificacaoService;

    /**
     * Recebe eventos de proposta criada e efetivada dos tópicos Kafka
     * e delega o processamento para o serviço de notificação.
     *
     * @param evento dados do evento de proposta
     */
    @KafkaListener(
            topics = {
                    KafkaTopics.RENEGOCIACAO_PROPOSTA_EFETIVADA,
                    KafkaTopics.RENEGOCIACAO_PROPOSTA_CRIADA
            },
            groupId = "notificacao-service"
    )
    public void consumirEventoProposta(Map<String, Object> evento) {
        log.info("Evento de proposta recebido no notificacao-service: {}", evento);

        try {
            notificacaoService.processarEventoProposta(evento);
            log.info("Evento de proposta processado com sucesso pelo notificacao-service");
        } catch (Exception e) {
            log.error("Erro ao processar evento de proposta no notificacao-service: {}", e.getMessage(), e);
        }
    }
}
