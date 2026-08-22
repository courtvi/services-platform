import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { DecimalPipe } from '@angular/common';
import { forkJoin } from 'rxjs';
import { CommandeService } from '../../../../core/services/commande.service';
import { Commande, CommandeStatut, LigneCommande } from '../../../../core/models/commande.model';
import { Boutique } from '../../../../core/models/boutique.model';
import { BOUTIQUES } from '../../../../core/data/boutiques.config';

interface ArticleAggrege {
  article: string;
  quantiteTotale: number;
  prixUnitaire: number;
  total: number;
  commandes: string[];
}

/**
 * Vue admin des commandes d'une boutique, sur le modèle exact de AdminCommandeList
 * (features/admin/admin-commande-list) mais filtrée par préfixe de référence
 * (ex: toutes les commandes "MIEL-..." pour la boutique miel). Générique : fonctionne
 * pour n'importe quelle boutique déclarée dans boutiques.config.ts sans code additionnel.
 */
@Component({
  selector: 'app-admin-boutique-commandes',
  standalone: true,
  imports: [RouterLink, DecimalPipe],
  templateUrl: './admin-boutique-commandes.html',
  styleUrl: './admin-boutique-commandes.css'
})
export class AdminBoutiqueCommandes implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly commandeService = inject(CommandeService);

  boutique = signal<Boutique | undefined>(undefined);
  commandes = signal<Commande[]>([]);
  loading = signal(true);
  error = signal<string | null>(null);
  filtreStatut = signal<CommandeStatut | 'TOUS'>('TOUS');
  enCoursLoading = signal(false);
  darkMode = signal(true);
  commandesAvecLignes = signal<Map<number, any>>(new Map());
  commandesOuvertes = signal<Set<number>>(new Set());

  readonly statuts: (CommandeStatut | 'TOUS')[] = ['TOUS', 'CREEE', 'EN_COURS', 'LIVREE', 'ANNULEE'];

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id') ?? '';
    const boutique = BOUTIQUES.find(b => b.id === id);
    this.boutique.set(boutique);

    this.commandeService.getCommandes().subscribe({
      next: (data) => {
        const prefixe = boutique?.prefixeReference ?? '\0';
        this.commandes.set(data.filter(c => c.reference.startsWith(prefixe)));
        this.loading.set(false);
      },
      error: (e: Error) => { this.error.set(e.message); this.loading.set(false); }
    });
  }

  get commandesFiltrees(): Commande[] {
    const filtre = this.filtreStatut();
    return filtre === 'TOUS'
      ? this.commandes()
      : this.commandes().filter(c => c.statut === filtre);
  }

  get articlesAgreges(): ArticleAggrege[] {
    const map = this.commandesAvecLignes();
    if (map.size === 0) return [];

    const agregation = new Map<string, ArticleAggrege>();

    map.forEach((detail) => {
      const reference = detail.commande.reference;
      (detail.lignes as LigneCommande[]).forEach(ligne => {
        const existing = agregation.get(ligne.article);
        if (existing) {
          existing.quantiteTotale += ligne.quantite;
          existing.total += ligne.total;
          if (!existing.commandes.includes(reference)) {
            existing.commandes.push(reference);
          }
        } else {
          agregation.set(ligne.article, {
            article: ligne.article,
            quantiteTotale: ligne.quantite,
            prixUnitaire: ligne.prixUnitaire,
            total: ligne.total,
            commandes: [reference]
          });
        }
      });
    });

    return Array.from(agregation.values());
  }

  get totalGeneral(): number {
    return this.articlesAgreges.reduce((acc, a) => acc + a.total, 0);
  }

  compterParStatut(statut: CommandeStatut | 'TOUS'): number {
    return statut === 'TOUS'
      ? this.commandes().length
      : this.commandes().filter(c => c.statut === statut).length;
  }

  setFiltre(statut: CommandeStatut | 'TOUS'): void {
    this.filtreStatut.set(statut);
    this.commandesOuvertes.set(new Set());

    if (statut === 'CREEE') {
      const crees = this.commandes().filter(c => c.statut === 'CREEE');
      if (crees.length === 0) return;
      forkJoin(crees.map(c => this.commandeService.getCommandeById(c.id)))
        .subscribe(details => {
          const map = new Map<number, any>();
          details.forEach(d => map.set(d.commande.id, d));
          this.commandesAvecLignes.set(map);
        });
    } else {
      this.commandesAvecLignes.set(new Map());
    }
  }

  toggleDetail(id: number): void {
    const ouvertes = new Set(this.commandesOuvertes());
    if (ouvertes.has(id)) {
      ouvertes.delete(id);
    } else {
      ouvertes.add(id);
    }
    this.commandesOuvertes.set(ouvertes);
  }

  isOuverte(id: number): boolean {
    return this.commandesOuvertes().has(id);
  }

  formatDate(date: string): string {
    return new Date(date).toLocaleDateString('fr-FR', {
      day: '2-digit', month: '2-digit', year: 'numeric',
      hour: '2-digit', minute: '2-digit'
    });
  }

  passerToutesEnCours(): void {
    const commandesCrees = this.commandes().filter(c => c.statut === 'CREEE');
    if (commandesCrees.length === 0) return;

    this.enCoursLoading.set(true);

    forkJoin(commandesCrees.map(c => this.commandeService.passerEnCours(c.id)))
      .subscribe({
        next: (updated) => {
          this.commandes.update(liste =>
            liste.map(c => updated.find(u => u.id === c.id) ?? c)
          );
          this.commandesAvecLignes.set(new Map());
          this.commandesOuvertes.set(new Set());
          this.enCoursLoading.set(false);
        },
        error: (e: Error) => {
          this.error.set(e.message);
          this.enCoursLoading.set(false);
        }
      });
  }

  toggleDarkMode(): void {
    this.darkMode.update(v => !v);
  }
}
