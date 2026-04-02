CREATE TABLE parcelas (
    id UUID PRIMARY KEY,
    proposta_id UUID,
    numero_parcela INT NOT NULL,
    valor NUMERIC(15,2) NOT NULL,
    data_vencimento DATE NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDENTE',
    CONSTRAINT fk_parcelas_proposta FOREIGN KEY (proposta_id) REFERENCES propostas(id) ON DELETE CASCADE
);

CREATE INDEX idx_parcelas_proposta_id ON parcelas(proposta_id);
