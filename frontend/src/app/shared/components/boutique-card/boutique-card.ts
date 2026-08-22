import { Component, Input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { TranslateModule } from '@ngx-translate/core';
import { Boutique } from '../../../core/models/boutique.model';

@Component({
  selector: 'app-boutique-card',
  standalone: true,
  imports: [RouterLink, MatIconModule, TranslateModule],
  templateUrl: './boutique-card.html',
  styleUrl: './boutique-card.css'
})
export class BoutiqueCard {
  @Input({ required: true }) boutique!: Boutique;
}
