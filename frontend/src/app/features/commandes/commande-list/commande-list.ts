import { Component, inject, OnInit, signal } from '@angular/core';
import { Router, NavigationEnd } from '@angular/router';
import { filter } from 'rxjs/operators';
import { DatePipe } from '@angular/common';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Commande } from '../../../core/models/commande.model';
import { CommandeService } from '../../../core/services/commande.service';
import { TranslateModule } from '@ngx-translate/core';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  selector: 'app-commande-list',
  standalone: true,
  imports: [
    DatePipe, MatTableModule, MatButtonModule, MatIconModule,
    MatChipsModule, MatProgressSpinnerModule,
    MatSnackBarModule, MatTooltipModule, TranslateModule
  ],
  templateUrl: './commande-list.html',
  styles: [`
    .container { max-width: 1000px; margin: 2rem auto; padding: 0 1rem; }
    .header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1.5rem; }
    .spinner-container { display: flex; justify-content: center; padding: 3rem; }
    mat-table { width: 100%; background: white; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
  `]
})
export class CommandeList implements OnInit {
  private readonly commandeService = inject(CommandeService);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);

  commandes = signal<Commande[]>([]);

  loading = signal(true);
  error = signal<string | null>(null);
  displayedColumns = ['reference', 'statut', 'dateCommande', 'actions'];

  ngOnInit(): void {
    this.loadCommandes();
    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd)
    ).subscribe((event: any) => {
      if (event.urlAfterRedirects === '/commandes') {
        this.loadCommandes();
      }
    });
  }

  loadCommandes(): void {
    this.loading.set(true);
    this.commandeService.getCommandes().subscribe({
      next: data => { this.commandes.set(data); this.loading.set(false); },
      error: () => { this.error.set('Erreur de chargement'); this.loading.set(false); }
    });
  }

  voirDetail(id: number): void { this.router.navigate(['/commandes', id]); }
  nouvelleCommande(): void { this.router.navigate(['/commandes', 'nouvelle']); }

  annuler(id: number): void {
    this.commandeService.annulerCommande(id).subscribe({
      next: () => { this.snackBar.open('Commande annulée', 'Fermer', { duration: 3000 }); this.loadCommandes(); },
      error: () => { this.snackBar.open('Impossible d\'annuler', 'Fermer', { duration: 3000 }); }
    });
  }

  statutColor(statut: string): string {
    const colors: Record<string, string> = {
      'CREEE': 'primary', 'EN_COURS': 'accent', 'LIVREE': 'primary', 'ANNULEE': 'warn'
    };
    return colors[statut] ?? 'primary';
  }
}
