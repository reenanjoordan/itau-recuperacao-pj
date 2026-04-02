package br.com.itau.recuperacao.renegociacao.infrastructure.messaging;

/**
 * Nomes dos tópicos Kafka utilizados pelo serviço de renegociação e referenciados
 * por consumidores nos demais microsserviços. Auto-criação habilitada no broker local
 * ({@code KAFKA_AUTO_CREATE_TOPICS_ENABLE} no {@code docker-compose.yml}).
 */
public final class KafkaTopicNames {

    private KafkaTopicNames() {
    }

    /** Evento de proposta criada (persistida). */
    public static final String RENEGOCIACAO_PROPOSTA_CRIADA = "renegociacao.proposta.criada";

    /** Evento de proposta efetivada (Saga — cobrança, pagamento, notificação). */
    public static final String RENEGOCIACAO_PROPOSTA_EFETIVADA = "renegociacao.proposta.efetivada";

    /** Evento de proposta cancelada. */
    public static final String RENEGOCIACAO_PROPOSTA_CANCELADA = "renegociacao.proposta.cancelada";

    /** Boleto gerado pelo pagamento-service (consumido pela renegociação). */
    public static final String PAGAMENTO_BOLETO_GERADO = "pagamento.boleto.gerado";

    /** Ação de cobrança concluída (publicado pelo cobranca-service). */
    public static final String COBRANCA_ACAO_REALIZADA = "cobranca.acao.realizada";
}
