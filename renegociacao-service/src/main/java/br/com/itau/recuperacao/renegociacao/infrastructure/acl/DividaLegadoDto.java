package br.com.itau.recuperacao.renegociacao.infrastructure.acl;

import java.math.BigDecimal;

/**
 * DTO representando uma dívida retornada pelo sistema legado.
 */
public record DividaLegadoDto(
    String contrato,
    BigDecimal valorOriginal,
    BigDecimal valorAtualizado,
    String dataVencimento,
    Integer diasAtraso,
    String produto
) {}
