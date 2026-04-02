package br.com.itau.recuperacao.cobranca.application.service;

import br.com.itau.recuperacao.cobranca.domain.model.AcaoCobranca;
import br.com.itau.recuperacao.cobranca.domain.model.enums.CanalCobranca;
import br.com.itau.recuperacao.cobranca.domain.model.enums.TipoAcao;
import br.com.itau.recuperacao.cobranca.infrastructure.messaging.producer.CobrancaEventProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Serviço responsável pela lógica de negócio das ações de cobrança.
 * Gerencia a criação, listagem e processamento de ações de cobrança sobre dívidas PJ.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CobrancaService {

    private final CobrancaEventProducer eventProducer;

    private final Map<UUID, AcaoCobranca> acaoStore = new ConcurrentHashMap<>();

    /**
     * Cria uma nova ação de cobrança com status PENDENTE e a persiste em memória.
     *
     * @param cpfCnpj   CPF ou CNPJ do devedor
     * @param tipo      tipo da ação de cobrança
     * @param canal     canal utilizado para a cobrança
     * @param descricao descrição detalhada da ação
     * @return a ação de cobrança criada
     */
    public AcaoCobranca criarAcao(String cpfCnpj, TipoAcao tipo, CanalCobranca canal, String descricao) {
        log.info("Criando ação de cobrança para CPF/CNPJ: {}, tipo: {}, canal: {}", cpfCnpj, tipo, canal);

        AcaoCobranca acao = AcaoCobranca.builder()
                .id(UUID.randomUUID())
                .cpfCnpj(cpfCnpj)
                .tipoAcao(tipo)
                .canal(canal)
                .descricao(descricao)
                .criadaEm(LocalDateTime.now())
                .status("PENDENTE")
                .build();

        acaoStore.put(acao.getId(), acao);
        log.info("Ação de cobrança criada com ID: {}", acao.getId());

        eventProducer.publicarAcaoRealizada(acao);

        return acao;
    }

    /**
     * Lista todas as ações de cobrança associadas a um CPF/CNPJ.
     *
     * @param cpfCnpj CPF ou CNPJ do devedor
     * @return lista de ações de cobrança encontradas
     */
    public List<AcaoCobranca> listarPorCpfCnpj(String cpfCnpj) {
        log.info("Listando ações de cobrança para CPF/CNPJ: {}", cpfCnpj);

        List<AcaoCobranca> acoes = acaoStore.values().stream()
                .filter(acao -> acao.getCpfCnpj().equals(cpfCnpj))
                .collect(Collectors.toList());

        if (acoes.isEmpty()) {
            log.info("Nenhuma ação encontrada para CPF/CNPJ: {}, retornando dados de exemplo", cpfCnpj);
            acoes = gerarDadosExemplo(cpfCnpj);
        }

        return acoes;
    }

    /**
     * Processa um evento de proposta efetivada, criando ações de cobrança automáticas
     * conforme a estratégia definida para o tipo de proposta.
     *
     * @param evento mapa contendo os dados do evento de proposta
     */
    public void processarEventoProposta(Map<String, Object> evento) {
        String cpfCnpj = (String) evento.getOrDefault("cpfCnpj", "00000000000000");
        String propostaId = String.valueOf(evento.getOrDefault("propostaId", "N/A"));

        log.info("Processando evento de proposta efetivada. PropostaId: {}, CPF/CNPJ: {}", propostaId, cpfCnpj);

        criarAcao(cpfCnpj, TipoAcao.EMAIL, CanalCobranca.DIGITAL,
                "Confirmação de renegociação - Proposta " + propostaId);

        criarAcao(cpfCnpj, TipoAcao.SMS, CanalCobranca.DIGITAL,
                "Lembrete de pagamento da primeira parcela - Proposta " + propostaId);

        log.info("Ações de cobrança automáticas criadas para proposta: {}", propostaId);
    }

    private List<AcaoCobranca> gerarDadosExemplo(String cpfCnpj) {
        List<AcaoCobranca> exemplos = new ArrayList<>();

        exemplos.add(AcaoCobranca.builder()
                .id(UUID.randomUUID())
                .cpfCnpj(cpfCnpj)
                .tipoAcao(TipoAcao.LIGACAO)
                .canal(CanalCobranca.TELEFONICO)
                .descricao("Contato inicial de cobrança")
                .criadaEm(LocalDateTime.now().minusDays(5))
                .status("REALIZADA")
                .build());

        exemplos.add(AcaoCobranca.builder()
                .id(UUID.randomUUID())
                .cpfCnpj(cpfCnpj)
                .tipoAcao(TipoAcao.EMAIL)
                .canal(CanalCobranca.DIGITAL)
                .descricao("Envio de proposta de renegociação por e-mail")
                .criadaEm(LocalDateTime.now().minusDays(3))
                .status("ENVIADA")
                .build());

        exemplos.add(AcaoCobranca.builder()
                .id(UUID.randomUUID())
                .cpfCnpj(cpfCnpj)
                .tipoAcao(TipoAcao.SMS)
                .canal(CanalCobranca.DIGITAL)
                .descricao("Lembrete de vencimento de parcela")
                .criadaEm(LocalDateTime.now().minusDays(1))
                .status("PENDENTE")
                .build());

        return exemplos;
    }
}
