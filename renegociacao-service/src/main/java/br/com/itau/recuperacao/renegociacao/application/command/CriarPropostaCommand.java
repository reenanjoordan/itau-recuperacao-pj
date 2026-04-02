package br.com.itau.recuperacao.renegociacao.application.command;

import br.com.itau.recuperacao.renegociacao.domain.model.enums.TipoAcordo;

import java.util.List;

/**
 * Comando para criação de uma nova proposta de renegociação de dívidas.
 *
 * @param cpfCnpj        CPF ou CNPJ do cliente
 * @param contratosIds   lista de identificadores dos contratos a serem renegociados
 * @param numeroParcelas número de parcelas desejado
 * @param tipoAcordo     tipo de acordo selecionado
 */
public record CriarPropostaCommand(
        String cpfCnpj,
        List<String> contratosIds,
        Integer numeroParcelas,
        TipoAcordo tipoAcordo
) {
}
