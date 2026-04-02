package br.com.itau.recuperacao.renegociacao.api.dto.response;

import br.com.itau.recuperacao.renegociacao.domain.model.enums.TipoAcordo;

import java.math.BigDecimal;

/**
 * DTO de resposta contendo uma opção de simulação de renegociação.
 *
 * @param tipoAcordo         tipo de acordo simulado
 * @param numeroParcelas     número de parcelas da simulação
 * @param valorTotal         valor total das dívidas
 * @param valorNegociado     valor negociado após cálculo
 * @param valorParcela       valor de cada parcela
 * @param percentualDesconto percentual de desconto aplicado (pode ser negativo para juros)
 */
public record SimulacaoResponse(
        TipoAcordo tipoAcordo,
        Integer numeroParcelas,
        BigDecimal valorTotal,
        BigDecimal valorNegociado,
        BigDecimal valorParcela,
        BigDecimal percentualDesconto
) {
}
