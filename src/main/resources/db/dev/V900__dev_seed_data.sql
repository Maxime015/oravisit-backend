-- ---------------------------------------------------------------------------
-- Jeu de donnees de DEMONSTRATION - profil dev uniquement.
-- Cette migration n'est jamais chargee en production (cf. spring.flyway.locations).
-- Mots de passe : Reception@2026 / Default@2026 (a ne jamais utiliser hors dev).
-- ---------------------------------------------------------------------------

INSERT INTO employees (matricule, first_name, last_name, email, password_hash,
                       role, direction, is_active, created_at, updated_at)
VALUES
    ('REC001', 'Ama', 'Kponton', 'ama.kponton@orabank.tg',
     '$2a$10$oRZFoXhnrNtNIzmpFtwagOiBDpv0tghYEmufrXd47.YPLys8BohAe',
     'RECEPTION', 'DIRECTION_OPERATIONS', TRUE, now(), now()),
    ('REC002', 'Kodjo', 'Amegan', 'kodjo.amegan@orabank.tg',
     '$2a$10$oRZFoXhnrNtNIzmpFtwagOiBDpv0tghYEmufrXd47.YPLys8BohAe',
     'RECEPTION', 'DIRECTION_OPERATIONS', TRUE, now(), now()),
    ('EMP001', 'Afi', 'Agbodjan', 'afi.agbodjan@orabank.tg',
     '$2a$10$Acl12Q5Gp32ZTPvsZq7C6euWjI1YbwJRnMKMY1DvKJqzUm2NxSk1K',
     'DEFAULT', 'DIRECTION_SYSTEMES_INFORMATION', TRUE, now(), now()),
    ('EMP002', 'Yao', 'Mensah', 'yao.mensah@orabank.tg',
     '$2a$10$Acl12Q5Gp32ZTPvsZq7C6euWjI1YbwJRnMKMY1DvKJqzUm2NxSk1K',
     'DEFAULT', 'DIRECTION_CLIENTELE_ENTREPRISES', TRUE, now(), now()),
    ('EMP003', 'Akouvi', 'Doe', 'akouvi.doe@orabank.tg',
     '$2a$10$Acl12Q5Gp32ZTPvsZq7C6euWjI1YbwJRnMKMY1DvKJqzUm2NxSk1K',
     'DEFAULT', 'DIRECTION_CAPITAL_HUMAIN', TRUE, now(), now()),
    ('EMP004', 'Komi', 'Sodji', 'komi.sodji@orabank.tg',
     '$2a$10$Acl12Q5Gp32ZTPvsZq7C6euWjI1YbwJRnMKMY1DvKJqzUm2NxSk1K',
     'DEFAULT', 'DIRECTION_ADMINISTRATION_FINANCES', TRUE, now(), now()),
    ('EMP005', 'Sena', 'Bakayoko', 'sena.bakayoko@orabank.tg',
     '$2a$10$Acl12Q5Gp32ZTPvsZq7C6euWjI1YbwJRnMKMY1DvKJqzUm2NxSk1K',
     'DEFAULT', 'DIRECTION_JURIDIQUE_CONTENTIEUX', FALSE, now(), now())
ON CONFLICT (matricule) DO NOTHING;

INSERT INTO visitors (first_name, last_name, email, phone,
                      identity_document_type, identity_card_number,
                      is_active, created_at, updated_at)
VALUES
    ('Kossi',   'Adjenou',  'kossi.adjenou@example.tg', '+228 90 11 22 33', 'CNI',       'TG-CNI-004512', TRUE, now(), now()),
    ('Adjoa',   'Lawson',   NULL,                       '+228 91 44 55 66', 'PASSEPORT', 'TG9087123',     TRUE, now(), now()),
    ('Mawuli',  'Tettey',   'mawuli.tettey@example.tg', '+228 92 77 88 99', 'PERMIS',    'PC-2019-4471',  TRUE, now(), now()),
    ('Essoham', 'Tchalim',  NULL,                       '+228 93 10 20 30', 'CNI',       'TG-CNI-771230', TRUE, now(), now()),
    ('Rachida', 'Ouedraogo','rachida.o@example.tg',     '+226 70 12 34 56', 'CARTE_CONSULAIRE', 'CC-BF-33012', TRUE, now(), now())
ON CONFLICT (identity_document_type, identity_card_number) DO NOTHING;

-- Historique de demonstration : deux visites terminees, une annulee, une en cours.
INSERT INTO visits (visitor_id, host_employee_id, registered_by_id, direction, purpose,
                    badge_number, status, check_in_at, check_out_at, cancellation_reason,
                    created_at, updated_at)
SELECT vi.id, h.id, r.id, h.direction, s.purpose, s.badge_number, s.status,
       s.check_in_at, s.check_out_at, s.cancellation_reason, s.check_in_at, now()
FROM (VALUES
    ('TG-CNI-004512', 'EMP001', 'REC001', 'Reunion projet monetique',        'B-101', 'TERMINEE', now() - INTERVAL '3 days',  now() - INTERVAL '3 days' + INTERVAL '45 minutes', NULL),
    ('TG9087123',     'EMP002', 'REC001', 'Depot de dossier de credit',      'B-102', 'TERMINEE', now() - INTERVAL '2 days',  now() - INTERVAL '2 days' + INTERVAL '30 minutes', NULL),
    ('PC-2019-4471',  'EMP003', 'REC002', 'Entretien de recrutement',        'B-103', 'ANNULEE',  now() - INTERVAL '1 days',  NULL, 'Visiteur reparti avant la rencontre'),
    ('TG-CNI-771230', 'EMP004', 'REC001', 'Signature de convention',         'B-104', 'TERMINEE', now() - INTERVAL '4 hours', now() - INTERVAL '3 hours', NULL),
    ('CC-BF-33012',   'EMP002', 'REC002', 'Ouverture de compte entreprise',  'B-105', 'EN_COURS', now() - INTERVAL '25 minutes', NULL, NULL)
) AS s (identity_card_number, host_matricule, agent_matricule, purpose, badge_number, status,
        check_in_at, check_out_at, cancellation_reason)
JOIN visitors  vi ON vi.identity_card_number = s.identity_card_number
JOIN employees h  ON h.matricule = s.host_matricule
JOIN employees r  ON r.matricule = s.agent_matricule
WHERE NOT EXISTS (SELECT 1 FROM visits);
