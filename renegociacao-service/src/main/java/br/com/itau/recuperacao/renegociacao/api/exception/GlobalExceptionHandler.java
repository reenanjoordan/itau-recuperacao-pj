package br.com.itau.recuperacao.renegociacao.api.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * Handler global de exceções da API REST.
 * Centraliza o tratamento de erros e padroniza as respostas.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Trata erros de validação dos campos da requisição.
     *
     * @param ex exceção de validação
     * @param request requisição HTTP
     * @return resposta com status 400 e detalhes dos campos inválidos
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        String mensagem = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));

        log.warn("Erro de validação na requisição {}: {}", request.getRequestURI(), mensagem);

        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                mensagem,
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * Trata exceção de proposta não encontrada.
     *
     * @param ex exceção de proposta não encontrada
     * @param request requisição HTTP
     * @return resposta com status 404
     */
    @ExceptionHandler(PropostaNaoEncontradaException.class)
    public ResponseEntity<ErrorResponse> handlePropostaNaoEncontrada(
            PropostaNaoEncontradaException ex, HttpServletRequest request) {

        log.warn("Proposta não encontrada: {}", ex.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    /**
     * Trata exceção de estado ilegal (proposta já efetivada ou cancelada).
     *
     * @param ex exceção de estado ilegal
     * @param request requisição HTTP
     * @return resposta com status 409 Conflict
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(
            IllegalStateException ex, HttpServletRequest request) {

        log.warn("Conflito de estado na requisição {}: {}", request.getRequestURI(), ex.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.CONFLICT.value(),
                "Conflict",
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    /**
     * Trata exceção de dívida inelegível para renegociação.
     *
     * @param ex exceção de dívida inelegível
     * @param request requisição HTTP
     * @return resposta com status 422 Unprocessable Entity
     */
    @ExceptionHandler(DividaInelegivelException.class)
    public ResponseEntity<ErrorResponse> handleDividaInelegivel(
            DividaInelegivelException ex, HttpServletRequest request) {

        log.warn("Dívida inelegível: {}", ex.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.UNPROCESSABLE_ENTITY.value(),
                "Unprocessable Entity",
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(errorResponse);
    }

    /**
     * Trata indisponibilidade do legado (circuit breaker / fallback).
     *
     * @param ex      exceção de legado indisponível
     * @param request requisição HTTP
     * @return resposta com status 503 Service Unavailable
     */
    @ExceptionHandler(LegadoIndisponivelException.class)
    public ResponseEntity<ErrorResponse> handleLegadoIndisponivel(
            LegadoIndisponivelException ex, HttpServletRequest request) {

        log.warn("Legado indisponível na requisição {}: {}", request.getRequestURI(), ex.getMessage());

        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                "Service Unavailable",
                ex.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorResponse);
    }

    /**
     * Trata exceções genéricas não mapeadas.
     *
     * @param ex exceção genérica
     * @param request requisição HTTP
     * @return resposta com status 500 Internal Server Error
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {

        log.error("Erro interno não esperado na requisição {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        ErrorResponse errorResponse = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "Erro interno do servidor. Por favor, tente novamente mais tarde.",
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
