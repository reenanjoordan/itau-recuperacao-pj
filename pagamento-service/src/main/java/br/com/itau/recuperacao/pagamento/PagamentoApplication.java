package br.com.itau.recuperacao.pagamento;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Aplicação principal do serviço de Pagamento.
 * Responsável pela geração e gestão de boletos vinculados a propostas de renegociação.
 */
@SpringBootApplication
public class PagamentoApplication {

    public static void main(String[] args) {
        SpringApplication.run(PagamentoApplication.class, args);
    }
}
