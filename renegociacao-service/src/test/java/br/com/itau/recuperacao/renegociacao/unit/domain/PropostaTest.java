package br.com.itau.recuperacao.renegociacao.unit.domain;

import br.com.itau.recuperacao.renegociacao.domain.event.DomainEvent;
import br.com.itau.recuperacao.renegociacao.domain.event.PropostaCanceladaEvent;
import br.com.itau.recuperacao.renegociacao.domain.event.PropostaCriadaEvent;
import br.com.itau.recuperacao.renegociacao.domain.event.PropostaEfetivadaEvent;
import br.com.itau.recuperacao.renegociacao.domain.model.Divida;
import br.com.itau.recuperacao.renegociacao.domain.model.Proposta;
import br.com.itau.recuperacao.renegociacao.domain.model.enums.PropostaStatus;
import br.com.itau.recuperacao.renegociacao.domain.model.enums.TipoAcordo;
import br.com.itau.recuperacao.renegociacao.domain.strategy.CalculoSemJuros;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("unit")
class PropostaTest {

    private final CalculoSemJuros calculoSemJuros = new CalculoSemJuros();

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
    @DisplayName("Deve criar proposta com status PENDENTE e calcular valores corretamente")
    void criar_comDadosValidos_deveRetornarPropostaComStatusPendente() {
        List<Divida> dividas = criarDividasElegiveis();
        int numeroParcelas = 10;

        Proposta proposta = Proposta.criar("12345678901234", dividas, numeroParcelas, calculoSemJuros);

        assertNotNull(proposta.getId());
        assertEquals(PropostaStatus.PENDENTE, proposta.getStatus());
        assertEquals("12345678901234", proposta.getCpfCnpj());
        assertEquals(new BigDecimal("10000.00"), proposta.getValorTotal());
        assertEquals(new BigDecimal("8500.00"), proposta.getValorNegociado());
        assertEquals(new BigDecimal("850.00"), proposta.getValorParcela());
        assertEquals(numeroParcelas, proposta.getNumeroParcelas());
        assertEquals(TipoAcordo.SEM_JUROS, proposta.getTipoAcordo());
        assertEquals(numeroParcelas, proposta.getParcelas().size());
        assertNotNull(proposta.getCriadaEm());

        List<DomainEvent> events = proposta.pullDomainEvents();
        assertEquals(1, events.size());
        assertTrue(events.get(0) instanceof PropostaCriadaEvent);
    }

    @Test
    @DisplayName("Deve efetivar proposta pendente com sucesso")
    void efetivar_propostaPendente_deveAlterarStatusParaEfetivada() {
        Proposta proposta = Proposta.criar("12345678901234", criarDividasElegiveis(), 10, calculoSemJuros);
        proposta.pullDomainEvents();

        proposta.efetivar();

        assertEquals(PropostaStatus.EFETIVADA, proposta.getStatus());
        assertNotNull(proposta.getAtualizadaEm());
        assertFalse(proposta.isEfetivavel());

        List<DomainEvent> events = proposta.pullDomainEvents();
        assertEquals(1, events.size());
        assertTrue(events.get(0) instanceof PropostaEfetivadaEvent);
    }

    @Test
    @DisplayName("Deve lançar exceção ao efetivar proposta já efetivada")
    void efetivar_propostaJaEfetivada_deveLancarIllegalStateException() {
        Proposta proposta = Proposta.criar("12345678901234", criarDividasElegiveis(), 10, calculoSemJuros);
        proposta.efetivar();

        IllegalStateException exception = assertThrows(IllegalStateException.class, proposta::efetivar);
        assertTrue(exception.getMessage().contains("EFETIVADA"));
    }

    @Test
    @DisplayName("Deve cancelar proposta pendente com sucesso")
    void cancelar_propostaPendente_deveAlterarStatusParaCancelada() {
        Proposta proposta = Proposta.criar("12345678901234", criarDividasElegiveis(), 10, calculoSemJuros);
        proposta.pullDomainEvents();

        proposta.cancelar("Cliente desistiu");

        assertEquals(PropostaStatus.CANCELADA, proposta.getStatus());
        assertNotNull(proposta.getAtualizadaEm());

        List<DomainEvent> events = proposta.pullDomainEvents();
        assertEquals(1, events.size());
        assertTrue(events.get(0) instanceof PropostaCanceladaEvent);
    }

    @Test
    @DisplayName("Deve lançar exceção ao cancelar proposta em status terminal")
    void cancelar_propostaEfetivada_deveLancarIllegalStateException() {
        Proposta proposta = Proposta.criar("12345678901234", criarDividasElegiveis(), 10, calculoSemJuros);
        proposta.efetivar();

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> proposta.cancelar("Tentativa inválida"));
        assertTrue(exception.getMessage().contains("terminal"));
    }

    @Test
    @DisplayName("Deve retornar e limpar eventos de domínio")
    void pullDomainEvents_deveRetornarEventosELimparLista() {
        Proposta proposta = Proposta.criar("12345678901234", criarDividasElegiveis(), 10, calculoSemJuros);

        List<DomainEvent> primeiraLeitura = proposta.pullDomainEvents();
        assertEquals(1, primeiraLeitura.size());

        List<DomainEvent> segundaLeitura = proposta.pullDomainEvents();
        assertTrue(segundaLeitura.isEmpty());
    }

    @Test
    @DisplayName("Deve lançar exceção para CPF/CNPJ nulo")
    void criar_comCpfCnpjNulo_deveLancarIllegalArgumentException() {
        List<Divida> dividas = criarDividasElegiveis();

        assertThrows(IllegalArgumentException.class,
                () -> Proposta.criar(null, dividas, 10, calculoSemJuros));
    }

    @Test
    @DisplayName("Deve lançar exceção para CPF/CNPJ vazio")
    void criar_comCpfCnpjVazio_deveLancarIllegalArgumentException() {
        List<Divida> dividas = criarDividasElegiveis();

        assertThrows(IllegalArgumentException.class,
                () -> Proposta.criar("  ", dividas, 10, calculoSemJuros));
    }

    @Test
    @DisplayName("Deve lançar exceção para número de parcelas inválido")
    void criar_comNumeroParcelasInvalido_deveLancarIllegalArgumentException() {
        List<Divida> dividas = criarDividasElegiveis();

        assertThrows(IllegalArgumentException.class,
                () -> Proposta.criar("12345678901234", dividas, 0, calculoSemJuros));

        assertThrows(IllegalArgumentException.class,
                () -> Proposta.criar("12345678901234", dividas, 61, calculoSemJuros));

        assertThrows(IllegalArgumentException.class,
                () -> Proposta.criar("12345678901234", dividas, null, calculoSemJuros));
    }

    @Test
    @DisplayName("Deve lançar exceção para lista de dívidas vazia")
    void criar_comListaDividasVazia_deveLancarIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> Proposta.criar("12345678901234", List.of(), 10, calculoSemJuros));
    }

    @Test
    @DisplayName("Deve lançar exceção para lista de dívidas nula")
    void criar_comListaDividasNula_deveLancarIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> Proposta.criar("12345678901234", null, 10, calculoSemJuros));
    }

    @Test
    @DisplayName("Deve calcular percentual de desconto corretamente para CalculoSemJuros")
    void criar_comCalculoSemJuros_deveCalcularPercentualDescontoCorreto() {
        Proposta proposta = Proposta.criar("12345678901234", criarDividasElegiveis(), 10, calculoSemJuros);

        assertEquals(new BigDecimal("15.00"), proposta.getPercentualDesconto());
    }

    @Test
    @DisplayName("Deve gerar parcelas com vencimento mensal sequencial")
    void criar_comDadosValidos_deveGerarParcelasComVencimentoMensal() {
        Proposta proposta = Proposta.criar("12345678901234", criarDividasElegiveis(), 3, calculoSemJuros);

        LocalDate hoje = LocalDate.now();
        assertEquals(3, proposta.getParcelas().size());
        assertEquals(hoje.plusMonths(1), proposta.getParcelas().get(0).getDataVencimento());
        assertEquals(hoje.plusMonths(2), proposta.getParcelas().get(1).getDataVencimento());
        assertEquals(hoje.plusMonths(3), proposta.getParcelas().get(2).getDataVencimento());
        assertEquals("PENDENTE", proposta.getParcelas().get(0).getStatus());
    }
}
