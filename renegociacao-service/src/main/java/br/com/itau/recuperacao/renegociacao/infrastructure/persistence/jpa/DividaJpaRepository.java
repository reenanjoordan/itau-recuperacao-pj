package br.com.itau.recuperacao.renegociacao.infrastructure.persistence.jpa;

import br.com.itau.recuperacao.renegociacao.infrastructure.persistence.entity.DividaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Repositório JPA para operações de persistência de dívidas.
 */
public interface DividaJpaRepository extends JpaRepository<DividaEntity, UUID> {

    /**
     * Busca dívidas por ID da proposta.
     *
     * @param propostaId identificador da proposta
     * @return lista de entidades de dívida
     */
    List<DividaEntity> findByPropostaId(UUID propostaId);
}
