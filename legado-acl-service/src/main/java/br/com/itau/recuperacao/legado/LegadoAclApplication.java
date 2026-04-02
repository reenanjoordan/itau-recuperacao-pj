package br.com.itau.recuperacao.legado;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Aplicação principal do serviço Anti-Corruption Layer (ACL) para o legado.
 * Atua como camada de tradução entre os microsserviços modernos e o
 * mainframe legado (COBOL/DB2/VSAM), isolando o domínio de negócio
 * das complexidades do sistema antigo.
 */
@SpringBootApplication
public class LegadoAclApplication {

    public static void main(String[] args) {
        SpringApplication.run(LegadoAclApplication.class, args);
    }
}
