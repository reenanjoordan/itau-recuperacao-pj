package br.com.itau.recuperacao.renegociacao.api.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Request para efetivação de proposta de renegociação.
 */
public record EfetivacaoRequest(
    @NotBlank(message = "CPF/CNPJ é obrigatório")
    String cpfCnpj
) {}
