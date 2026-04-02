package br.com.itau.recuperacao.renegociacao.unit.domain;

import br.com.itau.recuperacao.renegociacao.domain.model.enums.TipoAcordo;
import br.com.itau.recuperacao.renegociacao.domain.strategy.CalculoSemJuros;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("unit")
class CalculoSemJurosTest {

    private final CalculoSemJuros calculo = new CalculoSemJuros();

    @Test
    @DisplayName("Deve calcular valor negociado com desconto de 15%")
    void calcularValorNegociado_deveAplicarDescontoDe15Porcento() {
        BigDecimal valor = new BigDecimal("10000.00");

        BigDecimal resultado = calculo.calcularValorNegociado(valor, 10);

        // 10000 * 0.85 = 8500.00
        assertEquals(new BigDecimal("8500.00"), resultado);
    }

    @Test
    @DisplayName("Deve calcular valor da parcela sem juros")
    void calcularValorParcela_10parcelas_deveRetornarValorCorreto() {
        BigDecimal valor = new BigDecimal("10000.00");
        int parcelas = 10;

        BigDecimal resultado = calculo.calcularValorParcela(valor, parcelas);

        // 8500.00 / 10 = 850.00
        assertEquals(new BigDecimal("850.00"), resultado);
    }

    @Test
    @DisplayName("Deve calcular valor negociado independente do número de parcelas")
    void calcularValorNegociado_comDiferentesParcelas_deveRetornarMesmoValor() {
        BigDecimal valor = new BigDecimal("10000.00");

        BigDecimal resultado1 = calculo.calcularValorNegociado(valor, 1);
        BigDecimal resultado12 = calculo.calcularValorNegociado(valor, 12);
        BigDecimal resultado60 = calculo.calcularValorNegociado(valor, 60);

        assertEquals(new BigDecimal("8500.00"), resultado1);
        assertEquals(new BigDecimal("8500.00"), resultado12);
        assertEquals(new BigDecimal("8500.00"), resultado60);
    }

    @Test
    @DisplayName("Deve calcular parcela para 1 única parcela")
    void calcularValorParcela_1parcela_deveRetornarValorNegociadoIntegral() {
        BigDecimal valor = new BigDecimal("10000.00");

        BigDecimal resultado = calculo.calcularValorParcela(valor, 1);

        assertEquals(new BigDecimal("8500.00"), resultado);
    }

    @Test
    @DisplayName("Deve retornar valor negociado menor que o original")
    void calcularValorNegociado_deveSerMenorQueOriginal() {
        BigDecimal valor = new BigDecimal("50000.00");

        BigDecimal resultado = calculo.calcularValorNegociado(valor, 10);

        assertTrue(resultado.compareTo(valor) < 0);
        assertEquals(new BigDecimal("42500.00"), resultado);
    }

    @Test
    @DisplayName("Deve lançar exceção para valor zero")
    void calcularValorParcela_valorZero_deveLancarException() {
        assertThrows(IllegalArgumentException.class,
                () -> calculo.calcularValorParcela(BigDecimal.ZERO, 10));
    }

    @Test
    @DisplayName("Deve lançar exceção para valor negativo")
    void calcularValorNegociado_valorNegativo_deveLancarException() {
        assertThrows(IllegalArgumentException.class,
                () -> calculo.calcularValorNegociado(new BigDecimal("-100"), 10));
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
                () -> calculo.calcularValorNegociado(null, 10));
    }

    @Test
    @DisplayName("Deve lançar exceção para parcelas nulas")
    void calcularValorParcela_parcelasNulas_deveLancarException() {
        assertThrows(IllegalArgumentException.class,
                () -> calculo.calcularValorParcela(new BigDecimal("10000"), null));
    }

    @Test
    @DisplayName("Deve retornar tipo de acordo SEM_JUROS")
    void getTipoAcordo_deveRetornarSemJuros() {
        assertEquals(TipoAcordo.SEM_JUROS, calculo.getTipoAcordo());
    }

    @Test
    @DisplayName("Deve arredondar corretamente parcelas com divisão inexata")
    void calcularValorParcela_divisaoInexata_deveArredondarCorretamente() {
        BigDecimal valor = new BigDecimal("10000.00");
        int parcelas = 3;

        BigDecimal resultado = calculo.calcularValorParcela(valor, parcelas);

        // 8500.00 / 3 = 2833.33 (HALF_UP)
        assertEquals(new BigDecimal("2833.33"), resultado);
    }
}
