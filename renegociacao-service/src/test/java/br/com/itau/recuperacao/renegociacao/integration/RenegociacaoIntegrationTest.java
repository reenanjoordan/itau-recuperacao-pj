package br.com.itau.recuperacao.renegociacao.integration;

import br.com.itau.recuperacao.renegociacao.api.dto.request.EfetivacaoRequest;
import br.com.itau.recuperacao.renegociacao.api.dto.request.PropostaRequest;
import br.com.itau.recuperacao.renegociacao.api.dto.request.SimulacaoRequest;
import br.com.itau.recuperacao.renegociacao.api.dto.response.DividaResponse;
import br.com.itau.recuperacao.renegociacao.api.dto.response.PropostaResponse;
import br.com.itau.recuperacao.renegociacao.api.dto.response.SimulacaoResponse;
import br.com.itau.recuperacao.renegociacao.api.exception.ErrorResponse;
import br.com.itau.recuperacao.renegociacao.domain.model.Divida;
import br.com.itau.recuperacao.renegociacao.domain.model.enums.PropostaStatus;
import br.com.itau.recuperacao.renegociacao.domain.model.enums.TipoAcordo;
import br.com.itau.recuperacao.renegociacao.infrastructure.acl.LegadoAclClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@Tag("integration")
class RenegociacaoIntegrationTest extends AbstractIntegrationTest {

    private static final String BASE_URL = "/api/v1/renegociacao";
    private static final String CPF_CNPJ = "12.345.678/0001-99";

    @MockBean
    private LegadoAclClient legadoAclClient;

    private List<Divida> dividasElegiveis;

    @BeforeEach
    void setUp() {
        dividasElegiveis = List.of(
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

        when(legadoAclClient.buscarDividas(CPF_CNPJ)).thenReturn(dividasElegiveis);
    }

    @Test
    @DisplayName("Deve listar dívidas elegíveis com sucesso")
    void listarDividasElegiveis_deveRetornar200ComLista() {
        ResponseEntity<List<DividaResponse>> response = restTemplate.exchange(
                BASE_URL + "/dividas/" + CPF_CNPJ,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().size());
        assertEquals("CTR-001", response.getBody().get(0).contrato());
        assertTrue(response.getBody().get(0).elegivel());
    }

    @Test
    @DisplayName("Deve simular proposta com juros simples e retornar simulação válida")
    void simularProposta_comJurosSimples_deveRetornarSimulacaoValida() {
        SimulacaoRequest request = new SimulacaoRequest(
                CPF_CNPJ,
                List.of("CTR-001", "CTR-002"),
                List.of(12)
        );

        ResponseEntity<List<SimulacaoResponse>> response = restTemplate.exchange(
                BASE_URL + "/simular",
                HttpMethod.POST,
                new HttpEntity<>(request),
                new ParameterizedTypeReference<>() {}
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().size() >= 3);

        boolean temJurosSimples = response.getBody().stream()
                .anyMatch(s -> TipoAcordo.JUROS_SIMPLES.equals(s.tipoAcordo()));
        assertTrue(temJurosSimples);
    }

    @Test
    @DisplayName("Deve criar proposta e retornar 201 com Location header")
    void criarProposta_deveRetornar201ComLocationHeader() {
        PropostaRequest request = new PropostaRequest(
                CPF_CNPJ,
                List.of("CTR-001", "CTR-002"),
                10,
                TipoAcordo.SEM_JUROS
        );

        ResponseEntity<PropostaResponse> response = restTemplate.postForEntity(
                BASE_URL + "/proposta",
                request,
                PropostaResponse.class
        );

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().id());
        assertEquals(CPF_CNPJ, response.getBody().cpfCnpj());
        assertEquals(PropostaStatus.PENDENTE, response.getBody().status());
        assertEquals(new BigDecimal("10000.00"), response.getBody().valorTotal());
        assertEquals(10, response.getBody().numeroParcelas());
        assertNotNull(response.getHeaders().getLocation());
        assertTrue(response.getHeaders().getLocation().toString().contains("/proposta/"));
    }

    @Test
    @DisplayName("Deve efetivar proposta e retornar 200")
    void efetivarProposta_deveRetornar200EPublicarEventoKafka() {
        PropostaRequest criarRequest = new PropostaRequest(
                CPF_CNPJ, List.of("CTR-001", "CTR-002"), 10, TipoAcordo.SEM_JUROS);
        ResponseEntity<PropostaResponse> criadaResponse = restTemplate.postForEntity(
                BASE_URL + "/proposta", criarRequest, PropostaResponse.class);
        UUID propostaId = criadaResponse.getBody().id();

        EfetivacaoRequest efetivacaoRequest = new EfetivacaoRequest(CPF_CNPJ);
        ResponseEntity<PropostaResponse> response = restTemplate.postForEntity(
                BASE_URL + "/proposta/" + propostaId + "/efetivar",
                efetivacaoRequest,
                PropostaResponse.class
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(PropostaStatus.EFETIVADA, response.getBody().status());
        assertEquals(propostaId, response.getBody().id());
    }

    @Test
    @DisplayName("Deve retornar 409 ao efetivar proposta já efetivada")
    void efetivarProposta_jaEfetivada_deveRetornar409() {
        PropostaRequest criarRequest = new PropostaRequest(
                CPF_CNPJ, List.of("CTR-001", "CTR-002"), 10, TipoAcordo.SEM_JUROS);
        ResponseEntity<PropostaResponse> criadaResponse = restTemplate.postForEntity(
                BASE_URL + "/proposta", criarRequest, PropostaResponse.class);
        UUID propostaId = criadaResponse.getBody().id();

        EfetivacaoRequest efetivacaoRequest = new EfetivacaoRequest(CPF_CNPJ);
        restTemplate.postForEntity(
                BASE_URL + "/proposta/" + propostaId + "/efetivar",
                efetivacaoRequest, PropostaResponse.class);

        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                BASE_URL + "/proposta/" + propostaId + "/efetivar",
                efetivacaoRequest,
                ErrorResponse.class
        );

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(409, response.getBody().status());
    }

    @Test
    @DisplayName("Deve buscar proposta por ID com cache")
    void buscarProposta_deveRetornar200ComCache() {
        PropostaRequest criarRequest = new PropostaRequest(
                CPF_CNPJ, List.of("CTR-001", "CTR-002"), 10, TipoAcordo.SEM_JUROS);
        ResponseEntity<PropostaResponse> criadaResponse = restTemplate.postForEntity(
                BASE_URL + "/proposta", criarRequest, PropostaResponse.class);
        UUID propostaId = criadaResponse.getBody().id();

        ResponseEntity<PropostaResponse> response1 = restTemplate.getForEntity(
                BASE_URL + "/proposta/" + propostaId, PropostaResponse.class);

        assertEquals(HttpStatus.OK, response1.getStatusCode());
        assertNotNull(response1.getBody());
        assertEquals(propostaId, response1.getBody().id());
        assertEquals(CPF_CNPJ, response1.getBody().cpfCnpj());

        ResponseEntity<PropostaResponse> response2 = restTemplate.getForEntity(
                BASE_URL + "/proposta/" + propostaId, PropostaResponse.class);

        assertEquals(HttpStatus.OK, response2.getStatusCode());
        assertEquals(response1.getBody().id(), response2.getBody().id());
    }

    @Test
    @DisplayName("Deve cancelar proposta e retornar 204")
    void cancelarProposta_deveRetornar204() {
        PropostaRequest criarRequest = new PropostaRequest(
                CPF_CNPJ, List.of("CTR-001", "CTR-002"), 10, TipoAcordo.SEM_JUROS);
        ResponseEntity<PropostaResponse> criadaResponse = restTemplate.postForEntity(
                BASE_URL + "/proposta", criarRequest, PropostaResponse.class);
        UUID propostaId = criadaResponse.getBody().id();

        ResponseEntity<Void> response = restTemplate.exchange(
                BASE_URL + "/proposta/" + propostaId + "?motivo=Cliente desistiu",
                HttpMethod.DELETE,
                null,
                Void.class
        );

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    @DisplayName("Deve retornar 400 para CPF/CNPJ inválido")
    void criarProposta_cpfCnpjInvalido_deveRetornar400() {
        PropostaRequest request = new PropostaRequest(
                "",
                List.of("CTR-001"),
                10,
                TipoAcordo.SEM_JUROS
        );

        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                BASE_URL + "/proposta",
                request,
                ErrorResponse.class
        );

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().status());
    }
}
