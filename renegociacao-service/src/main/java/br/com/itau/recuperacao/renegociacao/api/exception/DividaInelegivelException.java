package br.com.itau.recuperacao.renegociacao.api.exception;

import java.util.List;

/**
 * Exceção lançada quando um ou mais contratos informados não são elegíveis para renegociação.
 */
public class DividaInelegivelException extends RuntimeException {

    /**
     * Cria uma exceção informando os contratos inelegíveis.
     *
     * @param contratosInelegiveis lista de identificadores de contratos não elegíveis
     */
    public DividaInelegivelException(List<String> contratosInelegiveis) {
        super("Os seguintes contratos não são elegíveis para renegociação: " + contratosInelegiveis);
    }
}
