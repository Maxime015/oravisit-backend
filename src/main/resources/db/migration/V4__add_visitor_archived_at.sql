-- ---------------------------------------------------------------------------
-- V4 : date d'archivage d'un visiteur.
--
-- updated_at ne pouvait pas jouer ce role : il change a chaque modification de
-- la fiche. Une colonne dediee permet d'afficher « archive le ... » dans la vue
-- des fiches archivees.
-- ---------------------------------------------------------------------------

ALTER TABLE visitors ADD COLUMN archived_at TIMESTAMPTZ;

COMMENT ON COLUMN visitors.archived_at
    IS 'Date d''archivage logique ; NULL tant que la fiche est active.';

-- Reprise des fiches deja archivees avant cette migration : faute de mieux,
-- la derniere modification connue fait office de date d'archivage.
UPDATE visitors
SET archived_at = updated_at
WHERE is_active = FALSE
  AND archived_at IS NULL;

CREATE INDEX ix_visitors_archived_at ON visitors (archived_at);
