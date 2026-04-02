package br.com.itau.recuperacao.renegociacao.api.exception;

import java.time.LocalDateTime;

/**
 * Estrutura padrão de resposta de erro da API.
 */
public record ErrorResponse(
    LocalDateTime timestamp,
    int status,
    String error,
    String message,
    String path
) {}
