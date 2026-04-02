package br.com.itau.recuperacao.notificacao.domain.strategy;

import br.com.itau.recuperacao.notificacao.domain.model.Notificacao;
import br.com.itau.recuperacao.notificacao.domain.model.enums.CanalNotificacao;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Implementação do canal de notificação por e-mail.
 * Simula o envio de e-mails para clientes PJ.
 */
@Component
@Slf4j
public class EmailChannel implements NotificacaoChannel {

    /**
     * Envia a notificação por e-mail.
     * Atualiza o status da notificação para ENVIADO após o processamento.
     *
     * @param notificacao a notificação a ser enviada por e-mail
     */
    @Override
    public void enviar(Notificacao notificacao) {
        log.info("Enviando e-mail para {}: {}", notificacao.getDestinatario(), notificacao.getTitulo());
        notificacao.setStatus("ENVIADO");
        log.info("E-mail enviado com sucesso para {} — Assunto: {}",
                notificacao.getDestinatario(), notificacao.getTitulo());
    }

    /**
     * Retorna o canal EMAIL.
     *
     * @return CanalNotificacao.EMAIL
     */
    @Override
    public CanalNotificacao getCanal() {
        return CanalNotificacao.EMAIL;
    }
}
