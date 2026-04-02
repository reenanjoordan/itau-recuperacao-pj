package br.com.itau.recuperacao.notificacao;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Aplicação principal do serviço de Notificação.
 * Responsável pelo envio de notificações multicanal (e-mail, SMS, carta)
 * relacionadas ao processo de recuperação de crédito PJ.
 */
@SpringBootApplication
public class NotificacaoApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificacaoApplication.class, args);
    }
}
