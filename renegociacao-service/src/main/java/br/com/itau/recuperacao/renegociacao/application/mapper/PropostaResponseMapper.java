package br.com.itau.recuperacao.renegociacao.application.mapper;

import br.com.itau.recuperacao.renegociacao.api.dto.response.DividaResponse;
import br.com.itau.recuperacao.renegociacao.api.dto.response.ParcelaResponse;
import br.com.itau.recuperacao.renegociacao.api.dto.response.PropostaResponse;
import br.com.itau.recuperacao.renegociacao.domain.model.Proposta;
import br.com.itau.recuperacao.renegociacao.domain.model.enums.PropostaStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Mapeia o agregado {@link Proposta} para {@link PropostaResponse} de forma consistente
 * em todos os handlers da camada de aplicação.
 */
@Component
public class PropostaResponseMapper {

    /**
     * Converte a proposta de domínio em DTO de resposta REST.
     *
     * @param proposta agregado de domínio
     * @return resposta com {@code efetivadaEm} preenchido quando status for {@link PropostaStatus#EFETIVADA}
     */
    public PropostaResponse toResponse(Proposta proposta) {
        List<DividaResponse> dividaResponses = proposta.getDividas().stream()
                .map(d -> new DividaResponse(
                        d.getId(), d.getContrato(), d.getValorOriginal(), d.getValorAtualizado(),
                        d.getDataVencimento(), d.getDiasAtraso(), d.getProduto(), d.isElegivel()))
                .toList();

        List<ParcelaResponse> parcelaResponses = proposta.getParcelas().stream()
                .map(p -> new ParcelaResponse(
                        p.getId(), p.getNumeroParcela(), p.getValor(),
                        p.getDataVencimento(), p.getStatus()))
                .toList();

        LocalDateTime efetivadaEm = proposta.getStatus() == PropostaStatus.EFETIVADA
                ? proposta.getAtualizadaEm()
                : null;

        return new PropostaResponse(
                proposta.getId(), proposta.getCpfCnpj(), proposta.getStatus(),
                proposta.getValorTotal(), proposta.getValorNegociado(),
                proposta.getPercentualDesconto(), proposta.getNumeroParcelas(),
                proposta.getValorParcela(), proposta.getTipoAcordo(),
                dividaResponses, parcelaResponses,
                proposta.getCriadaEm(), proposta.getAtualizadaEm(), efetivadaEm);
    }
}
