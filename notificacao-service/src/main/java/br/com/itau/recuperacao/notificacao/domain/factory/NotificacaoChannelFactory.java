package br.com.itau.recuperacao.notificacao.domain.factory;

import br.com.itau.recuperacao.notificacao.domain.model.enums.CanalNotificacao;
import br.com.itau.recuperacao.notificacao.domain.strategy.NotificacaoChannel;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Factory que gerencia e fornece implementações de canais de notificação.
 * Utiliza o padrão Factory combinado com Strategy para selecionar dinamicamente
 * o canal adequado com base no tipo de notificação solicitado.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificacaoChannelFactory {

    private final List<NotificacaoChannel> channels;
    private final Map<CanalNotificacao, NotificacaoChannel> channelMap = new EnumMap<>(CanalNotificacao.class);

    /**
     * Inicializa o mapa de canais após a injeção de dependências.
     * Registra cada implementação de NotificacaoChannel indexada pelo seu tipo de canal.
     */
    @PostConstruct
    public void init() {
        channels.forEach(channel -> {
            channelMap.put(channel.getCanal(), channel);
            log.info("Canal de notificação registrado: {}", channel.getCanal());
        });
        log.info("Total de canais registrados: {}", channelMap.size());
    }

    /**
     * Retorna a implementação do canal de notificação correspondente ao tipo solicitado.
     *
     * @param canal o tipo de canal desejado
     * @return a implementação do canal de notificação
     * @throws IllegalArgumentException se o canal solicitado não possuir implementação registrada
     */
    public NotificacaoChannel getChannel(CanalNotificacao canal) {
        NotificacaoChannel channel = channelMap.get(canal);
        if (channel == null) {
            throw new IllegalArgumentException("Canal de notificação não suportado: " + canal);
        }
        return channel;
    }
}
