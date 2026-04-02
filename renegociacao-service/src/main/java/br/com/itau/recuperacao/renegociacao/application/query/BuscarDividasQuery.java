package br.com.itau.recuperacao.renegociacao.application.query;

/**
 * Query para busca de dívidas elegíveis de um cliente.
 *
 * @param cpfCnpj CPF ou CNPJ do cliente
 */
public record BuscarDividasQuery(
        String cpfCnpj
) {
}
