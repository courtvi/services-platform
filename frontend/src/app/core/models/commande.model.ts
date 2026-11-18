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
  numeroClient: string;
  reference: string;
  statut: CommandeStatut;
  dateCommande: string;
  dateLivraison: string;
  total: number;
}

export type CommandeStatut = 'CREEE' | 'EN_COURS' | 'LIVREE' | 'ANNULEE';

export interface LigneCommandeRequest {
  // Seuls le produit et la quantité sont envoyés : le nom et le prix sont lus par
  // commande-service dans produit-service (le client ne peut plus fixer son prix).
  produitId: number;
  quantite: number;
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
