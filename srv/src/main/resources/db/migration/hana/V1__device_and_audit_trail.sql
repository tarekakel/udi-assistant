-- SAP HANA Cloud flavour of V1 (H2 flavour under ../h2). Same tables and constraints; HANA types: column store,
-- NVARCHAR, TIMESTAMP (HANA has no time-zone timestamp type; instants are stored normalised to UTC).
CREATE COLUMN TABLE device (
    id                  NVARCHAR(36)  NOT NULL,
    udi_di              NVARCHAR(14)  NOT NULL,
    name                NVARCHAR(200) NOT NULL,
    manufacturer        NVARCHAR(200) NOT NULL,
    risk_class          NVARCHAR(10)  NOT NULL,
    registration_status NVARCHAR(20)  NOT NULL,
    version             BIGINT        NOT NULL,
    created_at          TIMESTAMP     NOT NULL,
    created_by          NVARCHAR(100) NOT NULL,
    updated_at          TIMESTAMP     NOT NULL,
    updated_by          NVARCHAR(100) NOT NULL,
    CONSTRAINT pk_device PRIMARY KEY (id),
    CONSTRAINT uk_device_udi_di UNIQUE (udi_di)
);

-- Append-only audit trail: who changed what, when, and why (ALCOA+).
CREATE COLUMN TABLE audit_entry (
    id           NVARCHAR(36)   NOT NULL,
    entity_type  NVARCHAR(50)   NOT NULL,
    entity_id    NVARCHAR(64)   NOT NULL,
    action       NVARCHAR(20)   NOT NULL,
    field_name   NVARCHAR(50),
    old_value    NVARCHAR(1000),
    new_value    NVARCHAR(1000),
    reason       NVARCHAR(500),
    performed_by NVARCHAR(100)  NOT NULL,
    performed_at TIMESTAMP      NOT NULL,
    CONSTRAINT pk_audit_entry PRIMARY KEY (id)
);

CREATE INDEX ix_audit_entry_entity ON audit_entry (entity_type, entity_id, performed_at);
