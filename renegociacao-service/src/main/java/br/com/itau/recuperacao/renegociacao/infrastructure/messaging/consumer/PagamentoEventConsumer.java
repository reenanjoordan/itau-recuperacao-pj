package br.com.itau.recuperacao.renegociacao.infrastructure.messaging.consumer;

import br.com.itau.recuperacao.renegociacao.infrastructure.messaging.KafkaTopicNames;
import br.com.itau.recuperacao.renegociacao.infrastructure.persistence.entity.PropostaEntity;
import br.com.itau.recuperacao.renegociacao.infrastructure.persistence.jpa.PropostaJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Consumidor de eventos de pagamento do Kafka.
 * Processa notificações de boletos gerados pelo serviço de pagamento.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PagamentoEventConsumer {

    private final PropostaJpaRepository propostaJpaRepository;

    /**
     * Consome eventos de boleto gerado pelo serviço de pagamento.
     *
     * @param record registro do Kafka contendo dados do boleto
     */
    @KafkaListener(topics = KafkaTopicNames.PAGAMENTO_BOLETO_GERADO, groupId = "renegociacao-service")
    public void consumir(ConsumerRecord<String, Map<String, Object>> record) {
        Map<String, Object> payload = record.value();
        String propostaIdStr = String.valueOf(payload.get("propostaId"));
        String boletoId = String.valueOf(payload.get("boletoId"));

        log.info("Boleto gerado recebido para proposta={}, boletoId={}", propostaIdStr, boletoId);

        try {
            UUID propostaId = UUID.fromString(propostaIdStr);
            Optional<PropostaEntity> propostaOpt = propostaJpaRepository.findById(propostaId);

            if (propostaOpt.isEmpty()) {
                log.warn("Proposta não encontrada para atualização de boleto, propostaId={}", propostaId);
                return;
            }

            PropostaEntity proposta = propostaOpt.get();
            proposta.setAtualizadaEm(LocalDateTime.now());
            propostaJpaRepository.save(proposta);

            log.info("Proposta={} atualizada com referência ao boleto={}", propostaId, boletoId);

        } catch (IllegalArgumentException ex) {
            log.error("Erro ao processar evento de boleto gerado: propostaId inválido={}", propostaIdStr, ex);
        }
    }
}
