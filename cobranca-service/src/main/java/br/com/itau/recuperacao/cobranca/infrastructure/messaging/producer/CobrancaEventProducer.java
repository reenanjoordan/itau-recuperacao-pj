package br.com.itau.recuperacao.cobranca.infrastructure.messaging.producer;

import br.com.itau.recuperacao.cobranca.domain.model.AcaoCobranca;
import br.com.itau.recuperacao.cobranca.infrastructure.messaging.KafkaTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Produtor Kafka responsável por publicar eventos de ações de cobrança realizadas.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CobrancaEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Publica um evento no tópico Kafka informando que uma ação de cobrança foi realizada.
     *
     * @param acao a ação de cobrança que foi executada
     */
    public void publicarAcaoRealizada(AcaoCobranca acao) {
        log.info("Publicando evento de ação realizada. AcaoId: {}, Tipo: {}", acao.getId(), acao.getTipoAcao());

        Map<String, Object> evento = new HashMap<>();
        evento.put("acaoId", acao.getId().toString());
        evento.put("cpfCnpj", acao.getCpfCnpj());
        evento.put("tipoAcao", acao.getTipoAcao().name());
        evento.put("canal", acao.getCanal().name());
        evento.put("descricao", acao.getDescricao());
        evento.put("status", acao.getStatus());
        evento.put("criadaEm", acao.getCriadaEm().toString());

        kafkaTemplate.send(KafkaTopics.COBRANCA_ACAO_REALIZADA, acao.getCpfCnpj(), evento)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Falha ao publicar evento de ação realizada: {}", ex.getMessage(), ex);
                    } else {
                        log.info("Evento de ação realizada publicado com sucesso. Offset: {}",
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
