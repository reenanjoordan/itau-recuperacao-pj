package br.com.itau.recuperacao.pagamento.domain.model.enums;

/**
 * Status possíveis do ciclo de vida de um boleto bancário.
 */
public enum BoletoStatus {
    GERADO,
    PAGO,
    VENCIDO,
    CANCELADO
}
