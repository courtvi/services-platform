import { Component, computed, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { ProduitCard } from '../../../../shared/components/produit-card/produit-card';
import { ProduitService } from '../../../../core/services/produit.service';
import { PanierService } from '../../../../core/services/panier.service';
import { Produit } from '../../../../core/models/produit.model';

const BOUTIQUE_ID = 'vin';

@Component({
  selector: 'app-vin-catalogue',
  standalone: true,
  imports: [RouterLink, MatIconModule, MatButtonModule, MatSnackBarModule, TranslateModule, ProduitCard],
  templateUrl: './catalogue.html',
  styleUrl: './catalogue.css'
})
export class Catalogue {
  private readonly produitService = inject(ProduitService);
  private readonly panierService = inject(PanierService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly router = inject(Router);
  private readonly translate = inject(TranslateService);

  readonly boutiqueId = BOUTIQUE_ID;

  readonly filtres = [
    { key: 'tous', labelCle: 'boutiquevin.filtreTous' },
    { key: 'blanc', labelCle: 'boutiquevin.filtreVinBlanc' },
    { key: 'rouge', labelCle: 'boutiquevin.filtreVinRouge' },
    { key: 'cremant', labelCle: 'boutiquevin.filtreVinCremant' }
  ];

  produits = signal<Produit[]>([]);
  filtreActif = signal('tous');

  produitsAffiches = computed(() => {
    const filtre = this.filtreActif();
    const tous = this.produits();
    return filtre === 'tous' ? tous : tous.filter(p => p.categorie === filtre);
  });

  nombreArticlesPanier = computed(() => this.panierService.nombreArticles(BOUTIQUE_ID));

  constructor() {
    this.produitService.getProduits(BOUTIQUE_ID).subscribe(produits => this.produits.set(produits));
  }

  choisirFiltre(key: string): void {
    this.filtreActif.set(key);
  }

  ajouterAuPanier(produit: Produit): void {
    this.panierService.ajouter(BOUTIQUE_ID, produit, 1);
    const nom = this.translate.instant('articles.' + produit.cle);
    const message = this.translate.instant('boutiquevin.ajoute', { nom });
    const voirPanier = this.translate.instant('boutiquevin.voirPanier');
    this.snackBar.open(message, voirPanier, { duration: 3000 })
      .onAction()
      .subscribe(() => this.router.navigate(['/boutiques', BOUTIQUE_ID, 'panier']));
  }

  lienDetail(produit: Produit): unknown[] {
    return ['/boutiques', BOUTIQUE_ID, 'produit', produit.id];
  }
}
