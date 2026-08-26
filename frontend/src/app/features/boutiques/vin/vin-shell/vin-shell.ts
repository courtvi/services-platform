import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

/**
 * Coquille de la boutique "miel" : applique le thème visuel (theme-miel,
 * défini dans theme.miel.css) à toutes ses pages enfants via l'héritage
 * naturel des variables CSS. Sert de modèle pour toute future boutique.
 */
@Component({
  selector: 'app-miel-shell',
  standalone: true,
  imports: [RouterOutlet],
  template: `
    <div class="theme-miel boutique-shell">
      <router-outlet></router-outlet>
    </div>
  `,
  styles: [`
    .boutique-shell {
      background: var(--boutique-bg, #fff);
      min-height: calc(100vh - 64px);
    }
  `]
})
export class MielShell {}
