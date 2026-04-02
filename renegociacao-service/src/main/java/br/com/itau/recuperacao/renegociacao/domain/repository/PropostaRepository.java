package br.com.itau.recuperacao.renegociacao.domain.repository;

import br.com.itau.recuperacao.renegociacao.domain.model.Proposta;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositório de domínio para persistência e consulta de propostas de renegociação.
 * <p>
 * As implementações concretas devem residir na camada de infraestrutura.
 */
public interface PropostaRepository {

    /**
     * Salva ou atualiza uma proposta de renegociação.
     *
     * @param proposta proposta a ser persistida
     * @return proposta persistida com identificador atualizado
     */
    Proposta salvar(Proposta proposta);

    /**
     * Busca uma proposta pelo seu identificador único.
     *
     * @param id identificador da proposta
     * @return {@link Optional} contendo a proposta, ou vazio se não encontrada
     */
    Optional<Proposta> buscarPorId(UUID id);

    /**
     * Busca todas as propostas associadas a um CPF/CNPJ.
     *
     * @param cpfCnpj CPF ou CNPJ do cliente
     * @return lista de propostas encontradas (pode ser vazia)
     */
    List<Proposta> buscarPorCpfCnpj(String cpfCnpj);
}
