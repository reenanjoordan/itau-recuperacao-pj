CREATE TABLE dividas (
    id UUID PRIMARY KEY,
    proposta_id UUID,
    contrato VARCHAR(50),
    valor_original NUMERIC(15,2),
    valor_atualizado NUMERIC(15,2),
    data_vencimento DATE,
    dias_atraso INT,
    produto VARCHAR(100),
    CONSTRAINT fk_dividas_proposta FOREIGN KEY (proposta_id) REFERENCES propostas(id) ON DELETE CASCADE
);

CREATE INDEX idx_dividas_proposta_id ON dividas(proposta_id);
CREATE INDEX idx_dividas_contrato ON dividas(contrato);
