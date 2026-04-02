package br.com.itau.recuperacao.renegociacao.domain.model.enums;

/**
 * Enum que representa os possíveis status de uma proposta de renegociação.
 * <p>
 * Os status terminais ({@link #EFETIVADA} e {@link #CANCELADA}) indicam que a proposta
 * não permite mais transições de estado.
 */
public enum PropostaStatus {

    SIMULADA("Simulada"),
    PENDENTE("Pendente"),
    EFETIVADA("Efetivada"),
    CANCELADA("Cancelada");

    private final String descricao;

    PropostaStatus(String descricao) {
        this.descricao = descricao;
    }

    /**
     * Retorna a descrição legível do status.
     *
     * @return descrição do status
     */
    public String getDescricao() {
        return descricao;
    }

    /**
     * Verifica se o status é terminal, ou seja, não permite mais transições.
     *
     * @return {@code true} se o status for {@link #EFETIVADA} ou {@link #CANCELADA}
     */
    public boolean isTerminal() {
        return this == EFETIVADA || this == CANCELADA;
    }
}
