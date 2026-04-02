package br.com.itau.recuperacao.renegociacao.infrastructure.persistence.impl;

import br.com.itau.recuperacao.renegociacao.domain.model.Divida;
import br.com.itau.recuperacao.renegociacao.domain.repository.DividaRepository;
import br.com.itau.recuperacao.renegociacao.infrastructure.acl.LegadoAclClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Implementação do repositório de dívidas que delega ao client ACL do legado.
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class DividaRepositoryImpl implements DividaRepository {

    private final LegadoAclClient legadoAclClient;

    /**
     * Busca dívidas elegíveis para renegociação de um cliente via sistema legado.
     *
     * @param cpfCnpj CPF ou CNPJ do cliente
     * @return lista de dívidas de domínio
     */
    @Override
    public List<Divida> buscarDividasElegiveis(String cpfCnpj) {
        log.info("Buscando dívidas elegíveis no legado para cpfCnpj={}", mascararDocumento(cpfCnpj));
        List<Divida> dividas = legadoAclClient.buscarDividas(cpfCnpj);
        log.info("Retornadas {} dívidas do legado para cpfCnpj={}", dividas.size(), mascararDocumento(cpfCnpj));
        return dividas;
    }

    private String mascararDocumento(String cpfCnpj) {
        if (cpfCnpj == null || cpfCnpj.length() <= 3) {
            return "***";
        }
        int visibleLength = Math.min(cpfCnpj.length() - 2, 14);
        return cpfCnpj.substring(0, visibleLength) + "**";
    }
}
