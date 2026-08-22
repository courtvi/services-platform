import { Injectable, signal } from '@angular/core';
import { Produit } from '../models/produit.model';

export interface LignePanier {
  produit: Produit;
  quantite: number;
}

/**
 * Panier d'achat, scopé par boutique (chaque boutique a son propre panier,
 * indépendant des autres). Un seul service racine gère l'état de toutes les
 * boutiques via une map interne — ainsi une nouvelle boutique n'a pas besoin
 * de dupliquer ce service, il lui suffit d'utiliser son propre boutiqueId.
 */
@Injectable({ providedIn: 'root' })
export class PanierService {
  private readonly state = signal<Record<string, LignePanier[]>>({});

  lignes(boutiqueId: string): LignePanier[] {
    return this.state()[boutiqueId] ?? [];
  }

  ajouter(boutiqueId: string, produit: Produit, quantite = 1): void {
    this.state.update(s => {
      const lignes = s[boutiqueId] ?? [];
      const existante = lignes.find(l => l.produit.id === produit.id);
      const nouvellesLignes = existante
        ? lignes.map(l => l.produit.id === produit.id ? { ...l, quantite: l.quantite + quantite } : l)
        : [...lignes, { produit, quantite }];
      return { ...s, [boutiqueId]: nouvellesLignes };
    });
  }

  modifierQuantite(boutiqueId: string, produitId: number, quantite: number): void {
    this.state.update(s => {
      const lignes = s[boutiqueId] ?? [];
      const nouvellesLignes = quantite <= 0
        ? lignes.filter(l => l.produit.id !== produitId)
        : lignes.map(l => l.produit.id === produitId ? { ...l, quantite } : l);
      return { ...s, [boutiqueId]: nouvellesLignes };
    });
  }

  supprimer(boutiqueId: string, produitId: number): void {
    this.modifierQuantite(boutiqueId, produitId, 0);
  }

  vider(boutiqueId: string): void {
    this.state.update(s => ({ ...s, [boutiqueId]: [] }));
  }

  total(boutiqueId: string): number {
    return this.lignes(boutiqueId).reduce((somme, l) => somme + l.produit.prix * l.quantite, 0);
  }

  nombreArticles(boutiqueId: string): number {
    return this.lignes(boutiqueId).reduce((somme, l) => somme + l.quantite, 0);
  }
}
