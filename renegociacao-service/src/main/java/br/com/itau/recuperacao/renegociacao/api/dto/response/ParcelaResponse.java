package br.com.itau.recuperacao.renegociacao.api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO de resposta contendo os dados de uma parcela de renegociação.
 *
 * @param id             identificador da parcela
 * @param numeroParcela  número sequencial da parcela
 * @param valor          valor da parcela
 * @param dataVencimento data de vencimento da parcela
 * @param status         status da parcela
 */
public record ParcelaResponse(
        UUID id,
        Integer numeroParcela,
        BigDecimal valor,
        LocalDate dataVencimento,
        String status
) {
}
