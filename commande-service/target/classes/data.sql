INSERT INTO COMMANDES (USER_ID, REFERENCE, STATUT, DATE_COMMANDE, DATE_LIVRAISON) VALUES
('user-alice', 'CMD-ALICE-001', 'CREEE',    CURRENT_TIMESTAMP,                    CURRENT_TIMESTAMP + INTERVAL '1' DAY),
('user-alice', 'CMD-ALICE-002', 'LIVREE',   '2026-05-10 09:00:00',                '2026-05-15 09:00:00'),
('user-bob',   'CMD-BOB-001',   'EN_COURS', CURRENT_TIMESTAMP - INTERVAL '1' DAY, CURRENT_TIMESTAMP),
('user-bob',   'CMD-BOB-002',   'ANNULEE',  '2026-05-12 11:00:00',                '2026-05-18 11:00:00'),
('user-alice', 'CMD-ALICE-003', 'CREEE',    CURRENT_TIMESTAMP,                    CURRENT_TIMESTAMP + INTERVAL '1' DAY),
('user-alice', 'CMD-ALICE-004', 'CREEE',    CURRENT_TIMESTAMP,                    CURRENT_TIMESTAMP + INTERVAL '1' DAY),
('user-bob',   'CMD-BOB-003',   'CREEE',    CURRENT_TIMESTAMP,                    CURRENT_TIMESTAMP + INTERVAL '1' DAY),
('user-bob',   'CMD-BOB-004',   'CREEE',    CURRENT_TIMESTAMP,                    CURRENT_TIMESTAMP + INTERVAL '1' DAY),
('user-alice', 'CMD-ALICE-005', 'CREEE',    CURRENT_TIMESTAMP,                    CURRENT_TIMESTAMP + INTERVAL '1' DAY);

INSERT INTO LIGNECOMMANDES (COMMANDE_ID, ARTICLE, QUANTITE, PRIX_UNITAIRE, TOTAL) VALUES
-- CMD-ALICE-001 (id=1)
(1, 'Baguette',     2, 1.10,  2.20),
(1, 'Croissant',    3, 1.50,  4.50),
-- CMD-ALICE-002 (id=2)
(2, 'Pain complet', 1, 2.00,  2.00),
-- CMD-BOB-001 (id=3)
(3, 'Brioche',      2, 3.50,  7.00),
(3, 'Eclair',       4, 2.80, 11.20),
-- CMD-BOB-002 (id=4)
(4, 'Macaron',      6, 1.80, 10.80),
-- CMD-ALICE-003 (id=5)
(5, 'Baguette',     4, 1.10,  4.40),
(5, 'Croissant',    2, 1.50,  3.00),
(5, 'Eclair',       3, 2.80,  8.40),
-- CMD-ALICE-004 (id=6)
(6, 'Baguette',     1, 1.10,  1.10),
(6, 'Macaron',      4, 1.80,  7.20),
(6, 'Brioche',      1, 3.50,  3.50),
-- CMD-BOB-003 (id=7)
(7, 'Croissant',    5, 1.50,  7.50),
(7, 'Pain complet', 2, 2.00,  4.00),
(7, 'Baguette',     3, 1.10,  3.30),
-- CMD-BOB-004 (id=8)
(8, 'Eclair',       2, 2.80,  5.60),
(8, 'Croissant',    4, 1.50,  6.00),
(8, 'Macaron',      3, 1.80,  5.40),
-- CMD-ALICE-005 (id=9)
(9, 'Baguette',     6, 1.10,  6.60),
(9, 'Brioche',      2, 3.50,  7.00),
(9, 'Eclair',       1, 2.80,  2.80),
(9, 'Croissant',    3, 1.50,  4.50);