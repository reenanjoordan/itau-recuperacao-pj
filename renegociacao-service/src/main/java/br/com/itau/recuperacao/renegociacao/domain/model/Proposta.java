package br.com.itau.recuperacao.renegociacao.domain.model;

import br.com.itau.recuperacao.renegociacao.domain.event.DomainEvent;
import br.com.itau.recuperacao.renegociacao.domain.event.PropostaCanceladaEvent;
import br.com.itau.recuperacao.renegociacao.domain.event.PropostaCriadaEvent;
import br.com.itau.recuperacao.renegociacao.domain.event.PropostaEfetivadaEvent;
import br.com.itau.recuperacao.renegociacao.domain.model.enums.PropostaStatus;
import br.com.itau.recuperacao.renegociacao.domain.model.enums.TipoAcordo;
import br.com.itau.recuperacao.renegociacao.domain.strategy.CalculoParcelaStrategy;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

/**
 * Entidade de domínio rica que representa uma proposta de renegociação de dívidas PJ.
 * <p>
 * Mudanças de estado são realizadas exclusivamente através de métodos de domínio,
 * garantindo a consistência das regras de negócio. Não utiliza {@code @Data} nem
 * {@code @Setter} — toda alteração ocorre via comportamento explícito.
 */
@Getter
@ToString(exclude = "domainEvents")
@EqualsAndHashCode(of = "id")
public class Proposta {

    private UUID id;
    private String cpfCnpj;
    private PropostaStatus status;
    private BigDecimal valorTotal;
    private BigDecimal valorNegociado;
    private BigDecimal percentualDesconto;
    private Integer numeroParcelas;
    private BigDecimal valorParcela;
    private TipoAcordo tipoAcordo;
    private List<Divida> dividas;
    private List<Parcela> parcelas;

    @Getter(AccessLevel.NONE)
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    private LocalDateTime criadaEm;
    private LocalDateTime atualizadaEm;
    private Long versao;

    /**
     * Cria uma nova proposta de renegociação com base nas dívidas informadas e na estratégia de cálculo.
     * <p>
     * Realiza todas as validações de entrada, calcula valores negociados, gera parcelas sequenciais
     * com vencimento mensal e registra o evento {@link PropostaCriadaEvent}.
     *
     * @param cpfCnpj        CPF ou CNPJ do cliente
     * @param dividas        lista de dívidas a serem renegociadas (não pode ser vazia)
     * @param numeroParcelas número de parcelas desejado (entre 1 e 60)
     * @param strategy       estratégia de cálculo de parcelas a ser aplicada
     * @return nova instância de {@link Proposta} com status {@link PropostaStatus#PENDENTE}
     * @throws IllegalArgumentException se qualquer parâmetro for inválido
     */
    public static Proposta criar(String cpfCnpj, List<Divida> dividas, Integer numeroParcelas,
                                 CalculoParcelaStrategy strategy) {
        if (cpfCnpj == null || cpfCnpj.isBlank()) {
            throw new IllegalArgumentException("CPF/CNPJ é obrigatório");
        }
        if (numeroParcelas == null || numeroParcelas < 1 || numeroParcelas > 60) {
            throw new IllegalArgumentException("Número de parcelas deve estar entre 1 e 60");
        }
        if (dividas == null || dividas.isEmpty()) {
            throw new IllegalArgumentException("É necessário informar ao menos uma dívida");
        }

        Proposta proposta = new Proposta();
        proposta.id = UUID.randomUUID();
        proposta.cpfCnpj = cpfCnpj;
        proposta.dividas = new ArrayList<>(dividas);
        proposta.numeroParcelas = numeroParcelas;
        proposta.tipoAcordo = strategy.getTipoAcordo();
        proposta.status = PropostaStatus.PENDENTE;
        proposta.criadaEm = LocalDateTime.now();

        proposta.valorTotal = dividas.stream()
                .map(Divida::getValorAtualizado)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        proposta.valorNegociado = strategy.calcularValorNegociado(proposta.valorTotal, numeroParcelas);
        proposta.valorParcela = strategy.calcularValorParcela(proposta.valorTotal, numeroParcelas);

        if (proposta.valorNegociado.compareTo(proposta.valorTotal) >= 0) {
            proposta.percentualDesconto = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        } else {
            proposta.percentualDesconto = BigDecimal.ONE
                    .subtract(proposta.valorNegociado.divide(proposta.valorTotal, 10, RoundingMode.HALF_UP))
                    .multiply(new BigDecimal("100"))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        LocalDate hoje = LocalDate.now();
        proposta.parcelas = IntStream.rangeClosed(1, numeroParcelas)
                .mapToObj(i -> Parcela.builder()
                        .id(UUID.randomUUID())
                        .numeroParcela(i)
                        .valor(proposta.valorParcela)
                        .dataVencimento(hoje.plusMonths(i))
                        .status("PENDENTE")
                        .build())
                .toList();

        proposta.domainEvents.add(new PropostaCriadaEvent(
                proposta.id, cpfCnpj, proposta.valorTotal,
                numeroParcelas, proposta.tipoAcordo, proposta.criadaEm));

        return proposta;
    }

    /**
     * Efetiva a proposta de renegociação, alterando o status para {@link PropostaStatus#EFETIVADA}.
     * <p>
     * Registra o evento {@link PropostaEfetivadaEvent} no agregado.
     *
     * @throws IllegalStateException se a proposta não estiver em status {@link PropostaStatus#PENDENTE}
     */
    public void efetivar() {
        if (!isEfetivavel()) {
            throw new IllegalStateException("Proposta não pode ser efetivada. Status atual: " + status);
        }
        this.status = PropostaStatus.EFETIVADA;
        this.atualizadaEm = LocalDateTime.now();
        domainEvents.add(new PropostaEfetivadaEvent(
                id, cpfCnpj, valorNegociado, numeroParcelas, valorParcela, LocalDateTime.now()));
    }

    /**
     * Cancela a proposta de renegociação com um motivo informado.
     * <p>
     * Registra o evento {@link PropostaCanceladaEvent} no agregado.
     *
     * @param motivo motivo do cancelamento
     * @throws IllegalStateException se a proposta já estiver em status terminal
     */
    public void cancelar(String motivo) {
        if (status.isTerminal()) {
            throw new IllegalStateException("Proposta em status terminal não pode ser cancelada: " + status);
        }
        this.status = PropostaStatus.CANCELADA;
        this.atualizadaEm = LocalDateTime.now();
        domainEvents.add(new PropostaCanceladaEvent(id, cpfCnpj, motivo, LocalDateTime.now()));
    }

    /**
     * Verifica se a proposta pode ser efetivada.
     *
     * @return {@code true} se o status atual for {@link PropostaStatus#PENDENTE}
     */
    public boolean isEfetivavel() {
        return status == PropostaStatus.PENDENTE;
    }

    /**
     * Calcula o valor total das dívidas agrupadas na proposta.
     *
     * @return soma dos valores atualizados de todas as dívidas
     */
    public BigDecimal calcularValorTotal() {
        return dividas.stream()
                .map(Divida::getValorAtualizado)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Retorna e limpa a lista de eventos de domínio pendentes.
     * <p>
     * Após a chamada, a lista interna de eventos fica vazia.
     *
     * @return lista de eventos de domínio registrados desde a última chamada
     */
    public List<DomainEvent> pullDomainEvents() {
        List<DomainEvent> events = new ArrayList<>(domainEvents);
        domainEvents.clear();
        return events;
    }
}
