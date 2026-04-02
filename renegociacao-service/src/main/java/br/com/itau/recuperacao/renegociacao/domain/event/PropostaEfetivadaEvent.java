package br.com.itau.recuperacao.renegociacao.domain.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Evento de domínio emitido quando uma proposta de renegociação é efetivada.
 */
public record PropostaEfetivadaEvent(
        UUID eventId,
        LocalDateTime occurredOn,
        UUID propostaId,
        String cpfCnpj,
        BigDecimal valorNegociado,
        Integer numeroParcelas,
        BigDecimal valorParcela,
        LocalDateTime efetivadaEm
) implements DomainEvent {

    /**
     * Construtor simplificado que gera automaticamente o eventId e a data de ocorrência.
     *
     * @param propostaId     identificador da proposta efetivada
     * @param cpfCnpj        CPF/CNPJ do cliente
     * @param valorNegociado valor negociado final
     * @param numeroParcelas número de parcelas
     * @param valorParcela   valor de cada parcela
     * @param efetivadaEm    data/hora da efetivação
     */
    public PropostaEfetivadaEvent(UUID propostaId, String cpfCnpj, BigDecimal valorNegociado,
                                  Integer numeroParcelas, BigDecimal valorParcela, LocalDateTime efetivadaEm) {
        this(UUID.randomUUID(), LocalDateTime.now(), propostaId, cpfCnpj, valorNegociado,
                numeroParcelas, valorParcela, efetivadaEm);
    }

    @Override
    public UUID getEventId() {
        return eventId;
    }

    @Override
    public LocalDateTime getOccurredOn() {
        return occurredOn;
    }

    @Override
    public UUID getAggregateId() {
        return propostaId;
    }

    @Override
    public String getEventType() {
        return "PROPOSTA_EFETIVADA";
    }
}
