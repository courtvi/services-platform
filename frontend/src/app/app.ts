import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatDividerModule } from '@angular/material/divider';
import Keycloak from 'keycloak-js';
import { TranslateModule, TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    RouterOutlet, RouterLink, RouterLinkActive,
    MatToolbarModule, MatButtonModule,
    MatIconModule, MatMenuModule, MatDividerModule,
    TranslateModule
  ],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App implements OnInit {
  private readonly keycloak = inject(Keycloak) as Keycloak;
  private readonly translate = inject(TranslateService);

  isLoggedIn = signal(false);
  username = signal('');
  isAdmin = signal(false);

  async ngOnInit(): Promise<void> {
    const savedLang = localStorage.getItem('lang') || 'fr';
    this.translate.use(savedLang);
    this.isLoggedIn.set(this.keycloak.authenticated ?? false);
    if (this.isLoggedIn()) {
      const profile = await this.keycloak.loadUserProfile();
      this.username.set(profile.firstName ?? profile.username ?? 'Utilisateur');
      this.isAdmin.set(this.keycloak.hasResourceRole('ADMIN'));
    }
  }

  async logout(): Promise<void> {
        await this.keycloak.logout({ redirectUri: window.location.origin });
  }

  setLang(lang: string) {
    localStorage.setItem('lang', lang);
    this.translate.use(lang);
  }
}
