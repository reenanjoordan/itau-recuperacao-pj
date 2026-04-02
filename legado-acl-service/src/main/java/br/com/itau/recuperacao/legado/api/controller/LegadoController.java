package br.com.itau.recuperacao.legado.api.controller;

import br.com.itau.recuperacao.legado.application.service.LegadoService;
import br.com.itau.recuperacao.legado.domain.model.ClienteLegado;
import br.com.itau.recuperacao.legado.domain.model.DividaLegado;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller REST que expõe a camada Anti-Corruption Layer do legado.
 * Fornece endpoints para consulta de dívidas e dados cadastrais de clientes
 * traduzidos do formato do mainframe para o modelo de domínio moderno.
 */
@RestController
@RequestMapping("/api/v1/legado")
@RequiredArgsConstructor
@Slf4j
public class LegadoController {

    private final LegadoService legadoService;

    /**
     * Consulta as dívidas de um cliente no sistema legado pelo CPF/CNPJ.
     *
     * @param cpfCnpj CPF ou CNPJ do cliente
     * @return lista de dívidas encontradas no sistema legado
     */
    @GetMapping("/dividas/{*cpfCnpj}")
    public ResponseEntity<List<DividaLegado>> buscarDividas(@PathVariable String cpfCnpj) {
        log.info("Recebida requisição para buscar dívidas do legado. CPF/CNPJ: {}", cpfCnpj);

        List<DividaLegado> dividas = legadoService.buscarDividas(cpfCnpj);

        return ResponseEntity.ok(dividas);
    }

    /**
     * Consulta os dados cadastrais de um cliente no sistema legado pelo CPF/CNPJ.
     *
     * @param cpfCnpj CPF ou CNPJ do cliente
     * @return dados do cliente ou status HTTP 404 se não encontrado
     */
    @GetMapping("/clientes/{*cpfCnpj}")
    public ResponseEntity<ClienteLegado> buscarCliente(@PathVariable String cpfCnpj) {
        log.info("Recebida requisição para buscar cliente do legado. CPF/CNPJ: {}", cpfCnpj);

        return legadoService.buscarCliente(cpfCnpj)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
