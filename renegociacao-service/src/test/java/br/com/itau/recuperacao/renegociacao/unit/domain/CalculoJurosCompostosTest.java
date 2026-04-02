package br.com.itau.recuperacao.renegociacao.unit.domain;

import br.com.itau.recuperacao.renegociacao.domain.model.enums.TipoAcordo;
import br.com.itau.recuperacao.renegociacao.domain.strategy.CalculoJurosCompostos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("unit")
class CalculoJurosCompostosTest {

    private final CalculoJurosCompostos calculo = new CalculoJurosCompostos();

    @Test
    @DisplayName("Deve calcular valor negociado com juros compostos para 12 parcelas")
    void calcularValorNegociado_12parcelas_deveAplicarJurosCompostos() {
        BigDecimal valor = new BigDecimal("10000.00");
        int parcelas = 12;

        BigDecimal resultado = calculo.calcularValorNegociado(valor, parcelas);

        // 10000 * (1.015)^12 ≈ 11956.18
        BigDecimal esperado = new BigDecimal("11956.18");
        assertEquals(esperado, resultado);
    }

    @Test
    @DisplayName("Deve calcular parcela pela tabela Price")
    void calcularValorParcela_12parcelas_deveUsarTabelaPrice() {
        BigDecimal valor = new BigDecimal("10000.00");
        int parcelas = 12;

        BigDecimal resultado = calculo.calcularValorParcela(valor, parcelas);

        // PMT calculado com double + arredondamento HALF_UP em 2 casas (ver CalculoJurosCompostos)
        BigDecimal esperado = new BigDecimal("916.80");
        assertEquals(esperado, resultado);
    }

    @Test
    @DisplayName("Deve calcular valor negociado para 1 parcela")
    void calcularValorNegociado_1parcela_deveAplicarTaxaUnica() {
        BigDecimal valor = new BigDecimal("10000.00");

        BigDecimal resultado = calculo.calcularValorNegociado(valor, 1);

        // 10000 * (1.015)^1 = 10150.00
        assertEquals(new BigDecimal("10150.00"), resultado);
    }

    @Test
    @DisplayName("Deve calcular parcela para 1 única parcela")
    void calcularValorParcela_1parcela_deveRetornarValorTotal() {
        BigDecimal valor = new BigDecimal("10000.00");

        BigDecimal resultado = calculo.calcularValorParcela(valor, 1);

        // PMT com n=1: PV * [i*(1+i)] / [(1+i) - 1] = PV * 1.015 * 0.015 / 0.015 = PV * 1.015
        assertEquals(new BigDecimal("10150.00"), resultado);
    }

    @Test
    @DisplayName("Deve calcular valor negociado para 24 parcelas")
    void calcularValorNegociado_24parcelas_deveAplicarJurosCompostos() {
        BigDecimal valor = new BigDecimal("10000.00");

        BigDecimal resultado = calculo.calcularValorNegociado(valor, 24);

        // 10000 * (1.015)^24
        double fator = Math.pow(1.015, 24);
        BigDecimal esperado = valor.multiply(BigDecimal.valueOf(fator)).setScale(2, java.math.RoundingMode.HALF_UP);
        assertEquals(esperado, resultado);
    }

    @Test
    @DisplayName("Deve retornar valor negociado maior que o original para múltiplas parcelas")
    void calcularValorNegociado_multiplasParcelas_deveSerMaiorQueOriginal() {
        BigDecimal valor = new BigDecimal("10000.00");

        BigDecimal resultado = calculo.calcularValorNegociado(valor, 12);

        assertTrue(resultado.compareTo(valor) > 0);
    }

    @Test
    @DisplayName("Deve lançar exceção para valor zero")
    void calcularValorParcela_valorZero_deveLancarException() {
        assertThrows(IllegalArgumentException.class,
                () -> calculo.calcularValorParcela(BigDecimal.ZERO, 12));
    }

    @Test
    @DisplayName("Deve lançar exceção para valor negativo")
    void calcularValorNegociado_valorNegativo_deveLancarException() {
        assertThrows(IllegalArgumentException.class,
                () -> calculo.calcularValorNegociado(new BigDecimal("-5000"), 12));
    }

    @Test
    @DisplayName("Deve lançar exceção para parcelas negativas")
    void calcularValorParcela_parcelasNegativas_deveLancarException() {
        assertThrows(IllegalArgumentException.class,
                () -> calculo.calcularValorParcela(new BigDecimal("10000"), -1));
    }

    @Test
    @DisplayName("Deve lançar exceção para parcelas zero")
    void calcularValorParcela_parcelasZero_deveLancarException() {
        assertThrows(IllegalArgumentException.class,
                () -> calculo.calcularValorParcela(new BigDecimal("10000"), 0));
    }

    @Test
    @DisplayName("Deve lançar exceção para valor nulo")
    void calcularValorNegociado_valorNulo_deveLancarException() {
        assertThrows(IllegalArgumentException.class,
                () -> calculo.calcularValorNegociado(null, 12));
    }

    @Test
    @DisplayName("Deve lançar exceção para parcelas nulas")
    void calcularValorParcela_parcelasNulas_deveLancarException() {
        assertThrows(IllegalArgumentException.class,
                () -> calculo.calcularValorParcela(new BigDecimal("10000"), null));
    }

    @Test
    @DisplayName("Deve retornar tipo de acordo JUROS_COMPOSTOS")
    void getTipoAcordo_deveRetornarJurosCompostos() {
        assertEquals(TipoAcordo.JUROS_COMPOSTOS, calculo.getTipoAcordo());
    }

    @Test
    @DisplayName("Deve calcular parcela Price com soma total coerente com o financiamento à taxa mensal")
    void calcularValorParcela_somaParcelasMaiorQuePrincipal() {
        BigDecimal valor = new BigDecimal("10000.00");
        int n = 12;

        BigDecimal valorParcela = calculo.calcularValorParcela(valor, n);
        BigDecimal somaParcelas = valorParcela.multiply(BigDecimal.valueOf(n));

        // Sistema Price sobre o principal: total pago inclui juros embutidos nas parcelas
        assertTrue(somaParcelas.compareTo(valor) > 0);
        assertTrue(somaParcelas.compareTo(valor.multiply(new BigDecimal("1.25"))) < 0);
    }
}
