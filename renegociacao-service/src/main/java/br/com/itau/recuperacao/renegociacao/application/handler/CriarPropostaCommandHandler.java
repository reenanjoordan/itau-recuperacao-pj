package br.com.itau.recuperacao.renegociacao.application.handler;

import br.com.itau.recuperacao.renegociacao.api.dto.response.PropostaResponse;
import br.com.itau.recuperacao.renegociacao.api.exception.DividaInelegivelException;
import br.com.itau.recuperacao.renegociacao.application.command.CriarPropostaCommand;
import br.com.itau.recuperacao.renegociacao.application.mapper.PropostaResponseMapper;
import br.com.itau.recuperacao.renegociacao.domain.factory.CalculoParcelaStrategyFactory;
import br.com.itau.recuperacao.renegociacao.domain.model.Divida;
import br.com.itau.recuperacao.renegociacao.domain.model.Proposta;
import br.com.itau.recuperacao.renegociacao.domain.repository.DividaRepository;
import br.com.itau.recuperacao.renegociacao.domain.repository.PropostaRepository;
import br.com.itau.recuperacao.renegociacao.domain.strategy.CalculoParcelaStrategy;
import br.com.itau.recuperacao.renegociacao.infrastructure.messaging.producer.PropostaEventProducer;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Handler responsável por processar o comando de criação de proposta de renegociação.
 * <p>
 * Busca as dívidas elegíveis, valida os contratos solicitados, calcula os valores
 * utilizando a estratégia adequada e persiste a proposta gerada.
 */
@Service
@Slf4j
public class CriarPropostaCommandHandler {

    private final DividaRepository dividaRepository;
    private final PropostaRepository propostaRepository;
    private final PropostaEventProducer eventProducer;
    private final CalculoParcelaStrategyFactory strategyFactory;
    private final PropostaResponseMapper propostaResponseMapper;
    private final Counter propostasCriadasCounter;

    public CriarPropostaCommandHandler(
            DividaRepository dividaRepository,
            PropostaRepository propostaRepository,
            PropostaEventProducer eventProducer,
            CalculoParcelaStrategyFactory strategyFactory,
            PropostaResponseMapper propostaResponseMapper,
            MeterRegistry meterRegistry) {
        this.dividaRepository = dividaRepository;
        this.propostaRepository = propostaRepository;
        this.eventProducer = eventProducer;
        this.strategyFactory = strategyFactory;
        this.propostaResponseMapper = propostaResponseMapper;
        this.propostasCriadasCounter = Counter.builder("propostas_criadas_total")
                .description("Total de propostas criadas com sucesso")
                .register(meterRegistry);
    }

    /**
     * Processa o comando de criação de proposta.
     * <p>
     * Valida a elegibilidade dos contratos informados, calcula os valores usando a estratégia
     * correspondente ao tipo de acordo e persiste a proposta com suas parcelas geradas.
     *
     * @param command comando contendo os dados para criação da proposta
     * @return {@link PropostaResponse} com os dados da proposta criada
     * @throws DividaInelegivelException se algum contrato informado não for elegível
     */
    public PropostaResponse handle(CriarPropostaCommand command) {
        log.info("Criando proposta para cpfCnpj={}", maskCpfCnpj(command.cpfCnpj()));

        List<Divida> todasDividas = dividaRepository.buscarDividasElegiveis(command.cpfCnpj());

        Set<String> contratosElegiveis = todasDividas.stream()
                .filter(Divida::isElegivel)
                .map(Divida::getContrato)
                .collect(Collectors.toSet());

        List<String> contratosInelegiveis = command.contratosIds().stream()
                .filter(id -> !contratosElegiveis.contains(id))
                .toList();

        if (!contratosInelegiveis.isEmpty()) {
            throw new DividaInelegivelException(contratosInelegiveis);
        }

        Set<String> contratosDesejados = Set.copyOf(command.contratosIds());
        List<Divida> dividasFiltradas = todasDividas.stream()
                .filter(d -> contratosDesejados.contains(d.getContrato()) && d.isElegivel())
                .toList();

        CalculoParcelaStrategy strategy = strategyFactory.getStrategy(command.tipoAcordo());

        Proposta proposta = Proposta.criar(
                command.cpfCnpj(), dividasFiltradas, command.numeroParcelas(), strategy);

        proposta = propostaRepository.salvar(proposta);

        proposta.pullDomainEvents().forEach(eventProducer::publicar);

        propostasCriadasCounter.increment();

        log.info("Proposta criada com sucesso. propostaId={}, cpfCnpj={}",
                proposta.getId(), maskCpfCnpj(proposta.getCpfCnpj()));

        return propostaResponseMapper.toResponse(proposta);
    }

    private String maskCpfCnpj(String cpfCnpj) {
        if (cpfCnpj == null || cpfCnpj.length() < 16) {
            return "***";
        }
        return cpfCnpj.substring(0, 14) + "**";
    }
}
