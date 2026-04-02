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
 * Entidade JPA representando uma dívida vinculada a uma proposta.
 */
@Entity
@Table(name = "dividas")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DividaEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proposta_id")
    private PropostaEntity proposta;

    @Column(nullable = false)
    private String contrato;

    @Column(name = "valor_original")
    private BigDecimal valorOriginal;

    @Column(name = "valor_atualizado")
    private BigDecimal valorAtualizado;

    @Column(name = "data_vencimento")
    private LocalDate dataVencimento;

    @Column(name = "dias_atraso")
    private Integer diasAtraso;

    private String produto;
}
