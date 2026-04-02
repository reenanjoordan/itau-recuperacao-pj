package br.com.itau.recuperacao.renegociacao.application.command;

import java.util.UUID;

/**
 * Comando para efetivação de uma proposta de renegociação existente.
 *
 * @param propostaId identificador da proposta a ser efetivada
 * @param cpfCnpj    CPF ou CNPJ do cliente para validação de titularidade
 */
public record EfetivarPropostaCommand(
        UUID propostaId,
        String cpfCnpj
) {
}
