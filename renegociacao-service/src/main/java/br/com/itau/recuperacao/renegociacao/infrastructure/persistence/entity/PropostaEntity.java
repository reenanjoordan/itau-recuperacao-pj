package br.com.itau.recuperacao.renegociacao.infrastructure.persistence.entity;

import br.com.itau.recuperacao.renegociacao.domain.model.enums.PropostaStatus;
import br.com.itau.recuperacao.renegociacao.domain.model.enums.TipoAcordo;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entidade JPA representando uma proposta de renegociação no banco de dados.
 */
@Entity
@Table(name = "propostas")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PropostaEntity {

    @Id
    private UUID id;

    @Column(name = "cpf_cnpj", nullable = false)
    private String cpfCnpj;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PropostaStatus status;

    @Column(name = "valor_total")
    private BigDecimal valorTotal;

    @Column(name = "valor_negociado")
    private BigDecimal valorNegociado;

    @Column(name = "percentual_desconto")
    private BigDecimal percentualDesconto;

    @Column(name = "numero_parcelas")
    private Integer numeroParcelas;

    @Column(name = "valor_parcela")
    private BigDecimal valorParcela;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_acordo")
    private TipoAcordo tipoAcordo;

    @OneToMany(mappedBy = "proposta", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DividaEntity> dividas = new ArrayList<>();

    @OneToMany(mappedBy = "proposta", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ParcelaEntity> parcelas = new ArrayList<>();

    @Column(name = "criada_em", nullable = false)
    private LocalDateTime criadaEm;

    @Column(name = "atualizada_em")
    private LocalDateTime atualizadaEm;

    @Version
    @Column(name = "versao")
    private Long versao;
}
