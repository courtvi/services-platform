-- Catalogue initial (auparavant codé en dur dans le front) :
--   miel 1-6, vin 101-103, boulangerie 201-210, pains coupés (+0,10 € TTC) 215-218.
-- name = clé i18n utilisée par le front pour afficher le libellé traduit (articles.<name>).
-- price = prix HT ; HT x (1 + TVA) retombe exactement sur le prix TTC affiché par le front.
-- INSERT ... WHERE NOT EXISTS : le script peut être rejoué sans créer de doublon.

INSERT INTO produit (id, name, description, price, taux_tva) SELECT 1, 'miel_printemps', 'Miel de printemps - Saulnes - 1kg', 16.11, 5.50 WHERE NOT EXISTS (SELECT 1 FROM produit WHERE id = 1);  -- 17.00 TTC
INSERT INTO produit (id, name, description, price, taux_tva) SELECT 2, 'miel_ete', 'Miel d''été - Saulnes - 500 g', 9.00, 5.50 WHERE NOT EXISTS (SELECT 1 FROM produit WHERE id = 2);  -- 9.50 TTC
INSERT INTO produit (id, name, description, price, taux_tva) SELECT 3, 'miel_chataignier', 'Miel de châtaignier - Cévennes - 250 g', 9.38, 5.50 WHERE NOT EXISTS (SELECT 1 FROM produit WHERE id = 3);  -- 9.90 TTC
INSERT INTO produit (id, name, description, price, taux_tva) SELECT 4, 'miel_sapin', 'Miel de sapin - Vosges - 250 g', 10.33, 5.50 WHERE NOT EXISTS (SELECT 1 FROM produit WHERE id = 4);  -- 10.90 TTC
INSERT INTO produit (id, name, description, price, taux_tva) SELECT 5, 'miel_tilleul', 'Miel de tilleul - 250 g', 8.06, 5.50 WHERE NOT EXISTS (SELECT 1 FROM produit WHERE id = 5);  -- 8.50 TTC
INSERT INTO produit (id, name, description, price, taux_tva) SELECT 6, 'coffret_decouverte', 'Coffret découverte miels', 23.60, 5.50 WHERE NOT EXISTS (SELECT 1 FROM produit WHERE id = 6);  -- 24.90 TTC
INSERT INTO produit (id, name, description, price, taux_tva) SELECT 101, 'vin_rouge', 'Vin rouge - Saulnes - 75cl', 5.83, 20.00 WHERE NOT EXISTS (SELECT 1 FROM produit WHERE id = 101);  -- 7.00 TTC
INSERT INTO produit (id, name, description, price, taux_tva) SELECT 102, 'vin_blanc', 'Vin blanc - Saulnes - 75cl', 5.83, 20.00 WHERE NOT EXISTS (SELECT 1 FROM produit WHERE id = 102);  -- 7.00 TTC
INSERT INTO produit (id, name, description, price, taux_tva) SELECT 103, 'cremant', 'Crémant - Saulnes - 75cl', 6.67, 20.00 WHERE NOT EXISTS (SELECT 1 FROM produit WHERE id = 103);  -- 8.00 TTC
INSERT INTO produit (id, name, description, price, taux_tva) SELECT 201, 'petit_pain_blanc', 'Petit pain blanc', 0.62, 5.50 WHERE NOT EXISTS (SELECT 1 FROM produit WHERE id = 201);  -- 0.65 TTC
INSERT INTO produit (id, name, description, price, taux_tva) SELECT 202, 'petit_pain_cereales', 'Petit pain aux céréales', 0.62, 5.50 WHERE NOT EXISTS (SELECT 1 FROM produit WHERE id = 202);  -- 0.65 TTC
INSERT INTO produit (id, name, description, price, taux_tva) SELECT 203, 'baguette_blanche', 'Baguette blanche', 1.80, 5.50 WHERE NOT EXISTS (SELECT 1 FROM produit WHERE id = 203);  -- 1.90 TTC
INSERT INTO produit (id, name, description, price, taux_tva) SELECT 204, 'baguette_cereale', 'Baguette aux céréales', 2.84, 5.50 WHERE NOT EXISTS (SELECT 1 FROM produit WHERE id = 204);  -- 3.00 TTC
INSERT INTO produit (id, name, description, price, taux_tva) SELECT 205, 'pain_6_cereales', 'Pain 6 céréales', 3.55, 5.50 WHERE NOT EXISTS (SELECT 1 FROM produit WHERE id = 205);  -- 3.75 TTC
INSERT INTO produit (id, name, description, price, taux_tva) SELECT 206, 'pain_chef', 'Pain du chef', 3.32, 5.50 WHERE NOT EXISTS (SELECT 1 FROM produit WHERE id = 206);  -- 3.50 TTC
INSERT INTO produit (id, name, description, price, taux_tva) SELECT 207, 'pain_campagne', 'Pain de campagne', 2.84, 5.50 WHERE NOT EXISTS (SELECT 1 FROM produit WHERE id = 207);  -- 3.00 TTC
INSERT INTO produit (id, name, description, price, taux_tva) SELECT 208, 'pain_blanc', 'Pain blanc', 2.75, 5.50 WHERE NOT EXISTS (SELECT 1 FROM produit WHERE id = 208);  -- 2.90 TTC
INSERT INTO produit (id, name, description, price, taux_tva) SELECT 209, 'croissant', 'Croissant', 1.42, 5.50 WHERE NOT EXISTS (SELECT 1 FROM produit WHERE id = 209);  -- 1.50 TTC
INSERT INTO produit (id, name, description, price, taux_tva) SELECT 210, 'viennoiserie', 'Viennoiserie', 1.61, 5.50 WHERE NOT EXISTS (SELECT 1 FROM produit WHERE id = 210);  -- 1.70 TTC
INSERT INTO produit (id, name, description, price, taux_tva) SELECT 215, 'pain_6_cereales_coupe', 'Pain 6 céréales (coupé)', 3.65, 5.50 WHERE NOT EXISTS (SELECT 1 FROM produit WHERE id = 215);  -- 3.85 TTC
INSERT INTO produit (id, name, description, price, taux_tva) SELECT 216, 'pain_chef_coupe', 'Pain du chef (coupé)', 3.41, 5.50 WHERE NOT EXISTS (SELECT 1 FROM produit WHERE id = 216);  -- 3.60 TTC
INSERT INTO produit (id, name, description, price, taux_tva) SELECT 217, 'pain_campagne_coupe', 'Pain de campagne (coupé)', 2.94, 5.50 WHERE NOT EXISTS (SELECT 1 FROM produit WHERE id = 217);  -- 3.10 TTC
INSERT INTO produit (id, name, description, price, taux_tva) SELECT 218, 'pain_blanc_coupe', 'Pain blanc (coupé)', 2.84, 5.50 WHERE NOT EXISTS (SELECT 1 FROM produit WHERE id = 218);  -- 3.00 TTC

-- Fiches boutique (reprises des données du front). Miel 3 à 6 : inactifs, comme dans le front où ils sont commentés.
INSERT INTO fiche_boutique (produit_id, boutique_id, origine, unite, categorie, tag_cle, image, rupture, annee, actif) SELECT 1, 'miel', 'Saulnes', '1kg', 'fleurs', 'bestSeller', NULL, FALSE, NULL, TRUE WHERE NOT EXISTS (SELECT 1 FROM fiche_boutique WHERE boutique_id = 'miel' AND produit_id = 1);
INSERT INTO fiche_boutique (produit_id, boutique_id, origine, unite, categorie, tag_cle, image, rupture, annee, actif) SELECT 2, 'miel', 'Saulnes', '500 g', 'fleurs', 'populaire', NULL, FALSE, NULL, TRUE WHERE NOT EXISTS (SELECT 1 FROM fiche_boutique WHERE boutique_id = 'miel' AND produit_id = 2);
INSERT INTO fiche_boutique (produit_id, boutique_id, origine, unite, categorie, tag_cle, image, rupture, annee, actif) SELECT 3, 'miel', 'Cévennes', '250 g', 'foret', NULL, NULL, FALSE, NULL, FALSE WHERE NOT EXISTS (SELECT 1 FROM fiche_boutique WHERE boutique_id = 'miel' AND produit_id = 3);
INSERT INTO fiche_boutique (produit_id, boutique_id, origine, unite, categorie, tag_cle, image, rupture, annee, actif) SELECT 4, 'miel', 'Vosges', '250 g', 'foret', 'rare', NULL, FALSE, NULL, FALSE WHERE NOT EXISTS (SELECT 1 FROM fiche_boutique WHERE boutique_id = 'miel' AND produit_id = 4);
INSERT INTO fiche_boutique (produit_id, boutique_id, origine, unite, categorie, tag_cle, image, rupture, annee, actif) SELECT 5, 'miel', 'Bourgogne', '250 g', 'fleurs', NULL, NULL, FALSE, NULL, FALSE WHERE NOT EXISTS (SELECT 1 FROM fiche_boutique WHERE boutique_id = 'miel' AND produit_id = 5);
INSERT INTO fiche_boutique (produit_id, boutique_id, origine, unite, categorie, tag_cle, image, rupture, annee, actif) SELECT 6, 'miel', 'Sélection', '3 x 125 g', 'coffret', 'ideeCadeau', NULL, FALSE, NULL, FALSE WHERE NOT EXISTS (SELECT 1 FROM fiche_boutique WHERE boutique_id = 'miel' AND produit_id = 6);
INSERT INTO fiche_boutique (produit_id, boutique_id, origine, unite, categorie, tag_cle, image, rupture, annee, actif) SELECT 101, 'vin', 'Saulnes', '75cl', 'rouge', 'bestSeller', NULL, TRUE, NULL, TRUE WHERE NOT EXISTS (SELECT 1 FROM fiche_boutique WHERE boutique_id = 'vin' AND produit_id = 101);
INSERT INTO fiche_boutique (produit_id, boutique_id, origine, unite, categorie, tag_cle, image, rupture, annee, actif) SELECT 102, 'vin', 'Saulnes', '75cl', 'blanc', 'populaire', '/assets/images/ettiquette-2026.png', FALSE, '2026', TRUE WHERE NOT EXISTS (SELECT 1 FROM fiche_boutique WHERE boutique_id = 'vin' AND produit_id = 102);
INSERT INTO fiche_boutique (produit_id, boutique_id, origine, unite, categorie, tag_cle, image, rupture, annee, actif) SELECT 103, 'vin', 'Saulnes', '75cl', 'cremant', NULL, NULL, TRUE, NULL, TRUE WHERE NOT EXISTS (SELECT 1 FROM fiche_boutique WHERE boutique_id = 'vin' AND produit_id = 103);

-- Les produits créés ensuite via l'API prennent des ids à partir de 1000 (pas de collision avec les ids fixes).
ALTER TABLE produit ALTER COLUMN id RESTART WITH 1000;
