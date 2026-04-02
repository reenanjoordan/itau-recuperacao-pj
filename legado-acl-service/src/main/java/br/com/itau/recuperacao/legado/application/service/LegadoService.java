package br.com.itau.recuperacao.legado.application.service;

import br.com.itau.recuperacao.legado.domain.model.ClienteLegado;
import br.com.itau.recuperacao.legado.domain.model.DividaLegado;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Serviço que simula a integração com o mainframe legado (COBOL/DB2/VSAM).
 * Atua como Anti-Corruption Layer, traduzindo os dados do formato legado
 * para o modelo de domínio dos microsserviços modernos.
 *
 * <p>Em produção, este serviço realizaria chamadas ao mainframe via
 * MQ Series, CICS Transaction Gateway ou API proprietária.</p>
 */
@Service
@Slf4j
public class LegadoService {

    /**
     * Busca as dívidas de um cliente no sistema legado pelo CPF/CNPJ.
     * Retorna dados mockados que simulam a resposta do mainframe,
     * incluindo dívidas elegíveis e inelegíveis para renegociação.
     *
     * <p>Critérios de elegibilidade: diasAtraso >= 30 e valorAtualizado > 100.</p>
     *
     * @param cpfCnpj CPF ou CNPJ do cliente
     * @return lista de dívidas encontradas no sistema legado
     */
    public List<DividaLegado> buscarDividas(String cpfCnpj) {
        log.info("Consultando dívidas no mainframe para CPF/CNPJ: {}", cpfCnpj);
        simulateMainframeDelay();

        List<DividaLegado> dividas = new ArrayList<>();

        dividas.add(DividaLegado.builder()
                .contrato("CTR-001")
                .valorOriginal(new BigDecimal("50000.00"))
                .valorAtualizado(new BigDecimal("62000.00"))
                .dataVencimento("2024-06-15")
                .diasAtraso(180)
                .produto("Empréstimo PJ")
                .build());

        dividas.add(DividaLegado.builder()
                .contrato("CTR-002")
                .valorOriginal(new BigDecimal("120000.00"))
                .valorAtualizado(new BigDecimal("145000.00"))
                .dataVencimento("2024-03-10")
                .diasAtraso(275)
                .produto("Capital de Giro")
                .build());

        dividas.add(DividaLegado.builder()
                .contrato("CTR-003")
                .valorOriginal(new BigDecimal("75.00"))
                .valorAtualizado(new BigDecimal("82.50"))
                .dataVencimento("2025-01-20")
                .diasAtraso(45)
                .produto("Tarifa Bancária")
                .build());

        dividas.add(DividaLegado.builder()
                .contrato("CTR-004")
                .valorOriginal(new BigDecimal("230000.00"))
                .valorAtualizado(new BigDecimal("289000.00"))
                .dataVencimento("2024-01-05")
                .diasAtraso(340)
                .produto("Financiamento de Equipamentos")
                .build());

        dividas.add(DividaLegado.builder()
                .contrato("CTR-005")
                .valorOriginal(new BigDecimal("5000.00"))
                .valorAtualizado(new BigDecimal("5200.00"))
                .dataVencimento("2025-11-01")
                .diasAtraso(10)
                .produto("Cheque Especial PJ")
                .build());

        log.info("Retornadas {} dívidas do mainframe para CPF/CNPJ: {}", dividas.size(), cpfCnpj);
        return dividas;
    }

    /**
     * Busca os dados cadastrais de um cliente no sistema legado pelo CPF/CNPJ.
     * Retorna dados mockados que simulam a consulta ao mainframe.
     *
     * @param cpfCnpj CPF ou CNPJ do cliente
     * @return Optional contendo os dados do cliente se encontrado
     */
    public Optional<ClienteLegado> buscarCliente(String cpfCnpj) {
        log.info("Consultando dados de cliente no mainframe para CPF/CNPJ: {}", cpfCnpj);
        simulateMainframeDelay();

        ClienteLegado cliente = ClienteLegado.builder()
                .cpfCnpj(cpfCnpj)
                .razaoSocial("Tech Solutions Ltda")
                .email("financeiro@techsolutions.com.br")
                .telefone("11987654321")
                .build();

        log.info("Dados do cliente retornados do mainframe: {}", cliente.getRazaoSocial());
        return Optional.of(cliente);
    }

    /**
     * Simula a latência de comunicação com o mainframe legado.
     * Em produção, a latência real seria de 200-500ms via MQ Series.
     */
    private void simulateMainframeDelay() {
        try {
            Thread.sleep(150);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Simulação de latência do mainframe interrompida");
        }
    }
}
