package br.com.itau.recuperacao.cobranca.api.controller;

import br.com.itau.recuperacao.cobranca.application.service.CobrancaService;
import br.com.itau.recuperacao.cobranca.domain.model.AcaoCobranca;
import br.com.itau.recuperacao.cobranca.domain.model.enums.CanalCobranca;
import br.com.itau.recuperacao.cobranca.domain.model.enums.TipoAcao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller REST para operações de cobrança.
 * Expõe endpoints para criação e consulta de ações de cobrança.
 */
@RestController
@RequestMapping("/api/v1/cobranca")
@RequiredArgsConstructor
@Slf4j
public class CobrancaController {

    private final CobrancaService cobrancaService;

    /**
     * Cria uma nova ação de cobrança.
     *
     * @param request mapa contendo cpfCnpj, tipoAcao, canal e descricao
     * @return a ação de cobrança criada com status HTTP 201
     */
    @PostMapping("/acoes")
    public ResponseEntity<AcaoCobranca> criarAcao(@RequestBody Map<String, String> request) {
        log.info("Recebida requisição para criar ação de cobrança");

        String cpfCnpj = request.get("cpfCnpj");
        TipoAcao tipoAcao = TipoAcao.valueOf(request.get("tipoAcao"));
        CanalCobranca canal = CanalCobranca.valueOf(request.get("canal"));
        String descricao = request.get("descricao");

        AcaoCobranca acao = cobrancaService.criarAcao(cpfCnpj, tipoAcao, canal, descricao);

        return ResponseEntity.status(HttpStatus.CREATED).body(acao);
    }

    /**
     * Lista todas as ações de cobrança de um devedor pelo CPF/CNPJ.
     *
     * @param cpfCnpj CPF ou CNPJ do devedor
     * @return lista de ações de cobrança encontradas
     */
    @GetMapping("/acoes/{cpfCnpj}")
    public ResponseEntity<List<AcaoCobranca>> listarPorCpfCnpj(@PathVariable String cpfCnpj) {
        log.info("Recebida requisição para listar ações de cobrança do CPF/CNPJ: {}", cpfCnpj);

        List<AcaoCobranca> acoes = cobrancaService.listarPorCpfCnpj(cpfCnpj);

        return ResponseEntity.ok(acoes);
    }
}
