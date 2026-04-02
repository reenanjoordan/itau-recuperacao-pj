package br.com.itau.recuperacao.renegociacao.api.controller;

import br.com.itau.recuperacao.renegociacao.api.dto.request.EfetivacaoRequest;
import br.com.itau.recuperacao.renegociacao.api.dto.request.PropostaRequest;
import br.com.itau.recuperacao.renegociacao.api.dto.request.SimulacaoRequest;
import br.com.itau.recuperacao.renegociacao.api.dto.response.DividaResponse;
import br.com.itau.recuperacao.renegociacao.api.dto.response.PropostaResponse;
import br.com.itau.recuperacao.renegociacao.api.dto.response.SimulacaoResponse;
import br.com.itau.recuperacao.renegociacao.application.command.CancelarPropostaCommand;
import br.com.itau.recuperacao.renegociacao.application.command.CriarPropostaCommand;
import br.com.itau.recuperacao.renegociacao.application.command.EfetivarPropostaCommand;
import br.com.itau.recuperacao.renegociacao.application.handler.BuscarDividasQueryHandler;
import br.com.itau.recuperacao.renegociacao.application.handler.BuscarPropostaQueryHandler;
import br.com.itau.recuperacao.renegociacao.application.handler.CancelarPropostaCommandHandler;
import br.com.itau.recuperacao.renegociacao.application.handler.CriarPropostaCommandHandler;
import br.com.itau.recuperacao.renegociacao.application.handler.EfetivarPropostaCommandHandler;
import br.com.itau.recuperacao.renegociacao.application.query.BuscarDividasQuery;
import br.com.itau.recuperacao.renegociacao.application.query.BuscarPropostaQuery;
import br.com.itau.recuperacao.renegociacao.application.service.SimulacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * Controller REST para operações de renegociação de dívidas PJ.
 */
@RestController
@RequestMapping("/api/v1/renegociacao")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Renegociação", description = "API de Renegociação de Dívidas PJ")
public class RenegociacaoController {

    private final CriarPropostaCommandHandler criarPropostaCommandHandler;
    private final EfetivarPropostaCommandHandler efetivarPropostaCommandHandler;
    private final CancelarPropostaCommandHandler cancelarPropostaCommandHandler;
    private final BuscarPropostaQueryHandler buscarPropostaQueryHandler;
    private final BuscarDividasQueryHandler buscarDividasQueryHandler;
    private final SimulacaoService simulacaoService;

    /**
     * Busca dívidas elegíveis para renegociação de um cliente.
     *
     * @param cpfCnpj CPF ou CNPJ do cliente
     * @return lista de dívidas encontradas
     */
    @GetMapping("/dividas/{*cpfCnpj}")
    @Operation(summary = "Buscar dívidas elegíveis para renegociação")
    @ApiResponse(responseCode = "200", description = "Lista de dívidas retornada com sucesso")
    public ResponseEntity<List<DividaResponse>> buscarDividas(@PathVariable("cpfCnpj") String cpfCnpj) {
        String documento = normalizarDocumentoPath(cpfCnpj);
        log.info("Buscando dívidas para cpfCnpj={}", mascararDocumento(documento));
        List<DividaResponse> dividas = buscarDividasQueryHandler.handle(new BuscarDividasQuery(documento));
        log.info("Encontradas {} dívidas para cpfCnpj={}", dividas.size(), mascararDocumento(documento));
        return ResponseEntity.ok(dividas);
    }

    /**
     * Simula opções de renegociação para os contratos informados.
     *
     * @param request dados da simulação
     * @return lista de opções de simulação
     */
    @PostMapping("/simular")
    @Operation(summary = "Simular opções de renegociação")
    @ApiResponse(responseCode = "200", description = "Simulação realizada com sucesso")
    public ResponseEntity<List<SimulacaoResponse>> simular(@Valid @RequestBody SimulacaoRequest request) {
        log.info("Simulando renegociação para cpfCnpj={}, contratos={}",
                mascararDocumento(request.cpfCnpj()), request.contratosIds().size());
        List<SimulacaoResponse> simulacoes = simulacaoService.simular(request);
        log.info("Geradas {} opções de simulação para cpfCnpj={}",
                simulacoes.size(), mascararDocumento(request.cpfCnpj()));
        return ResponseEntity.ok(simulacoes);
    }

    /**
     * Cria uma nova proposta de renegociação.
     *
     * @param request dados da proposta
     * @return proposta criada com status 201
     */
    @PostMapping("/proposta")
    @Operation(summary = "Criar proposta de renegociação")
    @ApiResponse(responseCode = "201", description = "Proposta criada com sucesso")
    public ResponseEntity<PropostaResponse> criarProposta(@Valid @RequestBody PropostaRequest request) {
        log.info("Criando proposta para cpfCnpj={}, parcelas={}, tipo={}",
                mascararDocumento(request.cpfCnpj()), request.numeroParcelas(), request.tipoAcordo());

        CriarPropostaCommand command = new CriarPropostaCommand(
                request.cpfCnpj(),
                request.contratosIds(),
                request.numeroParcelas(),
                request.tipoAcordo()
        );

        PropostaResponse response = criarPropostaCommandHandler.handle(command);
        URI location = URI.create("/api/v1/renegociacao/proposta/" + response.id());

        log.info("Proposta criada com id={} para cpfCnpj={}", response.id(), mascararDocumento(request.cpfCnpj()));
        return ResponseEntity.created(location).body(response);
    }

    /**
     * Efetiva uma proposta de renegociação existente.
     *
     * @param id identificador da proposta
     * @param request dados de efetivação
     * @return proposta efetivada
     */
    @PostMapping("/proposta/{id}/efetivar")
    @Operation(summary = "Efetivar proposta de renegociação")
    @ApiResponse(responseCode = "200", description = "Proposta efetivada com sucesso")
    public ResponseEntity<PropostaResponse> efetivarProposta(
            @PathVariable UUID id, @Valid @RequestBody EfetivacaoRequest request) {
        log.info("Efetivando proposta id={} para cpfCnpj={}", id, mascararDocumento(request.cpfCnpj()));

        EfetivarPropostaCommand command = new EfetivarPropostaCommand(id, request.cpfCnpj());
        PropostaResponse response = efetivarPropostaCommandHandler.handle(command);

        log.info("Proposta id={} efetivada com sucesso", id);
        return ResponseEntity.ok(response);
    }

    /**
     * Consulta uma proposta de renegociação por ID.
     *
     * @param id identificador da proposta
     * @return proposta encontrada
     */
    @GetMapping("/proposta/{id}")
    @Operation(summary = "Consultar proposta por ID")
    @ApiResponse(responseCode = "200", description = "Proposta encontrada com sucesso")
    public ResponseEntity<PropostaResponse> buscarProposta(@PathVariable UUID id) {
        log.info("Buscando proposta id={}", id);
        PropostaResponse response = buscarPropostaQueryHandler.handle(new BuscarPropostaQuery(id));
        log.info("Proposta id={} encontrada com status={}", id, response.status());
        return ResponseEntity.ok(response);
    }

    /**
     * Cancela uma proposta de renegociação.
     *
     * @param id identificador da proposta
     * @param motivo motivo do cancelamento
     * @return resposta vazia com status 204
     */
    @DeleteMapping("/proposta/{id}")
    @Operation(summary = "Cancelar proposta de renegociação")
    @ApiResponse(responseCode = "204", description = "Proposta cancelada com sucesso")
    public ResponseEntity<Void> cancelarProposta(@PathVariable UUID id, @RequestParam String motivo) {
        log.info("Cancelando proposta id={}, motivo={}", id, motivo);

        CancelarPropostaCommand command = new CancelarPropostaCommand(id, motivo);
        cancelarPropostaCommandHandler.handle(command);

        log.info("Proposta id={} cancelada com sucesso", id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Variável de caminho {@code {*cpfCnpj}} pode vir com "/" inicial; remove para o domínio receber o documento correto.
     */
    private static String normalizarDocumentoPath(String cpfCnpj) {
        if (cpfCnpj != null && cpfCnpj.startsWith("/")) {
            return cpfCnpj.substring(1);
        }
        return cpfCnpj;
    }

    private String mascararDocumento(String cpfCnpj) {
        if (cpfCnpj == null || cpfCnpj.length() <= 3) {
            return "***";
        }
        int visibleLength = Math.min(cpfCnpj.length() - 2, 14);
        return cpfCnpj.substring(0, visibleLength) + "**";
    }
}
