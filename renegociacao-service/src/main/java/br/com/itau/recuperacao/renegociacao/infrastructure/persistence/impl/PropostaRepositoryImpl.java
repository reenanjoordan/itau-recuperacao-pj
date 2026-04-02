package br.com.itau.recuperacao.renegociacao.infrastructure.persistence.impl;

import br.com.itau.recuperacao.renegociacao.domain.model.Divida;
import br.com.itau.recuperacao.renegociacao.domain.model.Parcela;
import br.com.itau.recuperacao.renegociacao.domain.model.Proposta;
import br.com.itau.recuperacao.renegociacao.domain.repository.PropostaRepository;
import br.com.itau.recuperacao.renegociacao.infrastructure.persistence.entity.DividaEntity;
import br.com.itau.recuperacao.renegociacao.infrastructure.persistence.entity.ParcelaEntity;
import br.com.itau.recuperacao.renegociacao.infrastructure.persistence.entity.PropostaEntity;
import br.com.itau.recuperacao.renegociacao.infrastructure.persistence.jpa.PropostaJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementação do repositório de propostas utilizando JPA.
 * Utiliza reflection para reconstituir o agregado de domínio, preservando
 * o encapsulamento da entidade rica {@link Proposta}.
 */
@Repository
@RequiredArgsConstructor
public class PropostaRepositoryImpl implements PropostaRepository {

    private final PropostaJpaRepository propostaJpaRepository;

    /**
     * Salva uma proposta no banco de dados.
     * <p>
     * Se o ID já existir na sessão/persistência, atualiza a entidade gerenciada. Caso contrário
     * insere. Evita criar um segundo {@link PropostaEntity} com o mesmo ID (erro do Hibernate:
     * "A different object with the same identifier value was already associated with the session").
     *
     * @param proposta proposta de domínio a ser persistida
     * @return proposta de domínio com dados atualizados após persistência
     */
    @Override
    public Proposta salvar(Proposta proposta) {
        return propostaJpaRepository.findById(proposta.getId())
                .map(managed -> {
                    aplicarDominioNaEntidadeGerenciada(managed, proposta);
                    return toDomain(propostaJpaRepository.save(managed));
                })
                .orElseGet(() -> {
                    PropostaEntity nova = toEntity(proposta);
                    return toDomain(propostaJpaRepository.save(nova));
                });
    }

    /**
     * Copia o estado do domínio para a entidade já carregada pelo JPA (mesma sessão).
     * Não altera {@code versao}: o Hibernate mantém o controle otimista na entidade gerenciada.
     */
    private void aplicarDominioNaEntidadeGerenciada(PropostaEntity managed, Proposta proposta) {
        managed.setCpfCnpj(proposta.getCpfCnpj());
        managed.setStatus(proposta.getStatus());
        managed.setValorTotal(proposta.getValorTotal());
        managed.setValorNegociado(proposta.getValorNegociado());
        managed.setPercentualDesconto(proposta.getPercentualDesconto());
        managed.setNumeroParcelas(proposta.getNumeroParcelas());
        managed.setValorParcela(proposta.getValorParcela());
        managed.setTipoAcordo(proposta.getTipoAcordo());
        managed.setCriadaEm(proposta.getCriadaEm());
        managed.setAtualizadaEm(proposta.getAtualizadaEm());

        managed.getDividas().clear();
        managed.getParcelas().clear();
        if (proposta.getDividas() != null) {
            proposta.getDividas().forEach(d -> managed.getDividas().add(toDividaEntity(d, managed)));
        }
        if (proposta.getParcelas() != null) {
            proposta.getParcelas().forEach(p -> managed.getParcelas().add(toParcelaEntity(p, managed)));
        }
    }

    /**
     * Busca uma proposta pelo identificador.
     *
     * @param id identificador da proposta
     * @return Optional contendo a proposta ou vazio se não encontrada
     */
    @Override
    public Optional<Proposta> buscarPorId(UUID id) {
        return propostaJpaRepository.findById(id).map(this::toDomain);
    }

    /**
     * Busca propostas pelo CPF/CNPJ do cliente.
     *
     * @param cpfCnpj CPF ou CNPJ do cliente
     * @return lista de propostas encontradas
     */
    @Override
    public List<Proposta> buscarPorCpfCnpj(String cpfCnpj) {
        return propostaJpaRepository.findByCpfCnpj(cpfCnpj).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    private PropostaEntity toEntity(Proposta proposta) {
        PropostaEntity entity = PropostaEntity.builder()
                .id(proposta.getId())
                .cpfCnpj(proposta.getCpfCnpj())
                .status(proposta.getStatus())
                .valorTotal(proposta.getValorTotal())
                .valorNegociado(proposta.getValorNegociado())
                .percentualDesconto(proposta.getPercentualDesconto())
                .numeroParcelas(proposta.getNumeroParcelas())
                .valorParcela(proposta.getValorParcela())
                .tipoAcordo(proposta.getTipoAcordo())
                .criadaEm(proposta.getCriadaEm())
                .atualizadaEm(proposta.getAtualizadaEm())
                .dividas(new ArrayList<>())
                .parcelas(new ArrayList<>())
                .build();

        if (proposta.getDividas() != null) {
            List<DividaEntity> dividaEntities = proposta.getDividas().stream()
                    .map(divida -> toDividaEntity(divida, entity))
                    .collect(Collectors.toList());
            entity.setDividas(dividaEntities);
        }

        if (proposta.getParcelas() != null) {
            List<ParcelaEntity> parcelaEntities = proposta.getParcelas().stream()
                    .map(parcela -> toParcelaEntity(parcela, entity))
                    .collect(Collectors.toList());
            entity.setParcelas(parcelaEntities);
        }

        return entity;
    }

    private DividaEntity toDividaEntity(Divida divida, PropostaEntity propostaEntity) {
        return DividaEntity.builder()
                .id(divida.getId())
                .proposta(propostaEntity)
                .contrato(divida.getContrato())
                .valorOriginal(divida.getValorOriginal())
                .valorAtualizado(divida.getValorAtualizado())
                .dataVencimento(divida.getDataVencimento())
                .diasAtraso(divida.getDiasAtraso())
                .produto(divida.getProduto())
                .build();
    }

    private ParcelaEntity toParcelaEntity(Parcela parcela, PropostaEntity propostaEntity) {
        return ParcelaEntity.builder()
                .id(parcela.getId())
                .proposta(propostaEntity)
                .numeroParcela(parcela.getNumeroParcela())
                .valor(parcela.getValor())
                .dataVencimento(parcela.getDataVencimento())
                .status(parcela.getStatus())
                .build();
    }

    /**
     * Reconstitui o agregado Proposta a partir da entidade JPA via reflection,
     * preservando o encapsulamento do modelo de domínio rico.
     */
    private Proposta toDomain(PropostaEntity entity) {
        List<Divida> dividas = entity.getDividas() != null
                ? entity.getDividas().stream().map(this::toDividaDomain).collect(Collectors.toList())
                : new ArrayList<>();

        List<Parcela> parcelas = entity.getParcelas() != null
                ? entity.getParcelas().stream().map(this::toParcelaDomain).collect(Collectors.toList())
                : new ArrayList<>();

        try {
            Proposta proposta = Proposta.class.getDeclaredConstructor().newInstance();
            setField(proposta, "id", entity.getId());
            setField(proposta, "cpfCnpj", entity.getCpfCnpj());
            setField(proposta, "status", entity.getStatus());
            setField(proposta, "valorTotal", entity.getValorTotal());
            setField(proposta, "valorNegociado", entity.getValorNegociado());
            setField(proposta, "percentualDesconto", entity.getPercentualDesconto());
            setField(proposta, "numeroParcelas", entity.getNumeroParcelas());
            setField(proposta, "valorParcela", entity.getValorParcela());
            setField(proposta, "tipoAcordo", entity.getTipoAcordo());
            setField(proposta, "dividas", dividas);
            setField(proposta, "parcelas", parcelas);
            setField(proposta, "criadaEm", entity.getCriadaEm());
            setField(proposta, "atualizadaEm", entity.getAtualizadaEm());
            setField(proposta, "versao", entity.getVersao());
            return proposta;
        } catch (Exception ex) {
            throw new IllegalStateException("Erro ao reconstituir proposta do banco de dados: " + entity.getId(), ex);
        }
    }

    private void setField(Object target, String fieldName, Object value) {
        Field field = ReflectionUtils.findField(target.getClass(), fieldName);
        if (field != null) {
            ReflectionUtils.makeAccessible(field);
            ReflectionUtils.setField(field, target, value);
        }
    }

    private Divida toDividaDomain(DividaEntity entity) {
        return Divida.builder()
                .id(entity.getId())
                .contrato(entity.getContrato())
                .valorOriginal(entity.getValorOriginal())
                .valorAtualizado(entity.getValorAtualizado())
                .dataVencimento(entity.getDataVencimento())
                .diasAtraso(entity.getDiasAtraso())
                .produto(entity.getProduto())
                .build();
    }

    private Parcela toParcelaDomain(ParcelaEntity entity) {
        return Parcela.builder()
                .id(entity.getId())
                .numeroParcela(entity.getNumeroParcela())
                .valor(entity.getValor())
                .dataVencimento(entity.getDataVencimento())
                .status(entity.getStatus())
                .build();
    }
}
