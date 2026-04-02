package br.com.itau.recuperacao.notificacao.domain.model;

import br.com.itau.recuperacao.notificacao.domain.model.enums.CanalNotificacao;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Modelo de domínio que representa uma notificação enviada a um cliente PJ
 * durante o processo de recuperação de crédito.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Notificacao {

    private UUID id;
    private String cpfCnpj;
    private String destinatario;
    private CanalNotificacao canal;
    private String titulo;
    private String mensagem;
    private LocalDateTime enviadaEm;
    private String status;
}
