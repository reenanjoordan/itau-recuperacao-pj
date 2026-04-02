package br.com.itau.recuperacao.pagamento.application.service;

import br.com.itau.recuperacao.pagamento.domain.model.Boleto;
import br.com.itau.recuperacao.pagamento.domain.model.enums.BoletoStatus;
import br.com.itau.recuperacao.pagamento.infrastructure.messaging.producer.PagamentoConfirmadoProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Serviço responsável pela geração e gestão de boletos bancários
 * vinculados a propostas de renegociação de dívidas PJ.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BoletoService {

    private final PagamentoConfirmadoProducer pagamentoProducer;

    private final Map<UUID, Boleto> boletoStore = new ConcurrentHashMap<>();
    private final Random random = new Random();

    /**
     * Gera um boleto bancário para uma proposta de renegociação.
     * O valor da parcela é calculado dividindo o valor total pelo número de parcelas.
     * O código de barras é gerado aleatoriamente e o vencimento é definido para 30 dias.
     *
     * @param propostaId     identificador da proposta de renegociação
     * @param valor          valor total a ser parcelado
     * @param numeroParcelas número de parcelas do acordo
     * @return o boleto gerado com status GERADO
     */
    public Boleto gerarBoleto(UUID propostaId, BigDecimal valor, Integer numeroParcelas) {
        log.info("Gerando boleto para proposta: {}, valor: {}, parcelas: {}", propostaId, valor, numeroParcelas);

        BigDecimal valorParcela = valor.divide(BigDecimal.valueOf(numeroParcelas), 2, RoundingMode.HALF_UP);

        Boleto boleto = Boleto.builder()
                .id(UUID.randomUUID())
                .propostaId(propostaId)
                .codigoBarras(gerarCodigoBarras())
                .valor(valorParcela)
                .dataVencimento(LocalDate.now().plusDays(30))
                .status(BoletoStatus.GERADO)
                .criadoEm(LocalDateTime.now())
                .build();

        boletoStore.put(boleto.getId(), boleto);
        log.info("Boleto gerado com ID: {}, código de barras: {}, valor parcela: {}",
                boleto.getId(), boleto.getCodigoBarras(), valorParcela);

        return boleto;
    }

    /**
     * Busca um boleto pelo seu identificador único.
     *
     * @param id identificador UUID do boleto
     * @return Optional contendo o boleto se encontrado, ou vazio caso contrário
     */
    public Optional<Boleto> buscarPorId(UUID id) {
        log.info("Buscando boleto por ID: {}", id);
        return Optional.ofNullable(boletoStore.get(id));
    }

    /**
     * Processa um evento de proposta efetivada recebido via Kafka.
     * Extrai os dados da proposta, gera o boleto correspondente e publica
     * o evento de boleto gerado.
     *
     * @param evento mapa contendo os dados do evento de proposta efetivada
     */
    public void processarEventoProposta(Map<String, Object> evento) {
        String propostaIdStr = String.valueOf(evento.getOrDefault("propostaId", ""));
        Object valorObj = evento.getOrDefault("valorTotal", "0");
        Object parcelasObj = evento.getOrDefault("numeroParcelas", "1");

        log.info("Processando evento de proposta efetivada para geração de boleto. PropostaId: {}", propostaIdStr);

        try {
            UUID propostaId = UUID.fromString(propostaIdStr);
            BigDecimal valor = new BigDecimal(String.valueOf(valorObj));
            Integer numeroParcelas = Integer.parseInt(String.valueOf(parcelasObj));

            Boleto boleto = gerarBoleto(propostaId, valor, numeroParcelas);

            pagamentoProducer.publicarBoletoGerado(propostaId, boleto.getId());

            log.info("Boleto gerado e evento publicado com sucesso para proposta: {}", propostaId);
        } catch (IllegalArgumentException e) {
            log.error("Erro ao processar evento de proposta — dados inválidos: {}", e.getMessage(), e);
        }
    }

    private String gerarCodigoBarras() {
        StringBuilder sb = new StringBuilder();
        sb.append("23793.");
        for (int i = 0; i < 5; i++) {
            sb.append(String.format("%05d", random.nextInt(100000)));
            if (i < 4) sb.append(" ");
        }
        sb.append(" ");
        sb.append(String.format("%01d", random.nextInt(10)));
        sb.append(" ");
        sb.append(String.format("%014d", random.nextLong(100000000000000L)));
        return sb.toString();
    }
}
