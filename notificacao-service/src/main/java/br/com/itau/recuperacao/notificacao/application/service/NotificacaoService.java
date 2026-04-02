package br.com.itau.recuperacao.notificacao.application.service;

import br.com.itau.recuperacao.notificacao.domain.factory.NotificacaoChannelFactory;
import br.com.itau.recuperacao.notificacao.domain.model.Notificacao;
import br.com.itau.recuperacao.notificacao.domain.model.enums.CanalNotificacao;
import br.com.itau.recuperacao.notificacao.domain.strategy.NotificacaoChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Serviço responsável pela lógica de negócio do envio de notificações.
 * Utiliza o padrão Factory/Strategy para delegar o envio ao canal correto.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificacaoService {

    private final NotificacaoChannelFactory channelFactory;

    /**
     * Cria e envia uma notificação pelo canal especificado.
     * A notificação é construída com os dados fornecidos e delegada ao canal
     * apropriado através da factory.
     *
     * @param cpfCnpj      CPF ou CNPJ do cliente destinatário
     * @param destinatario endereço do destinatário (e-mail, telefone, endereço)
     * @param canal        canal de envio da notificação
     * @param titulo       título/assunto da notificação
     * @param mensagem     corpo da mensagem
     * @return a notificação criada e processada com seu status atualizado
     */
    public Notificacao enviar(String cpfCnpj, String destinatario, CanalNotificacao canal,
                              String titulo, String mensagem) {
        log.info("Iniciando envio de notificação. CPF/CNPJ: {}, Canal: {}, Destinatário: {}",
                cpfCnpj, canal, destinatario);

        Notificacao notificacao = Notificacao.builder()
                .id(UUID.randomUUID())
                .cpfCnpj(cpfCnpj)
                .destinatario(destinatario)
                .canal(canal)
                .titulo(titulo)
                .mensagem(mensagem)
                .enviadaEm(LocalDateTime.now())
                .status("PENDENTE")
                .build();

        NotificacaoChannel channel = channelFactory.getChannel(canal);
        channel.enviar(notificacao);

        log.info("Notificação processada. ID: {}, Status: {}", notificacao.getId(), notificacao.getStatus());

        return notificacao;
    }

    /**
     * Processa eventos de proposta recebidos via Kafka e dispara notificações
     * automáticas por e-mail e SMS para o cliente envolvido.
     *
     * @param evento mapa contendo os dados do evento de proposta
     */
    public void processarEventoProposta(Map<String, Object> evento) {
        String cpfCnpj = (String) evento.getOrDefault("cpfCnpj", "00000000000000");
        String email = (String) evento.getOrDefault("email", "cliente@empresa.com.br");
        String telefone = (String) evento.getOrDefault("telefone", "11999999999");
        String propostaId = String.valueOf(evento.getOrDefault("propostaId", "N/A"));
        String eventType = String.valueOf(evento.getOrDefault("eventType", "PROPOSTA_EVENTO"));

        log.info("Processando evento {} para notificação. PropostaId: {}, CPF/CNPJ: {}",
                eventType, propostaId, cpfCnpj);

        enviar(cpfCnpj, email, CanalNotificacao.EMAIL,
                "Atualização sobre sua renegociação — Proposta " + propostaId,
                "Prezado cliente, informamos que sua proposta de renegociação " + propostaId +
                        " teve uma atualização de status. Acesse o portal para mais detalhes.");

        enviar(cpfCnpj, telefone, CanalNotificacao.SMS,
                "Renegociação Itaú",
                "Itaú Recuperação: Sua proposta " + propostaId +
                        " foi atualizada. Acesse o portal ou ligue 4004-4828.");

        log.info("Notificações enviadas com sucesso para proposta: {}", propostaId);
    }
}
