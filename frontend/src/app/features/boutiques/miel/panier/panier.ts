import { Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CurrencyPipe } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { TranslateModule } from '@ngx-translate/core';
import { PanierService } from '../../../../core/services/panier.service';

const BOUTIQUE_ID = 'miel';
const TAUX_TVA = 0.055;

@Component({
  selector: 'app-miel-panier',
  standalone: true,
  imports: [RouterLink, CurrencyPipe, MatIconModule, MatButtonModule, TranslateModule],
  templateUrl: './panier.html',
  styleUrl: './panier.css'
})
export class Panier {
  private readonly panierService = inject(PanierService);

  readonly boutiqueId = BOUTIQUE_ID;

  lignes = computed(() => this.panierService.lignes(BOUTIQUE_ID));
  total = computed(() => this.panierService.total(BOUTIQUE_ID));
  totalHT = computed(() => this.total() / (1 + TAUX_TVA));
  montantTva = computed(() => this.total() - this.totalHT());

  augmenter(produitId: number, quantiteActuelle: number): void {
    this.panierService.modifierQuantite(BOUTIQUE_ID, produitId, quantiteActuelle + 1);
  }

  diminuer(produitId: number, quantiteActuelle: number): void {
    this.panierService.modifierQuantite(BOUTIQUE_ID, produitId, quantiteActuelle - 1);
  }

  supprimer(produitId: number): void {
    this.panierService.supprimer(BOUTIQUE_ID, produitId);
  }
}
