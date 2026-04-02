package br.com.itau.recuperacao.notificacao.domain.strategy;

import br.com.itau.recuperacao.notificacao.domain.model.Notificacao;
import br.com.itau.recuperacao.notificacao.domain.model.enums.CanalNotificacao;

/**
 * Interface Strategy que define o contrato para canais de notificação.
 * Cada implementação representa um canal específico (e-mail, SMS, carta)
 * com sua própria lógica de envio.
 */
public interface NotificacaoChannel {

    /**
     * Realiza o envio da notificação pelo canal específico.
     *
     * @param notificacao a notificação a ser enviada
     */
    void enviar(Notificacao notificacao);

    /**
     * Retorna o tipo de canal que esta implementação atende.
     *
     * @return o canal de notificação correspondente
     */
    CanalNotificacao getCanal();
}
