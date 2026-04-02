package br.com.itau.recuperacao.renegociacao.domain.strategy;

import br.com.itau.recuperacao.renegociacao.domain.model.enums.TipoAcordo;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Estratégia de cálculo sem juros, aplicando desconto de 15% sobre o valor original.
 * <p>
 * O valor negociado é obtido multiplicando o valor original por 0,85 (desconto fixo de 15%).
 * As parcelas são divididas igualmente pelo número solicitado.
 */
@Component
public class CalculoSemJuros implements CalculoParcelaStrategy {

    private static final BigDecimal FATOR_DESCONTO = new BigDecimal("0.85");

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
     * Aplica desconto fixo de 15%: {@code valorOriginal * 0.85}.
     *
     * @throws IllegalArgumentException se valorOriginal ou numeroParcelas forem inválidos
     */
    @Override
    public BigDecimal calcularValorNegociado(BigDecimal valorOriginal, Integer numeroParcelas) {
        validarParametros(valorOriginal, numeroParcelas);
        return valorOriginal.multiply(FATOR_DESCONTO).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public TipoAcordo getTipoAcordo() {
        return TipoAcordo.SEM_JUROS;
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
