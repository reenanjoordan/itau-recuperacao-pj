package br.com.itau.recuperacao.renegociacao.domain.strategy;

import br.com.itau.recuperacao.renegociacao.domain.model.enums.TipoAcordo;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Estratégia de cálculo com juros compostos a uma taxa mensal de 1,5%.
 * <p>
 * O valor negociado é calculado como: {@code valorOriginal * (1 + taxa)^numeroParcelas}.
 * O valor da parcela utiliza a fórmula Price (PMT):
 * {@code PMT = PV * [i * (1+i)^n] / [(1+i)^n - 1]}.
 */
@Component
public class CalculoJurosCompostos implements CalculoParcelaStrategy {

    private static final BigDecimal TAXA_MENSAL = new BigDecimal("0.015");
    private static final MathContext MC = MathContext.DECIMAL128;

    /**
     * {@inheritDoc}
     * <p>
     * Utiliza a fórmula Price (tabela Price / sistema francês de amortização):
     * {@code PMT = PV * [i * (1+i)^n] / [(1+i)^n - 1]}, onde PV = valorTotal,
     * i = taxa mensal (0,015), n = numeroParcelas.
     *
     * @throws IllegalArgumentException se valorTotal ou numeroParcelas forem inválidos
     */
    @Override
    public BigDecimal calcularValorParcela(BigDecimal valorTotal, Integer numeroParcelas) {
        validarParametros(valorTotal, numeroParcelas);

        double taxa = TAXA_MENSAL.doubleValue();
        int n = numeroParcelas;
        double fatorComposto = Math.pow(1 + taxa, n);

        double numerador = taxa * fatorComposto;
        double denominador = fatorComposto - 1;

        BigDecimal pmt = valorTotal.multiply(BigDecimal.valueOf(numerador / denominador), MC);
        return pmt.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Fórmula: {@code valorOriginal * (1 + 0.015)^numeroParcelas}.
     *
     * @throws IllegalArgumentException se valorOriginal ou numeroParcelas forem inválidos
     */
    @Override
    public BigDecimal calcularValorNegociado(BigDecimal valorOriginal, Integer numeroParcelas) {
        validarParametros(valorOriginal, numeroParcelas);

        double fator = Math.pow(1 + TAXA_MENSAL.doubleValue(), numeroParcelas);
        return valorOriginal.multiply(BigDecimal.valueOf(fator), MC).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public TipoAcordo getTipoAcordo() {
        return TipoAcordo.JUROS_COMPOSTOS;
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
