import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { BOUTIQUES } from '../../../../core/data/boutiques.config';

@Component({
  selector: 'app-admin-boutiques-accueil',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './admin-boutiques-accueil.html',
  styleUrl: './admin-boutiques-accueil.css'
})
export class AdminBoutiquesAccueil {
  readonly boutiques = BOUTIQUES;
}
