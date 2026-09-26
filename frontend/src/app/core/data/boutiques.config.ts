import { Boutique } from '../models/boutique.model';

export const BOUTIQUES: Boutique[] = [
  {
    id: 'miel',
    nom: 'Chabeille',
    descriptionCle: 'boutiques.miel.description',
    theme: 'theme-miel',
    icone: 'emoji_nature',
    prefixeReference: 'MIEL-',
    actif: true
  },

   {
     id: 'vin',
     nom: 'La cuvée de Saulnes',
     descriptionCle: 'boutiques.vin.description',
     theme: 'theme-vin',
     icone: 'restaurant',
     prefixeReference: 'VIN-',
     actif: true
   }
];
