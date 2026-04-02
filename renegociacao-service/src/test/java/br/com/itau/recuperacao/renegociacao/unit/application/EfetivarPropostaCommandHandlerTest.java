package br.com.itau.recuperacao.renegociacao.unit.application;

import br.com.itau.recuperacao.renegociacao.api.dto.response.PropostaResponse;
import br.com.itau.recuperacao.renegociacao.api.exception.PropostaNaoEncontradaException;
import br.com.itau.recuperacao.renegociacao.application.command.EfetivarPropostaCommand;
import br.com.itau.recuperacao.renegociacao.application.handler.EfetivarPropostaCommandHandler;
import br.com.itau.recuperacao.renegociacao.domain.event.DomainEvent;
import br.com.itau.recuperacao.renegociacao.domain.model.Divida;
import br.com.itau.recuperacao.renegociacao.domain.model.Proposta;
import br.com.itau.recuperacao.renegociacao.domain.model.enums.PropostaStatus;
import br.com.itau.recuperacao.renegociacao.domain.repository.PropostaRepository;
import br.com.itau.recuperacao.renegociacao.application.mapper.PropostaResponseMapper;
import br.com.itau.recuperacao.renegociacao.domain.strategy.CalculoSemJuros;
import br.com.itau.recuperacao.renegociacao.infrastructure.cache.PropostaCacheService;
import br.com.itau.recuperacao.renegociacao.infrastructure.messaging.producer.PropostaEventProducer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class EfetivarPropostaCommandHandlerTest {

    @Mock
    private PropostaRepository propostaRepository;

    @Mock
    private PropostaEventProducer eventProducer;

    @Mock
    private PropostaCacheService cacheService;

    private EfetivarPropostaCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new EfetivarPropostaCommandHandler(
                propostaRepository,
                eventProducer,
                cacheService,
                new PropostaResponseMapper(),
                new SimpleMeterRegistry());
    }

    private Proposta criarPropostaPendente(String cpfCnpj) {
        List<Divida> dividas = List.of(
                Divida.builder()
                        .id(UUID.randomUUID())
                        .contrato("CTR-001")
                        .valorOriginal(new BigDecimal("5000.00"))
                        .valorAtualizado(new BigDecimal("6000.00"))
                        .dataVencimento(LocalDate.now().minusMonths(6))
                        .diasAtraso(180)
                        .produto("Capital de Giro")
                        .elegivel(true)
                        .build()
        );
        Proposta proposta = Proposta.criar(cpfCnpj, dividas, 10, new CalculoSemJuros());
        proposta.pullDomainEvents();
        return proposta;
    }

    @Test
    @DisplayName("Deve efetivar proposta pendente com sucesso")
    void handle_propostaPendente_deveEfetivarEPublicarEvento() {
        String cpfCnpj = "12.345.678/0001-99";
        Proposta proposta = criarPropostaPendente(cpfCnpj);
        UUID propostaId = proposta.getId();

        EfetivarPropostaCommand command = new EfetivarPropostaCommand(propostaId, cpfCnpj);

        when(propostaRepository.buscarPorId(propostaId)).thenReturn(Optional.of(proposta));
        when(propostaRepository.salvar(any(Proposta.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PropostaResponse response = handler.handle(command);

        assertNotNull(response);
        assertEquals(PropostaStatus.EFETIVADA, response.status());
        assertEquals(propostaId, response.id());

        verify(propostaRepository).salvar(any(Proposta.class));
        verify(cacheService).invalidar(propostaId);
        verify(eventProducer).publicar(any(DomainEvent.class));
    }

    @Test
    @DisplayName("Deve lançar exceção quando proposta não encontrada")
    void handle_propostaInexistente_deveLancarPropostaNaoEncontradaException() {
        UUID propostaId = UUID.randomUUID();
        EfetivarPropostaCommand command = new EfetivarPropostaCommand(propostaId, "12.345.678/0001-99");

        when(propostaRepository.buscarPorId(propostaId)).thenReturn(Optional.empty());

        assertThrows(PropostaNaoEncontradaException.class, () -> handler.handle(command));

        verify(propostaRepository, never()).salvar(any());
        verify(eventProducer, never()).publicar(any());
    }

    @Test
    @DisplayName("Deve lançar exceção quando CPF/CNPJ não confere")
    void handle_cpfCnpjDivergente_deveLancarIllegalStateException() {
        String cpfCnpjOriginal = "12.345.678/0001-99";
        String cpfCnpjDivergente = "99.999.999/0001-99";

        Proposta proposta = criarPropostaPendente(cpfCnpjOriginal);
        UUID propostaId = proposta.getId();

        EfetivarPropostaCommand command = new EfetivarPropostaCommand(propostaId, cpfCnpjDivergente);

        when(propostaRepository.buscarPorId(propostaId)).thenReturn(Optional.of(proposta));

        assertThrows(IllegalStateException.class, () -> handler.handle(command));

        verify(propostaRepository, never()).salvar(any());
        verify(eventProducer, never()).publicar(any());
    }

    @Test
    @DisplayName("Deve lançar exceção ao efetivar proposta já efetivada")
    void handle_propostaJaEfetivada_deveLancarIllegalStateException() {
        String cpfCnpj = "12.345.678/0001-99";
        Proposta proposta = criarPropostaPendente(cpfCnpj);
        proposta.efetivar();
        proposta.pullDomainEvents();

        UUID propostaId = proposta.getId();
        EfetivarPropostaCommand command = new EfetivarPropostaCommand(propostaId, cpfCnpj);

        when(propostaRepository.buscarPorId(propostaId)).thenReturn(Optional.of(proposta));

        assertThrows(IllegalStateException.class, () -> handler.handle(command));

        verify(propostaRepository, never()).salvar(any());
    }
}
