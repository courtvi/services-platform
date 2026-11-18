import { Produit } from '../models/produit.model';

export const PRODUITS_VIN: Produit[] = [
  {
    id: 101,
    boutiqueId: 'vin',
    cle: 'vin_rouge',
    origine: 'Saulnes',
    prix: 7.00,
    unite: '75cl',
    categorie: 'rouge',
    tagCle: 'bestSeller',
    rupture: true
  },
  {
    id: 102,
    boutiqueId: 'vin',
    cle: 'vin_blanc',
    origine: 'Saulnes',
    prix: 7.00,
    unite: '75cl',
    categorie: 'blanc',
    tagCle: 'populaire',
    image: '/assets/images/ettiquette-2026.png',
    annee: '2026'
  },
  {
    id: 103,
    boutiqueId: 'vin',
    cle: 'cremant',
    origine: 'Saulnes',
    prix: 8.00,
    unite: '75cl',
    categorie: 'cremant',
    rupture: true
  }
];
