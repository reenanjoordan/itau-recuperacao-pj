package br.com.itau.recuperacao.legado.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Modelo de domínio que representa os dados cadastrais de um cliente PJ
 * obtidos do sistema legado (mainframe).
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ClienteLegado {

    private String cpfCnpj;
    private String razaoSocial;
    private String email;
    private String telefone;
}
