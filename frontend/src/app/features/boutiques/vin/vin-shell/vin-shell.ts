import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-vin-shell',
  standalone: true,
  imports: [RouterOutlet],
  template: `
    <div class="theme-vin boutique-shell">
      <router-outlet></router-outlet>
    </div>
  `,
  styles: [`
    .boutique-shell {
      --boutique-primary: #7b1e3a;
      --boutique-bg: #fbf8f6;
      --boutique-surface: #ffffff;
      --boutique-line: #eadfdc;
      --boutique-ink: #2a1a1f;
      --boutique-ink-soft: #7a6166;
      --boutique-font-title: inherit;

      background: var(--boutique-bg);
      min-height: calc(100vh - 64px);
    }
  `]
})
export class vinShell {}
