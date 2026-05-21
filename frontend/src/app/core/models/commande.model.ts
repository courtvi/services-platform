export interface LigneCommande {
  id: number;
  article: string;
  quantite: number;
  prixUnitaire: number;
  total: number;
}

export interface Commande {
  id: number;
  userId: string;
  reference: string;
  statut: CommandeStatut;
  dateCommande: string;
  dateLivraison: string;
  total: number;
}

export type CommandeStatut = 'CREEE' | 'EN_COURS' | 'LIVREE' | 'ANNULEE';

export interface LigneCommandeRequest {
  article: string;
  quantite: number;
  prixUnitaire: number;
  total: number;
}

export interface CommandeRequest {
  reference: string;
  dateCommande: string;
  dateLivraison: string;
  lignes: LigneCommandeRequest[];
}

export interface CommandeAvecLignes {
  commande: Commande;
  lignes: LigneCommande[];
}