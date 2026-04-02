package br.com.itau.recuperacao.renegociacao.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * Request para simulação de renegociação de dívidas.
 *
 * @param cpfCnpj                  CPF ou CNPJ do cliente
 * @param contratosIds             lista de identificadores dos contratos a simular
 * @param numerosParcelasDesejados lista de opções de número de parcelas a serem simuladas
 */
public record SimulacaoRequest(
    @NotBlank(message = "CPF/CNPJ é obrigatório")
    String cpfCnpj,

    @NotEmpty(message = "Lista de contratos é obrigatória")
    List<String> contratosIds,

    @NotEmpty(message = "Lista de parcelas desejadas é obrigatória")
    List<Integer> numerosParcelasDesejados
) {}
