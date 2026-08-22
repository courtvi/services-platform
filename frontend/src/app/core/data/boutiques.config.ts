import { Boutique } from '../models/boutique.model';

/**
 * Registre des boutiques disponibles sur la plateforme.
 *
 * Pour ajouter une nouvelle boutique :
 *  1. Créer le dossier src/app/features/boutiques/<id>/ sur le modèle de "miel"
 *     (un *-shell, un catalogue, un produit-detail, un panier, un checkout).
 *  2. Créer son fichier de thème theme.<id>.css et l'importer dans src/styles.css.
 *  3. Ajouter les routes correspondantes (client ET admin) dans app.routes.ts.
 *  4. Ajouter ses clés de traduction (boutiques.<id>.description, produits<Id>.*) dans les
 *     4 fichiers src/assets/i18n/*.json.
 *  5. Déclarer la boutique ci-dessous — elle apparaîtra automatiquement sur la page d'accueil
 *     des boutiques (/boutiques) et dans l'admin (/admin/boutiques).
 */
export const BOUTIQUES: Boutique[] = [
  {
    id: 'miel',
    nom: 'Chabeille',
    descriptionCle: 'boutiques.miel.description',
    theme: 'theme-miel',
    icone: 'emoji_nature',
    prefixeReference: 'MIEL-',
    actif: true
  }

  // Exemple pour la prochaine boutique :
  // {
  //   id: 'fromage',
  //   nom: 'La Fromagerie du Val',
  //   descriptionCle: 'boutiques.fromage.description',
  //   theme: 'theme-fromage',
  //   icone: 'restaurant',
  //   prefixeReference: 'FROMAGE-',
  //   actif: true
  // }
];
