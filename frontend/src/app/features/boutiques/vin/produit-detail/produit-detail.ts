import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CurrencyPipe } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { ProduitService } from '../../../../core/services/produit.service';
import { PanierService } from '../../../../core/services/panier.service';
import { Produit } from '../../../../core/models/produit.model';

const BOUTIQUE_ID = 'miel';

@Component({
  selector: 'app-miel-produit-detail',
  standalone: true,
  imports: [RouterLink, CurrencyPipe, MatIconModule, MatButtonModule, MatSnackBarModule, TranslateModule],
  templateUrl: './produit-detail.html',
  styleUrl: './produit-detail.css'
})
export class ProduitDetail implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly produitService = inject(ProduitService);
  private readonly panierService = inject(PanierService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly translate = inject(TranslateService);

  readonly boutiqueId = BOUTIQUE_ID;
  readonly traductionPrefixe = 'produitsMiel';

  produit = signal<Produit | undefined>(undefined);
  quantite = signal(1);
  chargement = signal(true);

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.produitService.getProduitById(BOUTIQUE_ID, id).subscribe(produit => {
      this.produit.set(produit);
      this.chargement.set(false);
    });
  }

  augmenter(): void {
    this.quantite.update(q => q + 1);
  }

  diminuer(): void {
    this.quantite.update(q => Math.max(1, q - 1));
  }

  ajouterAuPanier(): void {
    const produit = this.produit();
    if (!produit) return;
    this.panierService.ajouter(BOUTIQUE_ID, produit, this.quantite());
    const nom = this.translate.instant('articles.' + produit.cle);
    const message = this.translate.instant('boutiqueMiel.ajoute', { nom });
    const voirPanier = this.translate.instant('boutiqueMiel.voirPanier');
    this.snackBar.open(message, voirPanier, { duration: 3000 })
      .onAction()
      .subscribe(() => this.router.navigate(['/boutiques', BOUTIQUE_ID, 'panier']));
  }
}
