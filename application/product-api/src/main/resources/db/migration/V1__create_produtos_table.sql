CREATE TABLE produtos (
    id                  BIGSERIAL       PRIMARY KEY,
    nome                VARCHAR(255)    NOT NULL,
    descricao           TEXT,
    preco               NUMERIC(10, 2)  NOT NULL CHECK (preco > 0),
    quantidade_estoque  INTEGER         NOT NULL CHECK (quantidade_estoque >= 0),
    data_criacao        TIMESTAMP       NOT NULL DEFAULT now(),
    data_atualizacao    TIMESTAMP       NOT NULL DEFAULT now()
);

CREATE INDEX idx_produtos_nome ON produtos (nome);
