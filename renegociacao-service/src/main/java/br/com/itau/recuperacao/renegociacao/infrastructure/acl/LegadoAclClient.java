package br.com.itau.recuperacao.renegociacao.infrastructure.acl;

import br.com.itau.recuperacao.renegociacao.api.exception.LegadoIndisponivelException;
import br.com.itau.recuperacao.renegociacao.domain.model.Divida;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Collections;
import java.util.List;

/**
 * Client Anti-Corruption Layer para integração com o sistema legado de dívidas.
 * Utiliza Circuit Breaker e Retry para resiliência.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LegadoAclClient {

    private final RestTemplate restTemplate;
    private final LegadoDividaMapper legadoDividaMapper;

    @Value("${legado.acl.base-url:http://localhost:8085}")
    private String baseUrl;

    /**
     * Busca dívidas de um cliente no sistema legado.
     * Protegido por Circuit Breaker e Retry para resiliência.
     *
     * @param cpfCnpj CPF ou CNPJ do cliente
     * @return lista de dívidas convertidas para o modelo de domínio
     */
    @CircuitBreaker(name = "legado-client", fallbackMethod = "fallbackDividas")
    @Retry(name = "legado-client")
    public List<Divida> buscarDividas(String cpfCnpj) {
        log.info("Consultando dívidas no legado para cpfCnpj={}", mascararDocumento(cpfCnpj));

        URI uri = UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path("/api/v1/legado/dividas/")
                .pathSegment(cpfCnpj)
                .build()
                .toUri();

        ResponseEntity<List<DividaLegadoDto>> response = restTemplate.exchange(
                uri,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
        );

        List<DividaLegadoDto> legadoDividas = response.getBody();
        if (legadoDividas == null || legadoDividas.isEmpty()) {
            log.info("Nenhuma dívida encontrada no legado para cpfCnpj={}", mascararDocumento(cpfCnpj));
            return Collections.emptyList();
        }

        List<Divida> dividas = legadoDividaMapper.mapear(legadoDividas);
        log.info("Retornadas {} dívidas do legado para cpfCnpj={}", dividas.size(), mascararDocumento(cpfCnpj));
        return dividas;
    }

    /**
     * Fallback executado quando o sistema legado está indisponível ou o circuit breaker está aberto.
     * Propaga {@link LegadoIndisponivelException} para que a API retorne HTTP 503 ao cliente.
     *
     * @param cpfCnpj CPF ou CNPJ do cliente
     * @param t       exceção que causou o fallback
     * @return nunca retorna normalmente
     */
    @SuppressWarnings("unused")
    private List<Divida> fallbackDividas(String cpfCnpj, Throwable t) {
        log.warn("Legado indisponível (fallback/circuit breaker) para cpfCnpj={}: {}",
                mascararDocumento(cpfCnpj), t != null ? t.getMessage() : "sem detalhe");
        throw new LegadoIndisponivelException(
                "Sistema legado temporariamente indisponível. Tente novamente em instantes.", t);
    }

    private String mascararDocumento(String cpfCnpj) {
        if (cpfCnpj == null || cpfCnpj.length() <= 3) {
            return "***";
        }
        int visibleLength = Math.min(cpfCnpj.length() - 2, 14);
        return cpfCnpj.substring(0, visibleLength) + "**";
    }
}
