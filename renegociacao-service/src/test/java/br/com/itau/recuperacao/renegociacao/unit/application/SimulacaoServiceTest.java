package br.com.itau.recuperacao.renegociacao.unit.application;

import br.com.itau.recuperacao.renegociacao.api.dto.request.SimulacaoRequest;
import br.com.itau.recuperacao.renegociacao.api.dto.response.SimulacaoResponse;
import br.com.itau.recuperacao.renegociacao.application.service.SimulacaoService;
import br.com.itau.recuperacao.renegociacao.domain.factory.CalculoParcelaStrategyFactory;
import br.com.itau.recuperacao.renegociacao.domain.model.Divida;
import br.com.itau.recuperacao.renegociacao.domain.model.enums.TipoAcordo;
import br.com.itau.recuperacao.renegociacao.domain.repository.DividaRepository;
import br.com.itau.recuperacao.renegociacao.domain.strategy.CalculoJurosCompostos;
import br.com.itau.recuperacao.renegociacao.domain.strategy.CalculoJurosSimples;
import br.com.itau.recuperacao.renegociacao.domain.strategy.CalculoSemJuros;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class SimulacaoServiceTest {

    @Mock
    private DividaRepository dividaRepository;

    @Mock
    private CalculoParcelaStrategyFactory strategyFactory;

    @InjectMocks
    private SimulacaoService simulacaoService;

    private List<Divida> criarDividasElegiveis() {
        return List.of(
                Divida.builder()
                        .id(UUID.randomUUID())
                        .contrato("CTR-001")
                        .valorOriginal(new BigDecimal("5000.00"))
                        .valorAtualizado(new BigDecimal("6000.00"))
                        .dataVencimento(LocalDate.now().minusMonths(6))
                        .diasAtraso(180)
                        .produto("Capital de Giro")
                        .elegivel(true)
                        .build(),
                Divida.builder()
                        .id(UUID.randomUUID())
                        .contrato("CTR-002")
                        .valorOriginal(new BigDecimal("3000.00"))
                        .valorAtualizado(new BigDecimal("4000.00"))
                        .dataVencimento(LocalDate.now().minusMonths(3))
                        .diasAtraso(90)
                        .produto("Cheque Especial")
                        .elegivel(true)
                        .build()
        );
    }

    @Test
    @DisplayName("Deve retornar simulações para todos os tipos de acordo")
    void simular_comDividasElegiveis_deveRetornarTodasOpcoes() {
        String cpfCnpj = "12.345.678/0001-99";
        SimulacaoRequest request = new SimulacaoRequest(cpfCnpj, List.of("CTR-001", "CTR-002"), List.of(12));

        when(dividaRepository.buscarDividasElegiveis(cpfCnpj)).thenReturn(criarDividasElegiveis());
        when(strategyFactory.getStrategy(TipoAcordo.SEM_JUROS)).thenReturn(new CalculoSemJuros());
        when(strategyFactory.getStrategy(TipoAcordo.JUROS_SIMPLES)).thenReturn(new CalculoJurosSimples());
        when(strategyFactory.getStrategy(TipoAcordo.JUROS_COMPOSTOS)).thenReturn(new CalculoJurosCompostos());

        List<SimulacaoResponse> resultado = simulacaoService.simular(request);

        assertNotNull(resultado);
        assertEquals(3, resultado.size());

        SimulacaoResponse semJuros = resultado.stream()
                .filter(s -> TipoAcordo.SEM_JUROS.equals(s.tipoAcordo()))
                .findFirst().orElse(null);
        assertNotNull(semJuros);
        assertEquals(new BigDecimal("10000.00"), semJuros.valorTotal());
        assertEquals(new BigDecimal("8500.00"), semJuros.valorNegociado());
        assertEquals(12, semJuros.numeroParcelas());

        SimulacaoResponse jurosSimples = resultado.stream()
                .filter(s -> TipoAcordo.JUROS_SIMPLES.equals(s.tipoAcordo()))
                .findFirst().orElse(null);
        assertNotNull(jurosSimples);
        assertEquals(new BigDecimal("11800.00"), jurosSimples.valorNegociado());
    }

    @Test
    @DisplayName("Deve retornar simulações para múltiplas opções de parcelas")
    void simular_comMultiplasOpcoesParcelas_deveRetornarCombinacoes() {
        String cpfCnpj = "12.345.678/0001-99";
        SimulacaoRequest request = new SimulacaoRequest(cpfCnpj, List.of("CTR-001", "CTR-002"), List.of(6, 12));

        when(dividaRepository.buscarDividasElegiveis(cpfCnpj)).thenReturn(criarDividasElegiveis());
        when(strategyFactory.getStrategy(TipoAcordo.SEM_JUROS)).thenReturn(new CalculoSemJuros());
        when(strategyFactory.getStrategy(TipoAcordo.JUROS_SIMPLES)).thenReturn(new CalculoJurosSimples());
        when(strategyFactory.getStrategy(TipoAcordo.JUROS_COMPOSTOS)).thenReturn(new CalculoJurosCompostos());

        List<SimulacaoResponse> resultado = simulacaoService.simular(request);

        assertNotNull(resultado);
        assertEquals(6, resultado.size());

        long countParcelas6 = resultado.stream().filter(s -> s.numeroParcelas() == 6).count();
        long countParcelas12 = resultado.stream().filter(s -> s.numeroParcelas() == 12).count();
        assertEquals(3, countParcelas6);
        assertEquals(3, countParcelas12);
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando não há dívidas")
    void simular_semDividas_deveRetornarListaVazia() {
        String cpfCnpj = "12.345.678/0001-99";
        SimulacaoRequest request = new SimulacaoRequest(cpfCnpj, List.of("CTR-INEXISTENTE"), List.of(12));

        when(dividaRepository.buscarDividasElegiveis(cpfCnpj)).thenReturn(Collections.emptyList());

        List<SimulacaoResponse> resultado = simulacaoService.simular(request);

        assertNotNull(resultado);
        assertTrue(resultado.isEmpty());
    }

    @Test
    @DisplayName("Deve retornar valores de parcela coerentes na simulação")
    void simular_comDividasElegiveis_deveRetornarValoresParcelaCoerentes() {
        String cpfCnpj = "12.345.678/0001-99";
        SimulacaoRequest request = new SimulacaoRequest(cpfCnpj, List.of("CTR-001", "CTR-002"), List.of(10));

        when(dividaRepository.buscarDividasElegiveis(cpfCnpj)).thenReturn(criarDividasElegiveis());
        when(strategyFactory.getStrategy(TipoAcordo.SEM_JUROS)).thenReturn(new CalculoSemJuros());
        when(strategyFactory.getStrategy(TipoAcordo.JUROS_SIMPLES)).thenReturn(new CalculoJurosSimples());
        when(strategyFactory.getStrategy(TipoAcordo.JUROS_COMPOSTOS)).thenReturn(new CalculoJurosCompostos());

        List<SimulacaoResponse> resultado = simulacaoService.simular(request);

        assertFalse(resultado.isEmpty());
        for (SimulacaoResponse simulacao : resultado) {
            assertNotNull(simulacao.valorParcela());
            assertNotNull(simulacao.valorNegociado());
            assertTrue(simulacao.valorParcela().compareTo(BigDecimal.ZERO) > 0);
            assertTrue(simulacao.valorNegociado().compareTo(BigDecimal.ZERO) > 0);
        }
    }
}
