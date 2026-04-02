package br.com.itau.recuperacao.renegociacao.unit.api;

import br.com.itau.recuperacao.renegociacao.api.controller.RenegociacaoController;
import br.com.itau.recuperacao.renegociacao.api.dto.response.DividaResponse;
import br.com.itau.recuperacao.renegociacao.api.dto.response.ParcelaResponse;
import br.com.itau.recuperacao.renegociacao.api.dto.response.PropostaResponse;
import br.com.itau.recuperacao.renegociacao.api.dto.response.SimulacaoResponse;
import br.com.itau.recuperacao.renegociacao.api.exception.PropostaNaoEncontradaException;
import br.com.itau.recuperacao.renegociacao.application.handler.BuscarDividasQueryHandler;
import br.com.itau.recuperacao.renegociacao.application.handler.BuscarPropostaQueryHandler;
import br.com.itau.recuperacao.renegociacao.application.handler.CancelarPropostaCommandHandler;
import br.com.itau.recuperacao.renegociacao.application.handler.CriarPropostaCommandHandler;
import br.com.itau.recuperacao.renegociacao.application.handler.EfetivarPropostaCommandHandler;
import br.com.itau.recuperacao.renegociacao.application.service.SimulacaoService;
import br.com.itau.recuperacao.renegociacao.domain.model.enums.PropostaStatus;
import br.com.itau.recuperacao.renegociacao.domain.model.enums.TipoAcordo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("unit")
@WebMvcTest(RenegociacaoController.class)
@AutoConfigureMockMvc(addFilters = false)
class RenegociacaoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CriarPropostaCommandHandler criarPropostaCommandHandler;

    @MockBean
    private EfetivarPropostaCommandHandler efetivarPropostaCommandHandler;

    @MockBean
    private CancelarPropostaCommandHandler cancelarPropostaCommandHandler;

    @MockBean
    private BuscarPropostaQueryHandler buscarPropostaQueryHandler;

    @MockBean
    private BuscarDividasQueryHandler buscarDividasQueryHandler;

    @MockBean
    private SimulacaoService simulacaoService;

    private static final String BASE_URL = "/api/v1/renegociacao";
    private static final UUID PROPOSTA_ID = UUID.randomUUID();
    private static final String CPF_CNPJ = "12.345.678/0001-99";

    private PropostaResponse criarPropostaResponse() {
        return new PropostaResponse(
                PROPOSTA_ID,
                CPF_CNPJ,
                PropostaStatus.PENDENTE,
                new BigDecimal("10000.00"),
                new BigDecimal("8500.00"),
                new BigDecimal("15.00"),
                10,
                new BigDecimal("850.00"),
                TipoAcordo.SEM_JUROS,
                List.of(new DividaResponse(
                        UUID.randomUUID(), "CTR-001",
                        new BigDecimal("5000.00"), new BigDecimal("6000.00"),
                        LocalDate.now().minusMonths(6), 180, "Capital de Giro", true
                )),
                List.of(new ParcelaResponse(
                        UUID.randomUUID(), 1,
                        new BigDecimal("850.00"), LocalDate.now().plusMonths(1), "PENDENTE"
                )),
                LocalDateTime.now(),
                LocalDateTime.now(),
                null
        );
    }

    @Test
    @DisplayName("GET /dividas/{cpfCnpj} deve retornar 200 com lista de dívidas")
    void listarDividas_deveRetornar200() throws Exception {
        List<DividaResponse> dividas = List.of(
                new DividaResponse(
                        UUID.randomUUID(), "CTR-001",
                        new BigDecimal("5000.00"), new BigDecimal("6000.00"),
                        LocalDate.now().minusMonths(6), 180, "Capital de Giro", true
                ),
                new DividaResponse(
                        UUID.randomUUID(), "CTR-002",
                        new BigDecimal("3000.00"), new BigDecimal("4000.00"),
                        LocalDate.now().minusMonths(3), 90, "Cheque Especial", true
                )
        );

        when(buscarDividasQueryHandler.handle(any())).thenReturn(dividas);

        mockMvc.perform(get(BASE_URL + "/dividas/{cpfCnpj}", CPF_CNPJ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].contrato").value("CTR-001"))
                .andExpect(jsonPath("$[0].elegivel").value(true))
                .andExpect(jsonPath("$[1].contrato").value("CTR-002"));
    }

    @Test
    @DisplayName("POST /simular deve retornar 200 com simulações")
    void simular_deveRetornar200() throws Exception {
        List<SimulacaoResponse> simulacoes = List.of(
                new SimulacaoResponse(TipoAcordo.SEM_JUROS, 12,
                        new BigDecimal("10000.00"), new BigDecimal("8500.00"),
                        new BigDecimal("708.33"), new BigDecimal("15.00")),
                new SimulacaoResponse(TipoAcordo.JUROS_SIMPLES, 12,
                        new BigDecimal("10000.00"), new BigDecimal("11800.00"),
                        new BigDecimal("983.33"), BigDecimal.ZERO),
                new SimulacaoResponse(TipoAcordo.JUROS_COMPOSTOS, 12,
                        new BigDecimal("10000.00"), new BigDecimal("11956.18"),
                        new BigDecimal("916.80"), BigDecimal.ZERO)
        );

        when(simulacaoService.simular(any())).thenReturn(simulacoes);

        String requestBody = """
                {
                    "cpfCnpj": "12.345.678/0001-99",
                    "contratosIds": ["CTR-001", "CTR-002"],
                    "numerosParcelasDesejados": [12]
                }
                """;

        mockMvc.perform(post(BASE_URL + "/simular")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].tipoAcordo").value("SEM_JUROS"))
                .andExpect(jsonPath("$[0].numeroParcelas").value(12));
    }

    @Test
    @DisplayName("POST /proposta deve retornar 201 com Location header")
    void criarProposta_deveRetornar201() throws Exception {
        PropostaResponse response = criarPropostaResponse();

        when(criarPropostaCommandHandler.handle(any())).thenReturn(response);

        String requestBody = """
                {
                    "cpfCnpj": "12.345.678/0001-99",
                    "contratosIds": ["CTR-001"],
                    "numeroParcelas": 10,
                    "tipoAcordo": "SEM_JUROS"
                }
                """;

        mockMvc.perform(post(BASE_URL + "/proposta")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/renegociacao/proposta/" + PROPOSTA_ID))
                .andExpect(jsonPath("$.id").value(PROPOSTA_ID.toString()))
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andExpect(jsonPath("$.valorTotal").value(10000.00))
                .andExpect(jsonPath("$.valorNegociado").value(8500.00))
                .andExpect(jsonPath("$.numeroParcelas").value(10));
    }

    @Test
    @DisplayName("POST /proposta/{id}/efetivar deve retornar 200")
    void efetivarProposta_deveRetornar200() throws Exception {
        LocalDateTime agora = LocalDateTime.now();
        PropostaResponse response = new PropostaResponse(
                PROPOSTA_ID, CPF_CNPJ, PropostaStatus.EFETIVADA,
                new BigDecimal("10000.00"), new BigDecimal("8500.00"),
                new BigDecimal("15.00"), 10, new BigDecimal("850.00"),
                TipoAcordo.SEM_JUROS, List.of(), List.of(),
                agora, agora, agora
        );

        when(efetivarPropostaCommandHandler.handle(any())).thenReturn(response);

        String requestBody = """
                {
                    "cpfCnpj": "12.345.678/0001-99"
                }
                """;

        mockMvc.perform(post(BASE_URL + "/proposta/{id}/efetivar", PROPOSTA_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(PROPOSTA_ID.toString()))
                .andExpect(jsonPath("$.status").value("EFETIVADA"));
    }

    @Test
    @DisplayName("POST /proposta/{id}/efetivar já efetivada deve retornar 409")
    void efetivarProposta_jaEfetivada_deveRetornar409() throws Exception {
        when(efetivarPropostaCommandHandler.handle(any()))
                .thenThrow(new IllegalStateException("Proposta não pode ser efetivada. Status atual: EFETIVADA"));

        String requestBody = """
                {
                    "cpfCnpj": "12.345.678/0001-99"
                }
                """;

        mockMvc.perform(post(BASE_URL + "/proposta/{id}/efetivar", PROPOSTA_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"));
    }

    @Test
    @DisplayName("GET /proposta/{id} deve retornar 200")
    void buscarProposta_deveRetornar200() throws Exception {
        PropostaResponse response = criarPropostaResponse();

        when(buscarPropostaQueryHandler.handle(any())).thenReturn(response);

        mockMvc.perform(get(BASE_URL + "/proposta/{id}", PROPOSTA_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(PROPOSTA_ID.toString()))
                .andExpect(jsonPath("$.cpfCnpj").value(CPF_CNPJ))
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andExpect(jsonPath("$.dividas", hasSize(1)))
                .andExpect(jsonPath("$.parcelas", hasSize(1)));
    }

    @Test
    @DisplayName("GET /proposta/{id} não encontrada deve retornar 404")
    void buscarProposta_naoEncontrada_deveRetornar404() throws Exception {
        UUID idInexistente = UUID.randomUUID();

        when(buscarPropostaQueryHandler.handle(any()))
                .thenThrow(new PropostaNaoEncontradaException(idInexistente));

        mockMvc.perform(get(BASE_URL + "/proposta/{id}", idInexistente))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    @DisplayName("DELETE /proposta/{id} deve retornar 204")
    void cancelarProposta_deveRetornar204() throws Exception {
        doNothing().when(cancelarPropostaCommandHandler).handle(any());

        mockMvc.perform(delete(BASE_URL + "/proposta/{id}", PROPOSTA_ID)
                        .param("motivo", "Cliente desistiu"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("POST /proposta com dados inválidos deve retornar 400")
    void criarProposta_comDadosInvalidos_deveRetornar400() throws Exception {
        String requestBodyInvalido = """
                {
                    "cpfCnpj": "",
                    "contratosIds": [],
                    "numeroParcelas": 0,
                    "tipoAcordo": null
                }
                """;

        mockMvc.perform(post(BASE_URL + "/proposta")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBodyInvalido))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("POST /simular com dados inválidos deve retornar 400")
    void simular_comDadosInvalidos_deveRetornar400() throws Exception {
        String requestBodyInvalido = """
                {
                    "cpfCnpj": "",
                    "contratosIds": [],
                    "numerosParcelasDesejados": []
                }
                """;

        mockMvc.perform(post(BASE_URL + "/simular")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBodyInvalido))
                .andExpect(status().isBadRequest());
    }
}
