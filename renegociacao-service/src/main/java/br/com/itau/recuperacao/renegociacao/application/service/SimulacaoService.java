package br.com.itau.recuperacao.renegociacao.application.service;

import br.com.itau.recuperacao.renegociacao.api.dto.request.SimulacaoRequest;
import br.com.itau.recuperacao.renegociacao.api.dto.response.SimulacaoResponse;
import br.com.itau.recuperacao.renegociacao.domain.factory.CalculoParcelaStrategyFactory;
import br.com.itau.recuperacao.renegociacao.domain.model.Divida;
import br.com.itau.recuperacao.renegociacao.domain.model.enums.TipoAcordo;
import br.com.itau.recuperacao.renegociacao.domain.repository.DividaRepository;
import br.com.itau.recuperacao.renegociacao.domain.strategy.CalculoParcelaStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Serviço de aplicação responsável pela simulação de opções de renegociação de dívidas.
 * <p>
 * Para cada combinação de número de parcelas e tipo de acordo, calcula os valores
 * negociados, parcelas e descontos, retornando todas as opções disponíveis ao cliente.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SimulacaoService {

    private final DividaRepository dividaRepository;
    private final CalculoParcelaStrategyFactory strategyFactory;

    /**
     * Realiza a simulação de renegociação para as dívidas e opções de parcelas informadas.
     * <p>
     * Para cada número de parcelas desejado, calcula os valores usando todas as estratégias
     * disponíveis (Sem Juros, Juros Simples e Juros Compostos), retornando uma lista
     * completa de opções para o cliente.
     *
     * @param request request contendo CPF/CNPJ, contratos e opções de parcelas
     * @return lista de {@link SimulacaoResponse} com todas as opções simuladas
     */
    public List<SimulacaoResponse> simular(SimulacaoRequest request) {
        log.info("Simulando renegociação para cpfCnpj={}", maskCpfCnpj(request.cpfCnpj()));

        List<Divida> todasDividas = dividaRepository.buscarDividasElegiveis(request.cpfCnpj());

        Set<String> contratosDesejados = Set.copyOf(request.contratosIds());
        List<Divida> dividasFiltradas = todasDividas.stream()
                .filter(d -> contratosDesejados.contains(d.getContrato()))
                .toList();

        BigDecimal valorTotal = dividasFiltradas.stream()
                .map(Divida::getValorAtualizado)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (dividasFiltradas.isEmpty() || valorTotal.compareTo(BigDecimal.ZERO) <= 0) {
            log.info("Nenhuma dívida encontrada para simulação. cpfCnpj={}", maskCpfCnpj(request.cpfCnpj()));
            return List.of();
        }

        List<SimulacaoResponse> resultados = new ArrayList<>();

        for (Integer numeroParcelas : request.numerosParcelasDesejados()) {
            for (TipoAcordo tipoAcordo : TipoAcordo.values()) {
                CalculoParcelaStrategy strategy = strategyFactory.getStrategy(tipoAcordo);

                BigDecimal valorNegociado = strategy.calcularValorNegociado(valorTotal, numeroParcelas);
                BigDecimal valorParcela = strategy.calcularValorParcela(valorTotal, numeroParcelas);

                BigDecimal percentualDesconto;
                if (valorNegociado.compareTo(valorTotal) >= 0) {
                    percentualDesconto = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
                } else {
                    percentualDesconto = BigDecimal.ONE
                            .subtract(valorNegociado.divide(valorTotal, 10, RoundingMode.HALF_UP))
                            .multiply(new BigDecimal("100"))
                            .setScale(2, RoundingMode.HALF_UP);
                }

                resultados.add(new SimulacaoResponse(
                        tipoAcordo, numeroParcelas, valorTotal,
                        valorNegociado, valorParcela, percentualDesconto));
            }
        }

        log.info("Simulação concluída. {} opções geradas para cpfCnpj={}",
                resultados.size(), maskCpfCnpj(request.cpfCnpj()));

        return resultados;
    }

    private String maskCpfCnpj(String cpfCnpj) {
        if (cpfCnpj == null || cpfCnpj.length() < 16) {
            return "***";
        }
        return cpfCnpj.substring(0, 14) + "**";
    }
}
