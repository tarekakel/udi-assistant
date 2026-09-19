-- Devices identified by UDI-DI (GTIN-14). Records are never deleted; WITHDRAWN is the terminal status.
CREATE TABLE device (
    id                  UUID                        NOT NULL,
    udi_di              VARCHAR(14)                 NOT NULL,
    name                VARCHAR(200)                NOT NULL,
    manufacturer        VARCHAR(200)                NOT NULL,
    risk_class          VARCHAR(10)                 NOT NULL,
    registration_status VARCHAR(20)                 NOT NULL,
    version             BIGINT                      NOT NULL,
    created_at          TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    created_by          VARCHAR(100)                NOT NULL,
    updated_at          TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_by          VARCHAR(100)                NOT NULL,
    CONSTRAINT pk_device PRIMARY KEY (id),
    CONSTRAINT uk_device_udi_di UNIQUE (udi_di)
);

-- Append-only audit trail: who changed what, when, and why (ALCOA+).
CREATE TABLE audit_entry (
    id           UUID                        NOT NULL,
    entity_type  VARCHAR(50)                 NOT NULL,
    entity_id    VARCHAR(64)                 NOT NULL,
    action       VARCHAR(20)                 NOT NULL,
    field_name   VARCHAR(50),
    old_value    VARCHAR(1000),
    new_value    VARCHAR(1000),
    reason       VARCHAR(500),
    performed_by VARCHAR(100)                NOT NULL,
    performed_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_audit_entry PRIMARY KEY (id)
);

CREATE INDEX ix_audit_entry_entity ON audit_entry (entity_type, entity_id, performed_at);
