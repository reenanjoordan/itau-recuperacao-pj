package br.com.itau.recuperacao.notificacao.domain.strategy;

import br.com.itau.recuperacao.notificacao.domain.model.Notificacao;
import br.com.itau.recuperacao.notificacao.domain.model.enums.CanalNotificacao;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Implementação do canal de notificação por SMS.
 * Simula o envio de mensagens SMS para clientes PJ.
 */
@Component
@Slf4j
public class SmsChannel implements NotificacaoChannel {

    /**
     * Envia a notificação por SMS.
     * Atualiza o status da notificação para ENVIADO após o processamento.
     *
     * @param notificacao a notificação a ser enviada por SMS
     */
    @Override
    public void enviar(Notificacao notificacao) {
        log.info("Enviando SMS para {}: {}", notificacao.getDestinatario(), notificacao.getMensagem());
        notificacao.setStatus("ENVIADO");
        log.info("SMS enviado com sucesso para {}", notificacao.getDestinatario());
    }

    /**
     * Retorna o canal SMS.
     *
     * @return CanalNotificacao.SMS
     */
    @Override
    public CanalNotificacao getCanal() {
        return CanalNotificacao.SMS;
    }
}
