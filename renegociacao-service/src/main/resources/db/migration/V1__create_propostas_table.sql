CREATE TABLE propostas (
    id UUID PRIMARY KEY,
    cpf_cnpj VARCHAR(18) NOT NULL,
    status VARCHAR(20) NOT NULL,
    valor_total NUMERIC(15,2),
    valor_negociado NUMERIC(15,2),
    percentual_desconto NUMERIC(5,2),
    numero_parcelas INT,
    valor_parcela NUMERIC(15,2),
    tipo_acordo VARCHAR(30),
    criada_em TIMESTAMP NOT NULL DEFAULT NOW(),
    atualizada_em TIMESTAMP,
    versao BIGINT DEFAULT 0,
    CONSTRAINT chk_propostas_status CHECK (status IN ('SIMULADA', 'PENDENTE', 'EFETIVADA', 'CANCELADA'))
);

CREATE INDEX idx_propostas_cpf_cnpj ON propostas(cpf_cnpj);
CREATE INDEX idx_propostas_status ON propostas(status);
CREATE INDEX idx_propostas_criada_em ON propostas(criada_em);
