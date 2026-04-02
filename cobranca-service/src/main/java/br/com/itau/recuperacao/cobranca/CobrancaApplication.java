package br.com.itau.recuperacao.cobranca;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Aplicação principal do serviço de Cobrança.
 * Responsável por gerenciar ações de cobrança sobre dívidas PJ em recuperação.
 */
@SpringBootApplication
public class CobrancaApplication {

    public static void main(String[] args) {
        SpringApplication.run(CobrancaApplication.class, args);
    }
}
