package br.com.itau.recuperacao.renegociacao.domain.strategy;

import br.com.itau.recuperacao.renegociacao.domain.model.enums.TipoAcordo;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Estratégia de cálculo com juros simples a uma taxa mensal de 1,5%.
 * <p>
 * O valor negociado é calculado como: {@code valorOriginal * (1 + taxa * numeroParcelas)}.
 * O valor de cada parcela é obtido pela divisão do valor negociado pelo número de parcelas.
 */
@Component
public class CalculoJurosSimples implements CalculoParcelaStrategy {

    private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.015");

    /**
     * {@inheritDoc}
     *
     * @throws IllegalArgumentException se valorTotal ou numeroParcelas forem inválidos
     */
    @Override
    public BigDecimal calcularValorParcela(BigDecimal valorTotal, Integer numeroParcelas) {
        validarParametros(valorTotal, numeroParcelas);
        BigDecimal valorNegociado = calcularValorNegociado(valorTotal, numeroParcelas);
        return valorNegociado.divide(BigDecimal.valueOf(numeroParcelas), 2, RoundingMode.HALF_UP);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Fórmula: {@code valorOriginal * (1 + 0.015 * numeroParcelas)}.
     *
     * @throws IllegalArgumentException se valorOriginal ou numeroParcelas forem inválidos
     */
    @Override
    public BigDecimal calcularValorNegociado(BigDecimal valorOriginal, Integer numeroParcelas) {
        validarParametros(valorOriginal, numeroParcelas);
        BigDecimal fator = BigDecimal.ONE.add(TAXA_MENSAL.multiply(BigDecimal.valueOf(numeroParcelas)));
        return valorOriginal.multiply(fator).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public TipoAcordo getTipoAcordo() {
        return TipoAcordo.JUROS_SIMPLES;
    }

    private void validarParametros(BigDecimal valor, Integer parcelas) {
        if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O valor deve ser maior que zero");
        }
        if (parcelas == null || parcelas <= 0) {
            throw new IllegalArgumentException("O número de parcelas deve ser maior que zero");
        }
    }
}
