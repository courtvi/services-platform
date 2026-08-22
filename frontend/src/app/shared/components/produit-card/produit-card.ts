import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { TranslateModule } from '@ngx-translate/core';
import { Produit } from '../../../core/models/produit.model';

@Component({
  selector: 'app-produit-card',
  standalone: true,
  imports: [CurrencyPipe, RouterLink, MatIconModule, MatButtonModule, TranslateModule],
  templateUrl: './produit-card.html',
  styleUrl: './produit-card.css'
})
export class ProduitCard {
  @Input({ required: true }) produit!: Produit;
  @Input() icone = 'inventory_2';
  @Input() lienDetail: unknown[] = [];
  @Input() traductionPrefixe = 'produitsMiel';
  @Output() ajouter = new EventEmitter<Produit>();
}
