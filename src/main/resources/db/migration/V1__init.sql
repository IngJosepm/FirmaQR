CREATE TABLE documento (
    id              UUID PRIMARY KEY,
    nombre_archivo  VARCHAR(255)  NOT NULL,
    pdf_actual      BYTEA         NOT NULL,
    estado          VARCHAR(20)   NOT NULL DEFAULT 'PENDIENTE',
    creado_en       TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE TABLE firmante (
    id              UUID PRIMARY KEY,
    documento_id    UUID          NOT NULL REFERENCES documento(id) ON DELETE CASCADE,
    correo          VARCHAR(255)  NOT NULL,
    token           UUID          NOT NULL UNIQUE,
    orden           INTEGER       NOT NULL,
    estado          VARCHAR(20)   NOT NULL DEFAULT 'PENDIENTE',
    firmado_en      TIMESTAMPTZ
);

CREATE INDEX idx_firmante_documento ON firmante(documento_id);
CREATE INDEX idx_firmante_token ON firmante(token);
