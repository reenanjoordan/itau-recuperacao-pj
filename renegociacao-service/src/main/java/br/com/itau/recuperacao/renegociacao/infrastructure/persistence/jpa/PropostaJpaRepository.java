package br.com.itau.recuperacao.renegociacao.infrastructure.persistence.jpa;

import br.com.itau.recuperacao.renegociacao.infrastructure.persistence.entity.PropostaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Repositório JPA para operações de persistência de propostas.
 */
public interface PropostaJpaRepository extends JpaRepository<PropostaEntity, UUID> {

    /**
     * Busca propostas por CPF/CNPJ do cliente.
     *
     * @param cpfCnpj CPF ou CNPJ do cliente
     * @return lista de entidades de proposta
     */
    List<PropostaEntity> findByCpfCnpj(String cpfCnpj);
}
