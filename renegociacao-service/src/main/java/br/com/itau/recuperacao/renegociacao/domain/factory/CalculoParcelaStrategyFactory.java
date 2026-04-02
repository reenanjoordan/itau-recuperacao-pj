package br.com.itau.recuperacao.renegociacao.domain.factory;

import br.com.itau.recuperacao.renegociacao.domain.model.enums.TipoAcordo;
import br.com.itau.recuperacao.renegociacao.domain.strategy.CalculoParcelaStrategy;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Fábrica responsável por fornecer a estratégia de cálculo de parcelas adequada
 * com base no tipo de acordo selecionado.
 * <p>
 * Utiliza injeção de dependência para coletar todas as implementações de
 * {@link CalculoParcelaStrategy} e indexá-las por {@link TipoAcordo}.
 */
@Component
@RequiredArgsConstructor
public class CalculoParcelaStrategyFactory {

    private final List<CalculoParcelaStrategy> strategies;
    private final Map<TipoAcordo, CalculoParcelaStrategy> strategyMap = new EnumMap<>(TipoAcordo.class);

    /**
     * Inicializa o mapa de estratégias indexado por tipo de acordo.
     */
    @PostConstruct
    void init() {
        strategies.forEach(strategy -> strategyMap.put(strategy.getTipoAcordo(), strategy));
    }

    /**
     * Retorna a estratégia de cálculo correspondente ao tipo de acordo informado.
     *
     * @param tipo tipo de acordo desejado
     * @return instância de {@link CalculoParcelaStrategy} correspondente
     * @throws IllegalArgumentException se não houver estratégia cadastrada para o tipo informado
     */
    public CalculoParcelaStrategy getStrategy(TipoAcordo tipo) {
        CalculoParcelaStrategy strategy = strategyMap.get(tipo);
        if (strategy == null) {
            throw new IllegalArgumentException("Nenhuma estratégia encontrada para o tipo de acordo: " + tipo);
        }
        return strategy;
    }
}
