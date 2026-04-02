package br.com.itau.recuperacao.legado.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Modelo de domínio que representa uma dívida obtida do sistema legado (mainframe).
 * Contém os dados traduzidos do formato COBOL/DB2 para o formato dos microsserviços.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DividaLegado {

    private String contrato;
    private BigDecimal valorOriginal;
    private BigDecimal valorAtualizado;
    private String dataVencimento;
    private Integer diasAtraso;
    private String produto;
}
