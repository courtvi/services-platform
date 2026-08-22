import { Component } from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';
import { BoutiqueCard } from '../../../shared/components/boutique-card/boutique-card';
import { BOUTIQUES } from '../../../core/data/boutiques.config';

@Component({
  selector: 'app-boutiques-accueil',
  standalone: true,
  imports: [BoutiqueCard, TranslateModule],
  templateUrl: './boutiques-accueil.html',
  styleUrl: './boutiques-accueil.css'
})
export class BoutiquesAccueil {
  readonly boutiques = BOUTIQUES.filter(b => b.actif);
}
