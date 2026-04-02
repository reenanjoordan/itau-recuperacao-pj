package br.com.itau.recuperacao.notificacao.domain.strategy;

import br.com.itau.recuperacao.notificacao.domain.model.Notificacao;
import br.com.itau.recuperacao.notificacao.domain.model.enums.CanalNotificacao;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Implementação do canal de notificação por carta física.
 * Simula a geração de cartas para envio postal a clientes PJ.
 */
@Component
@Slf4j
public class CartaChannel implements NotificacaoChannel {

    /**
     * Gera a carta física para envio ao destinatário.
     * Atualiza o status da notificação para EM_PROCESSAMENTO, pois cartas
     * possuem prazo maior de entrega comparado a canais digitais.
     *
     * @param notificacao a notificação a ser enviada por carta
     */
    @Override
    public void enviar(Notificacao notificacao) {
        log.info("Gerando carta para {}: {}", notificacao.getDestinatario(), notificacao.getTitulo());
        notificacao.setStatus("EM_PROCESSAMENTO");
        log.info("Carta gerada e encaminhada para impressão — Destinatário: {}", notificacao.getDestinatario());
    }

    /**
     * Retorna o canal CARTA.
     *
     * @return CanalNotificacao.CARTA
     */
    @Override
    public CanalNotificacao getCanal() {
        return CanalNotificacao.CARTA;
    }
}
