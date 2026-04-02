package br.com.itau.recuperacao.renegociacao.api.exception;

import java.util.UUID;

/**
 * Exceção lançada quando uma proposta de renegociação não é encontrada pelo identificador informado.
 */
public class PropostaNaoEncontradaException extends RuntimeException {

    /**
     * Cria uma exceção informando o identificador da proposta não encontrada.
     *
     * @param propostaId identificador da proposta buscada
     */
    public PropostaNaoEncontradaException(UUID propostaId) {
        super("Proposta não encontrada com o ID: " + propostaId);
    }

    /**
     * Cria uma exceção com mensagem e causa encadeada.
     *
     * @param message mensagem descritiva do erro
     * @param cause   exceção que causou este erro
     */
    public PropostaNaoEncontradaException(String message, Throwable cause) {
        super(message, cause);
    }
}
