package br.com.itau.recuperacao.renegociacao.application.query;

import java.util.UUID;

/**
 * Query para busca de uma proposta de renegociação pelo identificador.
 *
 * @param propostaId identificador da proposta
 */
public record BuscarPropostaQuery(
        UUID propostaId
) {
}
