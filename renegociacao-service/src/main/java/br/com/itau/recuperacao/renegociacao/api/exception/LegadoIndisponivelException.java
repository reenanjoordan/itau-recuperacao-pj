package br.com.itau.recuperacao.renegociacao.api.exception;

/**
 * Lançada quando o sistema legado (ACL) está indisponível ou o circuit breaker está aberto.
 * Resulta em HTTP 503 para o cliente, conforme estratégia de degradação controlada.
 */
public class LegadoIndisponivelException extends RuntimeException {

    /**
     * @param message mensagem descritiva (sem dados sensíveis)
     */
    public LegadoIndisponivelException(String message) {
        super(message);
    }

    /**
     * @param message mensagem descritiva
     * @param cause   causa original
     */
    public LegadoIndisponivelException(String message, Throwable cause) {
        super(message, cause);
    }
}
