package br.com.itau.recuperacao.renegociacao.application.handler;

import br.com.itau.recuperacao.renegociacao.api.exception.PropostaNaoEncontradaException;
import br.com.itau.recuperacao.renegociacao.application.command.CancelarPropostaCommand;
import br.com.itau.recuperacao.renegociacao.domain.model.Proposta;
import br.com.itau.recuperacao.renegociacao.domain.repository.PropostaRepository;
import br.com.itau.recuperacao.renegociacao.infrastructure.cache.PropostaCacheService;
import br.com.itau.recuperacao.renegociacao.infrastructure.messaging.producer.PropostaEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Handler responsável por processar o comando de cancelamento de proposta de renegociação.
 * <p>
 * Busca a proposta, executa o cancelamento via método de domínio, persiste,
 * invalida o cache e publica os eventos gerados.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CancelarPropostaCommandHandler {

    private final PropostaRepository propostaRepository;
    private final PropostaEventProducer eventProducer;
    private final PropostaCacheService cacheService;

    /**
     * Processa o comando de cancelamento de proposta.
     * <p>
     * Busca a proposta pelo identificador, executa o cancelamento através do método
     * de domínio {@link Proposta#cancelar(String)}, persiste a alteração, invalida
     * o cache e publica os eventos de domínio.
     *
     * @param command comando contendo o ID da proposta e o motivo do cancelamento
     * @throws PropostaNaoEncontradaException se a proposta não for encontrada
     * @throws IllegalStateException se a proposta estiver em status terminal
     */
    public void handle(CancelarPropostaCommand command) {
        log.info("Cancelando proposta propostaId={}, motivo='{}'", command.propostaId(), command.motivo());

        Proposta proposta = propostaRepository.buscarPorId(command.propostaId())
                .orElseThrow(() -> new PropostaNaoEncontradaException(command.propostaId()));

        proposta.cancelar(command.motivo());

        proposta = propostaRepository.salvar(proposta);

        cacheService.invalidar(proposta.getId());

        proposta.pullDomainEvents().forEach(eventProducer::publicar);

        log.info("Proposta cancelada com sucesso. propostaId={}", proposta.getId());
    }
}
