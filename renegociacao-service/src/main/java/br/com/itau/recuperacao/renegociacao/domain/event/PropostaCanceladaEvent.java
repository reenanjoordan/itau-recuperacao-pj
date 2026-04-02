package br.com.itau.recuperacao.renegociacao.domain.event;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Evento de domínio emitido quando uma proposta de renegociação é cancelada.
 */
public record PropostaCanceladaEvent(
        UUID eventId,
        LocalDateTime occurredOn,
        UUID propostaId,
        String cpfCnpj,
        String motivo,
        LocalDateTime canceladaEm
) implements DomainEvent {

    /**
     * Construtor simplificado que gera automaticamente o eventId e a data de ocorrência.
     *
     * @param propostaId identificador da proposta cancelada
     * @param cpfCnpj    CPF/CNPJ do cliente
     * @param motivo     motivo do cancelamento
     * @param canceladaEm data/hora do cancelamento
     */
    public PropostaCanceladaEvent(UUID propostaId, String cpfCnpj, String motivo, LocalDateTime canceladaEm) {
        this(UUID.randomUUID(), LocalDateTime.now(), propostaId, cpfCnpj, motivo, canceladaEm);
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
        return "PROPOSTA_CANCELADA";
    }
}
