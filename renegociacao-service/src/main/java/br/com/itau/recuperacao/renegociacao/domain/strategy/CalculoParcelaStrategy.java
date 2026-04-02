package br.com.itau.recuperacao.renegociacao.domain.strategy;

import br.com.itau.recuperacao.renegociacao.domain.model.enums.TipoAcordo;

import java.math.BigDecimal;

/**
 * Estratégia de cálculo de parcelas para renegociação de dívidas.
 * <p>
 * Cada implementação define uma forma distinta de calcular o valor negociado
 * e o valor de cada parcela com base no tipo de acordo.
 */
public interface CalculoParcelaStrategy {

    /**
     * Calcula o valor de cada parcela com base no valor total e no número de parcelas.
     *
     * @param valorTotal     valor total das dívidas a serem renegociadas
     * @param numeroParcelas número de parcelas desejado
     * @return valor de cada parcela
     */
    BigDecimal calcularValorParcela(BigDecimal valorTotal, Integer numeroParcelas);

    /**
     * Calcula o valor total negociado com base no valor original e no número de parcelas.
     *
     * @param valorOriginal  valor original das dívidas
     * @param numeroParcelas número de parcelas desejado
     * @return valor total negociado
     */
    BigDecimal calcularValorNegociado(BigDecimal valorOriginal, Integer numeroParcelas);

    /**
     * Retorna o tipo de acordo associado a esta estratégia.
     *
     * @return {@link TipoAcordo} correspondente
     */
    TipoAcordo getTipoAcordo();
}
