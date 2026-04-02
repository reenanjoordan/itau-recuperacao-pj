package br.com.itau.recuperacao.renegociacao.domain.repository;

import br.com.itau.recuperacao.renegociacao.domain.model.Divida;

import java.util.List;

/**
 * Repositório de domínio para consulta de dívidas elegíveis para renegociação.
 * <p>
 * As implementações concretas devem residir na camada de infraestrutura,
 * podendo consultar sistemas legados ou bases de dados internas.
 */
public interface DividaRepository {

    /**
     * Busca todas as dívidas elegíveis para renegociação de um determinado CPF/CNPJ.
     *
     * @param cpfCnpj CPF ou CNPJ do cliente
     * @return lista de dívidas elegíveis (pode ser vazia)
     */
    List<Divida> buscarDividasElegiveis(String cpfCnpj);
}
