package br.com.itau.recuperacao.pagamento.infrastructure.messaging.consumer;

import br.com.itau.recuperacao.pagamento.application.service.BoletoService;
import br.com.itau.recuperacao.pagamento.infrastructure.messaging.KafkaTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Consumidor Kafka responsável por escutar eventos de propostas efetivadas
 * e disparar a geração automática de boletos.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PagamentoEventConsumer {

    private final BoletoService boletoService;

    /**
     * Recebe eventos de proposta efetivada do tópico Kafka e delega o
     * processamento para o serviço de boletos.
     *
     * @param evento dados do evento de proposta efetivada
     */
    @KafkaListener(topics = KafkaTopics.RENEGOCIACAO_PROPOSTA_EFETIVADA, groupId = "pagamento-service")
    public void consumirEventoPropostaEfetivada(Map<String, Object> evento) {
        log.info("Evento de proposta efetivada recebido no pagamento-service: {}", evento);

        try {
            boletoService.processarEventoProposta(evento);
            log.info("Evento processado com sucesso — boleto gerado");
        } catch (Exception e) {
            log.error("Erro ao processar evento de proposta efetivada no pagamento-service: {}", e.getMessage(), e);
        }
    }
}
