package br.com.itau.recuperacao.renegociacao.domain.model.enums;

import java.math.BigDecimal;

/**
 * Enum que representa os tipos de acordo disponíveis para renegociação de dívidas PJ.
 * <p>
 * Cada tipo define uma descrição legível e a taxa mensal padrão aplicada ao cálculo.
 */
public enum TipoAcordo {

    SEM_JUROS("Sem Juros", new BigDecimal("0.00")),
    JUROS_SIMPLES("Juros Simples", new BigDecimal("0.015")),
    JUROS_COMPOSTOS("Juros Compostos", new BigDecimal("0.015"));

    private final String descricao;
    private final BigDecimal taxaMensalPadrao;

    TipoAcordo(String descricao, BigDecimal taxaMensalPadrao) {
        this.descricao = descricao;
        this.taxaMensalPadrao = taxaMensalPadrao;
    }

    /**
     * Retorna a descrição legível do tipo de acordo.
     *
     * @return descrição do tipo
     */
    public String getDescricao() {
        return descricao;
    }

    /**
     * Retorna a taxa mensal padrão associada ao tipo de acordo.
     *
     * @return taxa mensal padrão como {@link BigDecimal}
     */
    public BigDecimal getTaxaMensalPadrao() {
        return taxaMensalPadrao;
    }
}
