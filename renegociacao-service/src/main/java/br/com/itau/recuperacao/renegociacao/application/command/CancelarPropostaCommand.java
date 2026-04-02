package br.com.itau.recuperacao.renegociacao.application.command;

import java.util.UUID;

/**
 * Comando para cancelamento de uma proposta de renegociação.
 *
 * @param propostaId identificador da proposta a ser cancelada
 * @param motivo     motivo do cancelamento
 */
public record CancelarPropostaCommand(
        UUID propostaId,
        String motivo
) {
}
