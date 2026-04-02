package br.com.itau.recuperacao.cobranca.domain.model;

import br.com.itau.recuperacao.cobranca.domain.model.enums.CanalCobranca;
import br.com.itau.recuperacao.cobranca.domain.model.enums.TipoAcao;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Modelo de domínio que representa uma ação de cobrança executada sobre um devedor PJ.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AcaoCobranca {

    private UUID id;
    private String cpfCnpj;
    private TipoAcao tipoAcao;
    private CanalCobranca canal;
    private String descricao;
    private LocalDateTime criadaEm;
    private String status;
}
