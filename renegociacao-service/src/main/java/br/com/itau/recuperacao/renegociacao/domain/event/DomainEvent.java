package br.com.itau.recuperacao.renegociacao.domain.event;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Interface base para todos os eventos de domínio do serviço de renegociação.
 * <p>
 * Cada evento deve conter um identificador único, a data de ocorrência,
 * o identificador do agregado raiz e o tipo do evento.
 */
public interface DomainEvent {

    /**
     * Retorna o identificador único do evento.
     *
     * @return UUID do evento
     */
    UUID getEventId();

    /**
     * Retorna a data e hora em que o evento ocorreu.
     *
     * @return data/hora da ocorrência
     */
    LocalDateTime getOccurredOn();

    /**
     * Retorna o identificador do agregado raiz associado ao evento.
     *
     * @return UUID do agregado
     */
    UUID getAggregateId();

    /**
     * Retorna o tipo do evento como string identificadora.
     *
     * @return tipo do evento
     */
    String getEventType();
}
