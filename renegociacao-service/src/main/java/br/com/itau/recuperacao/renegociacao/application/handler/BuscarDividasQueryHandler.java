package br.com.itau.recuperacao.renegociacao.application.handler;

import br.com.itau.recuperacao.renegociacao.api.dto.response.DividaResponse;
import br.com.itau.recuperacao.renegociacao.application.query.BuscarDividasQuery;
import br.com.itau.recuperacao.renegociacao.domain.model.Divida;
import br.com.itau.recuperacao.renegociacao.domain.repository.DividaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Handler responsável por processar a query de busca de dívidas elegíveis para renegociação.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BuscarDividasQueryHandler {

    private final DividaRepository dividaRepository;

    /**
     * Processa a query de busca de dívidas elegíveis para um determinado CPF/CNPJ.
     *
     * @param query query contendo o CPF/CNPJ do cliente
     * @return lista de {@link DividaResponse} com as dívidas encontradas
     */
    public List<DividaResponse> handle(BuscarDividasQuery query) {
        log.info("Buscando dívidas elegíveis para cpfCnpj={}", maskCpfCnpj(query.cpfCnpj()));

        List<Divida> dividas = dividaRepository.buscarDividasElegiveis(query.cpfCnpj());

        log.info("Encontradas {} dívidas elegíveis para cpfCnpj={}",
                dividas.size(), maskCpfCnpj(query.cpfCnpj()));

        return dividas.stream()
                .map(this::toResponse)
                .toList();
    }

    private DividaResponse toResponse(Divida divida) {
        return new DividaResponse(
                divida.getId(), divida.getContrato(), divida.getValorOriginal(),
                divida.getValorAtualizado(), divida.getDataVencimento(),
                divida.getDiasAtraso(), divida.getProduto(), divida.isElegivel());
    }

    private String maskCpfCnpj(String cpfCnpj) {
        if (cpfCnpj == null || cpfCnpj.length() < 16) {
            return "***";
        }
        return cpfCnpj.substring(0, 14) + "**";
    }
}
