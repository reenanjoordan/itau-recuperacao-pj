package br.com.itau.recuperacao.renegociacao.api.dto.request;

import br.com.itau.recuperacao.renegociacao.domain.model.enums.TipoAcordo;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Request para criação de proposta de renegociação.
 */
public record PropostaRequest(
    @NotBlank(message = "CPF/CNPJ é obrigatório")
    String cpfCnpj,

    @NotEmpty(message = "Lista de contratos é obrigatória")
    List<String> contratosIds,

    @NotNull(message = "Número de parcelas é obrigatório")
    @Min(value = 1, message = "Número mínimo de parcelas é 1")
    @Max(value = 60, message = "Número máximo de parcelas é 60")
    Integer numeroParcelas,

    @NotNull(message = "Tipo de acordo é obrigatório")
    TipoAcordo tipoAcordo
) {}
