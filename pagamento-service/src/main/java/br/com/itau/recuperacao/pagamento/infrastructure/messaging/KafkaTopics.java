package br.com.itau.recuperacao.pagamento.infrastructure.messaging;

/**
 * Nomes dos tópicos Kafka do pagamento-service.
 */
public final class KafkaTopics {

    private KafkaTopics() {
    }

    public static final String RENEGOCIACAO_PROPOSTA_EFETIVADA = "renegociacao.proposta.efetivada";

    public static final String PAGAMENTO_BOLETO_GERADO = "pagamento.boleto.gerado";
}
