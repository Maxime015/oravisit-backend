-- ---------------------------------------------------------------------------
-- V5 : numero de telephone d'un employe.
--
-- Colonne facultative : les comptes existants n'en ont pas, et l'exiger
-- empecherait toute modification de leur fiche tant qu'il n'est pas renseigne.
-- ---------------------------------------------------------------------------

ALTER TABLE employees ADD COLUMN phone VARCHAR(30);

COMMENT ON COLUMN employees.phone IS 'Telephone professionnel ; facultatif.';
