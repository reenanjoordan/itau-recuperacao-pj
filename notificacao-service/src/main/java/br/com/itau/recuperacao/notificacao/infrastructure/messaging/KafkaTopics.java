package br.com.itau.recuperacao.notificacao.infrastructure.messaging;

/**
 * Nomes dos tópicos Kafka consumidos pelo notificacao-service.
 */
public final class KafkaTopics {

    private KafkaTopics() {
    }

    public static final String RENEGOCIACAO_PROPOSTA_EFETIVADA = "renegociacao.proposta.efetivada";

    public static final String RENEGOCIACAO_PROPOSTA_CRIADA = "renegociacao.proposta.criada";
}
