package br.com.itau.recuperacao.renegociacao.domain.event;

import br.com.itau.recuperacao.renegociacao.domain.model.enums.TipoAcordo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Evento de domínio emitido quando uma nova proposta de renegociação é criada.
 */
public record PropostaCriadaEvent(
        UUID eventId,
        LocalDateTime occurredOn,
        UUID propostaId,
        String cpfCnpj,
        BigDecimal valorTotal,
        Integer numeroParcelas,
        TipoAcordo tipoAcordo,
        LocalDateTime criadaEm
) implements DomainEvent {

    /**
     * Construtor simplificado que gera automaticamente o eventId e a data de ocorrência.
     *
     * @param propostaId     identificador da proposta criada
     * @param cpfCnpj        CPF/CNPJ do cliente
     * @param valorTotal     valor total das dívidas agrupadas
     * @param numeroParcelas número de parcelas escolhido
     * @param tipoAcordo     tipo de acordo selecionado
     * @param criadaEm       data/hora de criação da proposta
     */
    public PropostaCriadaEvent(UUID propostaId, String cpfCnpj, BigDecimal valorTotal,
                               Integer numeroParcelas, TipoAcordo tipoAcordo, LocalDateTime criadaEm) {
        this(UUID.randomUUID(), LocalDateTime.now(), propostaId, cpfCnpj, valorTotal,
                numeroParcelas, tipoAcordo, criadaEm);
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
        return "PROPOSTA_CRIADA";
    }
}
