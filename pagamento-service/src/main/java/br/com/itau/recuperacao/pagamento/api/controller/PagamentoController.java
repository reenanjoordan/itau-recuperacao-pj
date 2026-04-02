package br.com.itau.recuperacao.pagamento.api.controller;

import br.com.itau.recuperacao.pagamento.application.service.BoletoService;
import br.com.itau.recuperacao.pagamento.domain.model.Boleto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Controller REST para operações de pagamento.
 * Expõe endpoints para geração e consulta de boletos bancários.
 */
@RestController
@RequestMapping("/api/v1/pagamento")
@RequiredArgsConstructor
@Slf4j
public class PagamentoController {

    private final BoletoService boletoService;

    /**
     * Gera um novo boleto bancário para uma proposta de renegociação.
     *
     * @param request mapa contendo propostaId, valor e numeroParcelas
     * @return o boleto gerado com status HTTP 201
     */
    @PostMapping("/boletos")
    public ResponseEntity<Boleto> gerarBoleto(@RequestBody Map<String, String> request) {
        log.info("Recebida requisição para gerar boleto");

        UUID propostaId = UUID.fromString(request.get("propostaId"));
        BigDecimal valor = new BigDecimal(request.get("valor"));
        Integer numeroParcelas = Integer.parseInt(request.get("numeroParcelas"));

        Boleto boleto = boletoService.gerarBoleto(propostaId, valor, numeroParcelas);

        return ResponseEntity.status(HttpStatus.CREATED).body(boleto);
    }

    /**
     * Busca um boleto pelo seu identificador único.
     *
     * @param id identificador UUID do boleto
     * @return o boleto encontrado ou status HTTP 404
     */
    @GetMapping("/boletos/{id}")
    public ResponseEntity<Boleto> buscarBoleto(@PathVariable UUID id) {
        log.info("Recebida requisição para buscar boleto ID: {}", id);

        return boletoService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
