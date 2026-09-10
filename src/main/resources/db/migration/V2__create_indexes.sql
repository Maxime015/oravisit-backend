-- ---------------------------------------------------------------------------
-- V2 : index de recherche et garanties d'unicite metier.
-- ---------------------------------------------------------------------------

CREATE INDEX ix_visits_status           ON visits (status);
CREATE INDEX ix_visits_check_in_at      ON visits (check_in_at);
CREATE INDEX ix_visits_visitor_id       ON visits (visitor_id);
CREATE INDEX ix_visits_host_employee_id ON visits (host_employee_id);
CREATE INDEX ix_visits_direction        ON visits (direction);

-- Regle §5.1 : un badge ne peut equiper qu'une seule visite en cours.
-- Garantie au niveau du SGBD pour resister aux acces concurrents.
CREATE UNIQUE INDEX ux_visits_badge_active
    ON visits (badge_number)
    WHERE status = 'EN_COURS';

-- Regle §5.2 : un visiteur ne peut avoir qu'une seule visite en cours.
CREATE UNIQUE INDEX ux_visits_visitor_active
    ON visits (visitor_id)
    WHERE status = 'EN_COURS';

-- Recherche insensible a la casse sur les visiteurs (parametre q).
CREATE INDEX ix_visitors_last_name_lower  ON visitors (LOWER(last_name));
CREATE INDEX ix_visitors_first_name_lower ON visitors (LOWER(first_name));
CREATE INDEX ix_visitors_phone            ON visitors (phone);
CREATE INDEX ix_visitors_identity_number_lower ON visitors (LOWER(identity_card_number));

-- Recherche et filtres sur les employes.
CREATE INDEX ix_employees_direction ON employees (direction);
CREATE INDEX ix_employees_role      ON employees (role);
