package br.com.itau.recuperacao.renegociacao.application.handler;

import br.com.itau.recuperacao.renegociacao.api.dto.response.PropostaResponse;
import br.com.itau.recuperacao.renegociacao.api.exception.PropostaNaoEncontradaException;
import br.com.itau.recuperacao.renegociacao.application.command.EfetivarPropostaCommand;
import br.com.itau.recuperacao.renegociacao.application.mapper.PropostaResponseMapper;
import br.com.itau.recuperacao.renegociacao.domain.model.Proposta;
import br.com.itau.recuperacao.renegociacao.domain.repository.PropostaRepository;
import br.com.itau.recuperacao.renegociacao.infrastructure.cache.PropostaCacheService;
import br.com.itau.recuperacao.renegociacao.infrastructure.messaging.producer.PropostaEventProducer;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Handler responsável por processar o comando de efetivação de proposta de renegociação.
 * <p>
 * Valida a titularidade do CPF/CNPJ, efetiva a proposta no domínio, persiste,
 * invalida o cache e publica os eventos gerados.
 */
@Service
@Slf4j
public class EfetivarPropostaCommandHandler {

    private final PropostaRepository propostaRepository;
    private final PropostaEventProducer eventProducer;
    private final PropostaCacheService cacheService;
    private final PropostaResponseMapper propostaResponseMapper;
    private final Counter propostasEfetivadasCounter;

    public EfetivarPropostaCommandHandler(
            PropostaRepository propostaRepository,
            PropostaEventProducer eventProducer,
            PropostaCacheService cacheService,
            PropostaResponseMapper propostaResponseMapper,
            MeterRegistry meterRegistry) {
        this.propostaRepository = propostaRepository;
        this.eventProducer = eventProducer;
        this.cacheService = cacheService;
        this.propostaResponseMapper = propostaResponseMapper;
        this.propostasEfetivadasCounter = Counter.builder("propostas_efetivadas_total")
                .description("Total de propostas efetivadas com sucesso")
                .register(meterRegistry);
    }

    /**
     * Processa o comando de efetivação de proposta.
     * <p>
     * Busca a proposta, valida que o CPF/CNPJ corresponde ao titular, efetiva a proposta
     * através do método de domínio, persiste, invalida o cache e publica os eventos.
     *
     * @param command comando contendo o ID da proposta e CPF/CNPJ do titular
     * @return {@link PropostaResponse} com os dados da proposta efetivada
     * @throws PropostaNaoEncontradaException se a proposta não for encontrada
     * @throws IllegalStateException          se o CPF/CNPJ não corresponder ao titular
     */
    public PropostaResponse handle(EfetivarPropostaCommand command) {
        log.info("Efetivando proposta propostaId={}, cpfCnpj={}",
                command.propostaId(), maskCpfCnpj(command.cpfCnpj()));

        Proposta proposta = propostaRepository.buscarPorId(command.propostaId())
                .orElseThrow(() -> new PropostaNaoEncontradaException(command.propostaId()));

        if (!proposta.getCpfCnpj().equals(command.cpfCnpj())) {
            throw new IllegalStateException(
                    "CPF/CNPJ informado não corresponde ao titular da proposta");
        }

        proposta.efetivar();

        proposta = propostaRepository.salvar(proposta);

        cacheService.invalidar(proposta.getId());

        proposta.pullDomainEvents().forEach(eventProducer::publicar);

        propostasEfetivadasCounter.increment();

        log.info("Proposta efetivada com sucesso. propostaId={}", proposta.getId());

        return propostaResponseMapper.toResponse(proposta);
    }

    private String maskCpfCnpj(String cpfCnpj) {
        if (cpfCnpj == null || cpfCnpj.length() < 16) {
            return "***";
        }
        return cpfCnpj.substring(0, 14) + "**";
    }
}
