package br.com.itau.recuperacao.renegociacao.api.dto.response;

import br.com.itau.recuperacao.renegociacao.domain.model.enums.PropostaStatus;
import br.com.itau.recuperacao.renegociacao.domain.model.enums.TipoAcordo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO de resposta contendo os dados de uma proposta de renegociação.
 *
 * @param id                 identificador da proposta
 * @param cpfCnpj            CPF ou CNPJ do cliente
 * @param status             status atual da proposta
 * @param valorTotal         valor total das dívidas agrupadas
 * @param valorNegociado     valor negociado após cálculo
 * @param percentualDesconto percentual de desconto aplicado
 * @param numeroParcelas     número de parcelas
 * @param valorParcela       valor de cada parcela
 * @param tipoAcordo         tipo de acordo selecionado
 * @param dividas            lista de dívidas incluídas na proposta
 * @param parcelas           lista de parcelas geradas
 * @param criadaEm           data/hora de criação
 * @param atualizadaEm       data/hora da última atualização
 * @param efetivadaEm        data/hora da efetivação; preenchido apenas quando status é EFETIVADA
 */
public record PropostaResponse(
        UUID id,
        String cpfCnpj,
        PropostaStatus status,
        BigDecimal valorTotal,
        BigDecimal valorNegociado,
        BigDecimal percentualDesconto,
        Integer numeroParcelas,
        BigDecimal valorParcela,
        TipoAcordo tipoAcordo,
        List<DividaResponse> dividas,
        List<ParcelaResponse> parcelas,
        LocalDateTime criadaEm,
        LocalDateTime atualizadaEm,
        LocalDateTime efetivadaEm
) {
}
