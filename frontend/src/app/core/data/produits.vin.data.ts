import { Produit } from '../models/produit.model';

export const PRODUITS_VIN: Produit[] = [
  {
    id: 1,
    boutiqueId: 'vin',
    cle: 'vin_rouge',
    origine: 'Saulnes',
    prix: 7.00,
    unite: '75cl',
    categorie: 'bouteille',
    tagCle: 'bestSeller'
  },
  {
    id: 2,
    boutiqueId: 'vin',
    cle: 'vin_blanc',
    origine: 'Saulnes',
    prix: 7.00,
    unite: '75cl',
    categorie: 'bouteille',
    tagCle: 'populaire'
  },
  {
    id: 3,
    boutiqueId: 'vin',
    cle: 'cremant',
    origine: 'Saulnes',
    prix: 8.00,
    unite: '250 g',
    categorie: 'bouteille'
  }
];
