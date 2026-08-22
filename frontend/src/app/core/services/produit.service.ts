import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { Produit } from '../models/produit.model';
import { PRODUITS_MIEL } from '../data/produits.miel.data';

/**
 * Catalogue produits par boutique.
 *
 * Implémentation actuelle : données statiques en mémoire, exposées via
 * Observable pour que l'interface des composants ne change pas le jour où
 * ces données viendront d'un vrai product-service HTTP (il suffira alors de
 * remplacer le corps de ces méthodes par des appels this.http.get(...)).
 */
@Injectable({ providedIn: 'root' })
export class ProduitService {
  private readonly catalogues: Record<string, Produit[]> = {
    miel: PRODUITS_MIEL
  };

  getProduits(boutiqueId: string): Observable<Produit[]> {
    return of(this.catalogues[boutiqueId] ?? []);
  }

  getProduitsParCategorie(boutiqueId: string, categorie: string): Observable<Produit[]> {
    const tous = this.catalogues[boutiqueId] ?? [];
    const filtres = categorie === 'tous' ? tous : tous.filter(p => p.categorie === categorie);
    return of(filtres);
  }

  getProduitById(boutiqueId: string, id: number): Observable<Produit | undefined> {
    return of((this.catalogues[boutiqueId] ?? []).find(p => p.id === id));
  }
}
