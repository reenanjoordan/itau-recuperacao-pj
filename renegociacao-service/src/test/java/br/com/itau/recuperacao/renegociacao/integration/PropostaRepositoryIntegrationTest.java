package br.com.itau.recuperacao.renegociacao.integration;

import br.com.itau.recuperacao.renegociacao.domain.model.enums.PropostaStatus;
import br.com.itau.recuperacao.renegociacao.domain.model.enums.TipoAcordo;
import br.com.itau.recuperacao.renegociacao.infrastructure.acl.LegadoAclClient;
import br.com.itau.recuperacao.renegociacao.infrastructure.persistence.entity.DividaEntity;
import br.com.itau.recuperacao.renegociacao.infrastructure.persistence.entity.ParcelaEntity;
import br.com.itau.recuperacao.renegociacao.infrastructure.persistence.entity.PropostaEntity;
import br.com.itau.recuperacao.renegociacao.infrastructure.persistence.jpa.PropostaJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("integration")
class PropostaRepositoryIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private PropostaJpaRepository propostaJpaRepository;

    @MockBean
    private LegadoAclClient legadoAclClient;

    private static final String CPF_CNPJ = "12345678901234";

    @BeforeEach
    void setUp() {
        propostaJpaRepository.deleteAll();
    }

    private PropostaEntity criarPropostaEntity(String cpfCnpj, PropostaStatus status) {
        PropostaEntity proposta = PropostaEntity.builder()
                .id(UUID.randomUUID())
                .cpfCnpj(cpfCnpj)
                .status(status)
                .valorTotal(new BigDecimal("10000.00"))
                .valorNegociado(new BigDecimal("8500.00"))
                .percentualDesconto(new BigDecimal("15.00"))
                .numeroParcelas(10)
                .valorParcela(new BigDecimal("850.00"))
                .tipoAcordo(TipoAcordo.SEM_JUROS)
                .criadaEm(LocalDateTime.now())
                .build();

        return proposta;
    }

    @Test
    @DisplayName("Deve salvar e buscar proposta por ID")
    void salvar_devePersistirEEncontrarPorId() {
        PropostaEntity proposta = criarPropostaEntity(CPF_CNPJ, PropostaStatus.PENDENTE);

        PropostaEntity salva = propostaJpaRepository.save(proposta);

        assertNotNull(salva.getId());
        assertNotNull(salva.getVersao());

        Optional<PropostaEntity> encontrada = propostaJpaRepository.findById(salva.getId());

        assertTrue(encontrada.isPresent());
        assertEquals(CPF_CNPJ, encontrada.get().getCpfCnpj());
        assertEquals(PropostaStatus.PENDENTE, encontrada.get().getStatus());
        assertEquals(new BigDecimal("10000.00"), encontrada.get().getValorTotal());
        assertEquals(new BigDecimal("8500.00"), encontrada.get().getValorNegociado());
        assertEquals(10, encontrada.get().getNumeroParcelas());
        assertEquals(TipoAcordo.SEM_JUROS, encontrada.get().getTipoAcordo());
    }

    @Test
    @DisplayName("Deve buscar propostas por CPF/CNPJ")
    void buscarPorCpfCnpj_deveRetornarPropostasDoCliente() {
        PropostaEntity proposta1 = criarPropostaEntity(CPF_CNPJ, PropostaStatus.PENDENTE);
        PropostaEntity proposta2 = criarPropostaEntity(CPF_CNPJ, PropostaStatus.EFETIVADA);
        PropostaEntity propostaOutroCliente = criarPropostaEntity("99999999999999", PropostaStatus.PENDENTE);

        propostaJpaRepository.saveAll(List.of(proposta1, proposta2, propostaOutroCliente));

        List<PropostaEntity> resultado = propostaJpaRepository.findByCpfCnpj(CPF_CNPJ);

        assertEquals(2, resultado.size());
        assertTrue(resultado.stream().allMatch(p -> CPF_CNPJ.equals(p.getCpfCnpj())));
    }

    @Test
    @DisplayName("Deve retornar lista vazia para CPF/CNPJ sem propostas")
    void buscarPorCpfCnpj_semPropostas_deveRetornarListaVazia() {
        List<PropostaEntity> resultado = propostaJpaRepository.findByCpfCnpj("00000000000000");

        assertNotNull(resultado);
        assertTrue(resultado.isEmpty());
    }

    @Test
    @Transactional
    @DisplayName("Deve salvar proposta com dívidas e parcelas em cascata")
    void salvar_comDividasEParcelas_devePersistirEmCascata() {
        PropostaEntity proposta = criarPropostaEntity(CPF_CNPJ, PropostaStatus.PENDENTE);

        DividaEntity divida1 = DividaEntity.builder()
                .id(UUID.randomUUID())
                .proposta(proposta)
                .contrato("CTR-001")
                .valorOriginal(new BigDecimal("5000.00"))
                .valorAtualizado(new BigDecimal("6000.00"))
                .dataVencimento(LocalDate.now().minusMonths(6))
                .diasAtraso(180)
                .produto("Capital de Giro")
                .build();

        DividaEntity divida2 = DividaEntity.builder()
                .id(UUID.randomUUID())
                .proposta(proposta)
                .contrato("CTR-002")
                .valorOriginal(new BigDecimal("3000.00"))
                .valorAtualizado(new BigDecimal("4000.00"))
                .dataVencimento(LocalDate.now().minusMonths(3))
                .diasAtraso(90)
                .produto("Cheque Especial")
                .build();

        proposta.setDividas(new ArrayList<>(List.of(divida1, divida2)));

        ParcelaEntity parcela1 = ParcelaEntity.builder()
                .id(UUID.randomUUID())
                .proposta(proposta)
                .numeroParcela(1)
                .valor(new BigDecimal("850.00"))
                .dataVencimento(LocalDate.now().plusMonths(1))
                .status("PENDENTE")
                .build();

        ParcelaEntity parcela2 = ParcelaEntity.builder()
                .id(UUID.randomUUID())
                .proposta(proposta)
                .numeroParcela(2)
                .valor(new BigDecimal("850.00"))
                .dataVencimento(LocalDate.now().plusMonths(2))
                .status("PENDENTE")
                .build();

        proposta.setParcelas(new ArrayList<>(List.of(parcela1, parcela2)));

        PropostaEntity salva = propostaJpaRepository.save(proposta);

        Optional<PropostaEntity> encontrada = propostaJpaRepository.findById(salva.getId());

        assertTrue(encontrada.isPresent());
        assertEquals(2, encontrada.get().getDividas().size());
        assertEquals(2, encontrada.get().getParcelas().size());

        DividaEntity dividaSalva = encontrada.get().getDividas().stream()
                .filter(d -> "CTR-001".equals(d.getContrato()))
                .findFirst().orElse(null);
        assertNotNull(dividaSalva);
        assertEquals(new BigDecimal("6000.00"), dividaSalva.getValorAtualizado());
        assertEquals(180, dividaSalva.getDiasAtraso());

        ParcelaEntity parcelaSalva = encontrada.get().getParcelas().stream()
                .filter(p -> p.getNumeroParcela() == 1)
                .findFirst().orElse(null);
        assertNotNull(parcelaSalva);
        assertEquals(new BigDecimal("850.00"), parcelaSalva.getValor());
        assertEquals("PENDENTE", parcelaSalva.getStatus());
    }

    @Test
    @DisplayName("Deve atualizar status da proposta")
    void atualizar_statusDaProposta_devePersistirAlteracao() {
        PropostaEntity proposta = criarPropostaEntity(CPF_CNPJ, PropostaStatus.PENDENTE);
        PropostaEntity salva = propostaJpaRepository.save(proposta);

        salva.setStatus(PropostaStatus.EFETIVADA);
        salva.setAtualizadaEm(LocalDateTime.now());
        propostaJpaRepository.save(salva);

        Optional<PropostaEntity> atualizada = propostaJpaRepository.findById(salva.getId());

        assertTrue(atualizada.isPresent());
        assertEquals(PropostaStatus.EFETIVADA, atualizada.get().getStatus());
        assertNotNull(atualizada.get().getAtualizadaEm());
    }

    @Test
    @DisplayName("Deve deletar proposta e entidades filhas em cascata")
    void deletar_deveFuncionar() {
        PropostaEntity proposta = criarPropostaEntity(CPF_CNPJ, PropostaStatus.CANCELADA);

        ParcelaEntity parcela = ParcelaEntity.builder()
                .id(UUID.randomUUID())
                .proposta(proposta)
                .numeroParcela(1)
                .valor(new BigDecimal("850.00"))
                .dataVencimento(LocalDate.now().plusMonths(1))
                .status("PENDENTE")
                .build();
        proposta.setParcelas(new ArrayList<>(List.of(parcela)));

        PropostaEntity salva = propostaJpaRepository.save(proposta);

        propostaJpaRepository.deleteById(salva.getId());

        assertFalse(propostaJpaRepository.findById(salva.getId()).isPresent());
    }
}
