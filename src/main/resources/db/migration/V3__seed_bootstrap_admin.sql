-- ---------------------------------------------------------------------------
-- V3 : amorcage du premier compte ADMIN.
--
-- Le matricule et le hash BCrypt proviennent des variables d'environnement
-- APP_BOOTSTRAP_ADMIN_MATRICULE / APP_BOOTSTRAP_ADMIN_PASSWORD_HASH, injectees
-- comme placeholders Flyway. Si l'une des deux est absente, aucune ligne n'est
-- creee (le profil dev fournit des valeurs de demonstration).
-- ---------------------------------------------------------------------------

INSERT INTO employees (matricule, first_name, last_name, email, password_hash,
                       role, direction, is_active, created_at, updated_at)
SELECT '${bootstrapAdminMatricule}',
       '${bootstrapAdminFirstName}',
       '${bootstrapAdminLastName}',
       '${bootstrapAdminEmail}',
       '${bootstrapAdminPasswordHash}',
       'ADMIN',
       '${bootstrapAdminDirection}',
       TRUE,
       now(),
       now()
WHERE '${bootstrapAdminMatricule}' <> ''
  AND '${bootstrapAdminPasswordHash}' <> ''
ON CONFLICT (matricule) DO NOTHING;
