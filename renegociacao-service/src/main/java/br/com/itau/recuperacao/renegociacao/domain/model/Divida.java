package br.com.itau.recuperacao.renegociacao.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Value Object que representa uma dívida elegível para renegociação.
 * <p>
 * Contém informações do contrato legado, valores originais e atualizados,
 * e indicação de elegibilidade para o processo de renegociação.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Divida {

    private UUID id;
    private String contrato;
    private BigDecimal valorOriginal;
    private BigDecimal valorAtualizado;
    private LocalDate dataVencimento;
    private Integer diasAtraso;
    private String produto;
    private boolean elegivel;
}
