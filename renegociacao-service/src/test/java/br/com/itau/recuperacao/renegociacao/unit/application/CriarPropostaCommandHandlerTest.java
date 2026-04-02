package br.com.itau.recuperacao.renegociacao.unit.application;

import br.com.itau.recuperacao.renegociacao.api.dto.response.PropostaResponse;
import br.com.itau.recuperacao.renegociacao.api.exception.DividaInelegivelException;
import br.com.itau.recuperacao.renegociacao.application.command.CriarPropostaCommand;
import br.com.itau.recuperacao.renegociacao.application.handler.CriarPropostaCommandHandler;
import br.com.itau.recuperacao.renegociacao.domain.event.DomainEvent;
import br.com.itau.recuperacao.renegociacao.domain.factory.CalculoParcelaStrategyFactory;
import br.com.itau.recuperacao.renegociacao.domain.model.Divida;
import br.com.itau.recuperacao.renegociacao.domain.model.Proposta;
import br.com.itau.recuperacao.renegociacao.domain.model.enums.PropostaStatus;
import br.com.itau.recuperacao.renegociacao.domain.model.enums.TipoAcordo;
import br.com.itau.recuperacao.renegociacao.domain.repository.DividaRepository;
import br.com.itau.recuperacao.renegociacao.domain.repository.PropostaRepository;
import br.com.itau.recuperacao.renegociacao.application.mapper.PropostaResponseMapper;
import br.com.itau.recuperacao.renegociacao.domain.strategy.CalculoSemJuros;
import br.com.itau.recuperacao.renegociacao.infrastructure.messaging.producer.PropostaEventProducer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
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
class CriarPropostaCommandHandlerTest {

    @Mock
    private DividaRepository dividaRepository;

    @Mock
    private PropostaRepository propostaRepository;

    @Mock
    private PropostaEventProducer eventProducer;

    @Mock
    private CalculoParcelaStrategyFactory strategyFactory;

    private CriarPropostaCommandHandler handler;

    @Captor
    private ArgumentCaptor<Proposta> propostaCaptor;

    @BeforeEach
    void setUp() {
        handler = new CriarPropostaCommandHandler(
                dividaRepository,
                propostaRepository,
                eventProducer,
                strategyFactory,
                new PropostaResponseMapper(),
                new SimpleMeterRegistry());
    }

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
    @DisplayName("Deve criar proposta com dívidas elegíveis e publicar evento")
    void handle_comDividasElegiveis_devePersistirEPublicarEvento() {
        String cpfCnpj = "12.345.678/0001-99";
        List<String> contratos = List.of("CTR-001", "CTR-002");
        List<Divida> dividas = criarDividasElegiveis();
        CalculoSemJuros strategy = new CalculoSemJuros();

        CriarPropostaCommand command = new CriarPropostaCommand(
                cpfCnpj, contratos, 10, TipoAcordo.SEM_JUROS);

        when(dividaRepository.buscarDividasElegiveis(cpfCnpj)).thenReturn(dividas);
        when(strategyFactory.getStrategy(TipoAcordo.SEM_JUROS)).thenReturn(strategy);
        when(propostaRepository.salvar(any(Proposta.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PropostaResponse response = handler.handle(command);

        assertNotNull(response);
        assertNotNull(response.id());
        assertEquals(cpfCnpj, response.cpfCnpj());
        assertEquals(PropostaStatus.PENDENTE, response.status());
        assertEquals(new BigDecimal("10000.00"), response.valorTotal());
        assertEquals(new BigDecimal("8500.00"), response.valorNegociado());
        assertEquals(10, response.numeroParcelas());

        verify(propostaRepository).salvar(propostaCaptor.capture());
        Proposta propostaSalva = propostaCaptor.getValue();
        assertNotNull(propostaSalva.getId());
        assertEquals(cpfCnpj, propostaSalva.getCpfCnpj());

        verify(eventProducer).publicar(any(DomainEvent.class));
    }

    @Test
    @DisplayName("Deve lançar exceção quando dívida é inelegível")
    void handle_comDividaInelegivel_deveLancarDividaInelegivelException() {
        String cpfCnpj = "12.345.678/0001-99";
        List<String> contratos = List.of("CTR-001");

        List<Divida> dividasInelegiveis = List.of(
                Divida.builder()
                        .id(UUID.randomUUID())
                        .contrato("CTR-001")
                        .valorOriginal(new BigDecimal("5000.00"))
                        .valorAtualizado(new BigDecimal("5500.00"))
                        .dataVencimento(LocalDate.now().minusDays(15))
                        .diasAtraso(15)
                        .produto("Capital de Giro")
                        .elegivel(false)
                        .build()
        );

        CriarPropostaCommand command = new CriarPropostaCommand(
                cpfCnpj, contratos, 10, TipoAcordo.SEM_JUROS);

        when(dividaRepository.buscarDividasElegiveis(cpfCnpj)).thenReturn(dividasInelegiveis);

        assertThrows(DividaInelegivelException.class, () -> handler.handle(command));

        verify(propostaRepository, never()).salvar(any());
        verify(eventProducer, never()).publicar(any());
    }

    @Test
    @DisplayName("Deve lançar exceção quando contrato não encontrado nas dívidas")
    void handle_comContratoInexistente_deveLancarDividaInelegivelException() {
        String cpfCnpj = "12.345.678/0001-99";
        List<String> contratos = List.of("CTR-INEXISTENTE");

        CriarPropostaCommand command = new CriarPropostaCommand(
                cpfCnpj, contratos, 10, TipoAcordo.SEM_JUROS);

        when(dividaRepository.buscarDividasElegiveis(cpfCnpj)).thenReturn(Collections.emptyList());

        assertThrows(DividaInelegivelException.class, () -> handler.handle(command));

        verify(propostaRepository, never()).salvar(any());
        verify(eventProducer, never()).publicar(any());
    }
}
