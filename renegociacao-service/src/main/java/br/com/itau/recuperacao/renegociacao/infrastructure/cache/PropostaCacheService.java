package br.com.itau.recuperacao.renegociacao.infrastructure.cache;

import br.com.itau.recuperacao.renegociacao.api.dto.response.PropostaResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Serviço de cache para propostas utilizando Redis.
 * Falhas no cache são tratadas de forma resiliente sem propagar exceções.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PropostaCacheService {

    private static final String KEY_PREFIX = "proposta::";
    private static final Duration TTL = Duration.ofMinutes(5);

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Busca uma proposta no cache Redis.
     *
     * @param id identificador da proposta
     * @return Optional contendo a proposta ou vazio se não encontrada no cache
     */
    public Optional<PropostaResponse> buscar(UUID id) {
        try {
            String json = redisTemplate.opsForValue().get(KEY_PREFIX + id);
            if (json == null) {
                return Optional.empty();
            }
            PropostaResponse response = objectMapper.readValue(json, PropostaResponse.class);
            log.debug("Cache hit para proposta id={}", id);
            return Optional.of(response);
        } catch (JsonProcessingException ex) {
            log.warn("Erro ao deserializar proposta do cache, id={}: {}", id, ex.getMessage());
            return Optional.empty();
        } catch (Exception ex) {
            log.warn("Erro ao acessar cache Redis para proposta id={}: {}", id, ex.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Armazena uma proposta no cache Redis com TTL de 5 minutos.
     *
     * @param id identificador da proposta
     * @param proposta dados da proposta a ser cacheada
     */
    public void armazenar(UUID id, PropostaResponse proposta) {
        try {
            String json = objectMapper.writeValueAsString(proposta);
            redisTemplate.opsForValue().set(KEY_PREFIX + id, json, TTL);
            log.debug("Proposta id={} armazenada no cache", id);
        } catch (JsonProcessingException ex) {
            log.warn("Erro ao serializar proposta para cache, id={}: {}", id, ex.getMessage());
        } catch (Exception ex) {
            log.warn("Erro ao armazenar proposta no cache Redis, id={}: {}", id, ex.getMessage());
        }
    }

    /**
     * Remove uma proposta do cache Redis.
     *
     * @param id identificador da proposta a ser invalidada
     */
    public void invalidar(UUID id) {
        try {
            redisTemplate.delete(KEY_PREFIX + id);
            log.debug("Cache invalidado para proposta id={}", id);
        } catch (Exception ex) {
            log.warn("Erro ao invalidar cache Redis para proposta id={}: {}", id, ex.getMessage());
        }
    }
}
