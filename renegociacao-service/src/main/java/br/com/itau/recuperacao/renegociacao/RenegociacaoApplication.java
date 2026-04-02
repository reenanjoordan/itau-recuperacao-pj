package br.com.itau.recuperacao.renegociacao;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Aplicação principal do serviço de Renegociação de Dívidas PJ.
 */
@SpringBootApplication
public class RenegociacaoApplication {

    public static void main(String[] args) {
        SpringApplication.run(RenegociacaoApplication.class, args);
    }
}
