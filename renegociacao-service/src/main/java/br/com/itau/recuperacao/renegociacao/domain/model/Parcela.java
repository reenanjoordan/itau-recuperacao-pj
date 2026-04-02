package br.com.itau.recuperacao.renegociacao.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Value Object que representa uma parcela de um acordo de renegociação.
 * <p>
 * Cada parcela possui um número sequencial, valor, data de vencimento e status de pagamento.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Parcela {

    private UUID id;
    private Integer numeroParcela;
    private BigDecimal valor;
    private LocalDate dataVencimento;
    @Builder.Default
    private String status = "PENDENTE";
}
