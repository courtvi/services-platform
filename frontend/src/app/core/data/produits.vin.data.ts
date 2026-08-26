import { Produit } from '../models/produit.model';

export const PRODUITS_MIEL: Produit[] = [
  {
    id: 1,
    boutiqueId: 'miel',
    cle: 'miel_printemps',
    origine: 'Saulnes',
    prix: 17.00,
    unite: '1kg',
    categorie: 'fleurs',
    tagCle: 'bestSeller'
  },
  {
    id: 2,
    boutiqueId: 'miel',
    cle: 'miel_ete',
    origine: 'Saulnes',
    prix: 9.50,
    unite: '500 g',
    categorie: 'fleurs',
    tagCle: 'populaire'
  },
 /* {
    id: 3,
    boutiqueId: 'miel',
    cle: 'miel_chataignier',
    origine: 'Cévennes',
    prix: 9.90,
    unite: '250 g',
    categorie: 'foret'
  },
  {
    id: 4,
    boutiqueId: 'miel',
    cle: 'miel_sapin',
    origine: 'Vosges',
    prix: 10.90,
    unite: '250 g',
    categorie: 'foret',
    tagCle: 'rare'
  },
  {
    id: 5,
    boutiqueId: 'miel',
    cle: 'miel_tilleul',
    origine: 'Bourgogne',
    prix: 8.50,
    unite: '250 g',
    categorie: 'fleurs'
  },
  {
    id: 6,
    boutiqueId: 'miel',
    cle: 'coffret_decouverte',
    origine: 'Sélection',
    prix: 24.90,
    unite: '3 x 125 g',
    categorie: 'coffret',
    tagCle: 'ideeCadeau'
  }
*/
];
