package br.com.itau.recuperacao.renegociacao.unit.domain;

import br.com.itau.recuperacao.renegociacao.domain.model.enums.TipoAcordo;
import br.com.itau.recuperacao.renegociacao.domain.strategy.CalculoJurosSimples;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("unit")
class CalculoJurosSimplesTest {

    private final CalculoJurosSimples calculo = new CalculoJurosSimples();

    @Test
    @DisplayName("Deve calcular valor negociado com juros simples para 12 parcelas")
    void calcularValorNegociado_12parcelas_deveAplicarJurosSimples() {
        BigDecimal valor = new BigDecimal("10000.00");
        int parcelas = 12;

        BigDecimal resultado = calculo.calcularValorNegociado(valor, parcelas);

        // 10000 * (1 + 0.015 * 12) = 10000 * 1.18 = 11800.00
        assertEquals(new BigDecimal("11800.00"), resultado);
    }

    @Test
    @DisplayName("Deve calcular valor da parcela com juros simples")
    void calcularValorParcela_12parcelas_deveRetornarValorCorreto() {
        BigDecimal valor = new BigDecimal("10000.00");
        int parcelas = 12;

        BigDecimal resultado = calculo.calcularValorParcela(valor, parcelas);

        // 11800.00 / 12 = 983.33
        assertEquals(new BigDecimal("983.33"), resultado);
    }

    @Test
    @DisplayName("Deve calcular valor negociado para 1 parcela")
    void calcularValorNegociado_1parcela_deveAplicarTaxaUnica() {
        BigDecimal valor = new BigDecimal("10000.00");
        int parcelas = 1;

        BigDecimal resultado = calculo.calcularValorNegociado(valor, parcelas);

        // 10000 * (1 + 0.015 * 1) = 10000 * 1.015 = 10150.00
        assertEquals(new BigDecimal("10150.00"), resultado);
    }

    @Test
    @DisplayName("Deve calcular valor da parcela para 1 parcela")
    void calcularValorParcela_1parcela_deveRetornarValorNegociadoIntegral() {
        BigDecimal valor = new BigDecimal("10000.00");

        BigDecimal resultado = calculo.calcularValorParcela(valor, 1);

        assertEquals(new BigDecimal("10150.00"), resultado);
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
                () -> calculo.calcularValorNegociado(new BigDecimal("-100"), 12));
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
    @DisplayName("Deve retornar tipo de acordo JUROS_SIMPLES")
    void getTipoAcordo_deveRetornarJurosSimples() {
        assertEquals(TipoAcordo.JUROS_SIMPLES, calculo.getTipoAcordo());
    }

    @Test
    @DisplayName("Deve calcular corretamente para valores com centavos")
    void calcularValorParcela_comCentavos_deveArredondarCorretamente() {
        BigDecimal valor = new BigDecimal("9999.99");
        int parcelas = 7;

        BigDecimal valorNegociado = calculo.calcularValorNegociado(valor, parcelas);
        BigDecimal valorParcela = calculo.calcularValorParcela(valor, parcelas);

        // 9999.99 * (1 + 0.015 * 7) = 9999.99 * 1.105 = 11049.99
        assertEquals(new BigDecimal("11049.99"), valorNegociado);
        // 11049.99 / 7 = 1578.57
        assertEquals(new BigDecimal("1578.57"), valorParcela);
    }
}
