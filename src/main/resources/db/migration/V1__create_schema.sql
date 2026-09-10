-- ---------------------------------------------------------------------------
-- V1 : schema initial d'OraVisit (employes, visiteurs, visites).
-- Toutes les dates sont stockees en TIMESTAMPTZ (manipulees en Instant cote Java).
-- ---------------------------------------------------------------------------

CREATE TABLE employees (
    id            BIGSERIAL    PRIMARY KEY,
    matricule     VARCHAR(20)  NOT NULL,
    first_name    VARCHAR(60)  NOT NULL,
    last_name     VARCHAR(60)  NOT NULL,
    email         VARCHAR(120) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL,
    direction     VARCHAR(64)  NOT NULL,
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uk_employees_matricule UNIQUE (matricule),
    CONSTRAINT uk_employees_email     UNIQUE (email),
    CONSTRAINT ck_employees_role      CHECK (role IN ('ADMIN', 'RECEPTION', 'DEFAULT'))
);

COMMENT ON TABLE employees IS 'Personnel Orabank : comptes applicatifs et hotes visitables.';
COMMENT ON COLUMN employees.password_hash IS 'Hash BCrypt - jamais expose par l''API.';
COMMENT ON COLUMN employees.is_active IS 'Desactivation logique : un employe n''est jamais supprime.';

CREATE TABLE visitors (
    id                     BIGSERIAL    PRIMARY KEY,
    first_name             VARCHAR(60)  NOT NULL,
    last_name              VARCHAR(60)  NOT NULL,
    email                  VARCHAR(120),
    phone                  VARCHAR(30)  NOT NULL,
    identity_document_type VARCHAR(20)  NOT NULL,
    identity_card_number   VARCHAR(40)  NOT NULL,
    is_active              BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at             TIMESTAMPTZ  NOT NULL,
    updated_at             TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uk_visitors_identity_document UNIQUE (identity_document_type, identity_card_number),
    CONSTRAINT ck_visitors_document_type CHECK (
        identity_document_type IN ('CNI', 'PASSEPORT', 'PERMIS', 'CARTE_CONSULAIRE', 'AUTRE')
    )
);

COMMENT ON TABLE visitors IS 'Visiteurs se presentant spontanement a l''accueil.';
COMMENT ON CONSTRAINT uk_visitors_identity_document ON visitors
    IS 'L''unicite porte sur le couple type de piece + numero.';

CREATE TABLE visits (
    id                  BIGSERIAL    PRIMARY KEY,
    visitor_id          BIGINT       NOT NULL,
    host_employee_id    BIGINT       NOT NULL,
    registered_by_id    BIGINT       NOT NULL,
    direction           VARCHAR(64)  NOT NULL,
    purpose             VARCHAR(255) NOT NULL,
    badge_number        VARCHAR(20)  NOT NULL,
    status              VARCHAR(20)  NOT NULL,
    check_in_at         TIMESTAMPTZ  NOT NULL,
    check_out_at        TIMESTAMPTZ,
    cancellation_reason VARCHAR(255),
    created_at          TIMESTAMPTZ  NOT NULL,
    updated_at          TIMESTAMPTZ  NOT NULL,
    CONSTRAINT fk_visits_visitor       FOREIGN KEY (visitor_id)       REFERENCES visitors (id),
    CONSTRAINT fk_visits_host_employee FOREIGN KEY (host_employee_id) REFERENCES employees (id),
    CONSTRAINT fk_visits_registered_by FOREIGN KEY (registered_by_id) REFERENCES employees (id),
    CONSTRAINT ck_visits_status CHECK (status IN ('EN_COURS', 'TERMINEE', 'ANNULEE')),
    CONSTRAINT ck_visits_check_out_after_check_in CHECK (
        check_out_at IS NULL OR check_out_at >= check_in_at
    )
);

COMMENT ON TABLE visits IS 'Visites spontanees : creees EN_COURS, puis TERMINEE ou ANNULEE.';
COMMENT ON COLUMN visits.direction IS 'Copie figee de la direction de l''employe hote au moment de la visite.';
COMMENT ON COLUMN visits.registered_by_id IS 'Agent d''accueil authentifie ayant enregistre la visite.';
