package br.com.itau.recuperacao.renegociacao.infrastructure.acl;

import br.com.itau.recuperacao.renegociacao.domain.model.Divida;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Mapper responsável por converter dívidas do formato legado para o modelo de domínio.
 * Aplica regras de elegibilidade durante a conversão.
 */
@Component
public class LegadoDividaMapper {

    private static final int DIAS_ATRASO_MINIMO = 30;
    private static final BigDecimal VALOR_MINIMO_ELEGIBILIDADE = new BigDecimal("100");
    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Converte uma lista de dívidas do formato legado para o modelo de domínio.
     * Aplica regras de elegibilidade: diasAtraso >= 30 e valorAtualizado > R$100,00.
     *
     * @param legadoDividas lista de dívidas no formato do sistema legado
     * @return lista de dívidas no modelo de domínio com elegibilidade calculada
     */
    public List<Divida> mapear(List<DividaLegadoDto> legadoDividas) {
        if (legadoDividas == null) {
            return List.of();
        }

        return legadoDividas.stream()
                .map(this::converterParaDominio)
                .collect(Collectors.toList());
    }

    private Divida converterParaDominio(DividaLegadoDto dto) {
        LocalDate dataVencimento = parseData(dto.dataVencimento());
        boolean elegivel = verificarElegibilidade(dto.diasAtraso(), dto.valorAtualizado());

        return Divida.builder()
                .id(UUID.randomUUID())
                .contrato(dto.contrato())
                .valorOriginal(dto.valorOriginal())
                .valorAtualizado(dto.valorAtualizado())
                .dataVencimento(dataVencimento)
                .diasAtraso(dto.diasAtraso())
                .produto(dto.produto())
                .elegivel(elegivel)
                .build();
    }

    private boolean verificarElegibilidade(Integer diasAtraso, BigDecimal valorAtualizado) {
        if (diasAtraso == null || valorAtualizado == null) {
            return false;
        }
        return diasAtraso >= DIAS_ATRASO_MINIMO
                && valorAtualizado.compareTo(VALOR_MINIMO_ELEGIBILIDADE) > 0;
    }

    private LocalDate parseData(String data) {
        if (data == null || data.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(data, FORMATO_DATA);
        } catch (DateTimeParseException ex) {
            return LocalDate.parse(data);
        }
    }
}
