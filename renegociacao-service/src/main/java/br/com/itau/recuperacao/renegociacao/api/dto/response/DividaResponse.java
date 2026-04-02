package br.com.itau.recuperacao.renegociacao.api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO de resposta contendo os dados de uma dívida elegível.
 *
 * @param id              identificador da dívida
 * @param contrato        número do contrato legado
 * @param valorOriginal   valor original da dívida
 * @param valorAtualizado valor atualizado com encargos
 * @param dataVencimento  data de vencimento original
 * @param diasAtraso      dias em atraso
 * @param produto         tipo de produto (ex: "Empréstimo PJ")
 * @param elegivel        indica se a dívida é elegível para renegociação
 */
public record DividaResponse(
        UUID id,
        String contrato,
        BigDecimal valorOriginal,
        BigDecimal valorAtualizado,
        LocalDate dataVencimento,
        Integer diasAtraso,
        String produto,
        boolean elegivel
) {
}
