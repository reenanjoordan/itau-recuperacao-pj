package br.com.itau.recuperacao.pagamento.domain.model;

import br.com.itau.recuperacao.pagamento.domain.model.enums.BoletoStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Modelo de domínio que representa um boleto bancário gerado para pagamento
 * de parcelas de uma proposta de renegociação.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Boleto {

    private UUID id;
    private UUID propostaId;
    private String codigoBarras;
    private BigDecimal valor;
    private LocalDate dataVencimento;
    private BoletoStatus status;
    private LocalDateTime criadoEm;
}
