package br.com.itau.recuperacao.renegociacao.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Entidade JPA representando uma parcela de uma proposta de renegociação.
 */
@Entity
@Table(name = "parcelas")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParcelaEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposta_id")
    private PropostaEntity proposta;

    @Column(name = "numero_parcela")
    private Integer numeroParcela;

    private BigDecimal valor;

    @Column(name = "data_vencimento")
    private LocalDate dataVencimento;

    private String status;
}
