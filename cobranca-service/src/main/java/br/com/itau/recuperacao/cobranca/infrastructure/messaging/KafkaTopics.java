package br.com.itau.recuperacao.cobranca.infrastructure.messaging;

/**
 * Nomes dos tópicos Kafka (alinhados ao {@code renegociacao-service} / infra Docker).
 */
public final class KafkaTopics {

    private KafkaTopics() {
    }

    public static final String RENEGOCIACAO_PROPOSTA_EFETIVADA = "renegociacao.proposta.efetivada";

    public static final String COBRANCA_ACAO_REALIZADA = "cobranca.acao.realizada";
}
