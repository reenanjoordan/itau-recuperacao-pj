package br.com.itau.recuperacao.renegociacao.application.handler;

import br.com.itau.recuperacao.renegociacao.api.dto.response.PropostaResponse;
import br.com.itau.recuperacao.renegociacao.api.exception.PropostaNaoEncontradaException;
import br.com.itau.recuperacao.renegociacao.application.mapper.PropostaResponseMapper;
import br.com.itau.recuperacao.renegociacao.application.query.BuscarPropostaQuery;
import br.com.itau.recuperacao.renegociacao.domain.model.Proposta;
import br.com.itau.recuperacao.renegociacao.domain.repository.PropostaRepository;
import br.com.itau.recuperacao.renegociacao.infrastructure.cache.PropostaCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Handler responsável por processar a query de busca de proposta de renegociação.
 * <p>
 * Implementa cache-aside: primeiro consulta o cache e, em caso de miss,
 * busca no repositório e armazena o resultado no cache.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BuscarPropostaQueryHandler {

    private final PropostaRepository propostaRepository;
    private final PropostaCacheService cacheService;
    private final PropostaResponseMapper propostaResponseMapper;

    /**
     * Processa a query de busca de proposta pelo identificador.
     * <p>
     * Consulta o cache primeiro (cache-aside). Em caso de miss, busca no repositório,
     * mapeia para response, armazena no cache e retorna.
     *
     * @param query query contendo o identificador da proposta
     * @return {@link PropostaResponse} com os dados da proposta
     * @throws PropostaNaoEncontradaException se a proposta não for encontrada
     */
    public PropostaResponse handle(BuscarPropostaQuery query) {
        log.info("Buscando proposta propostaId={}", query.propostaId());

        Optional<PropostaResponse> cached = cacheService.buscar(query.propostaId());
        if (cached.isPresent()) {
            log.debug("Proposta encontrada no cache. propostaId={}", query.propostaId());
            return cached.get();
        }

        Proposta proposta = propostaRepository.buscarPorId(query.propostaId())
                .orElseThrow(() -> new PropostaNaoEncontradaException(query.propostaId()));

        PropostaResponse response = propostaResponseMapper.toResponse(proposta);

        cacheService.armazenar(query.propostaId(), response);

        log.info("Proposta encontrada e armazenada em cache. propostaId={}", query.propostaId());

        return response;
    }
}
