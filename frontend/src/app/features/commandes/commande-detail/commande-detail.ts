import { Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { CommonModule, CurrencyPipe } from '@angular/common';
import { CommandeService } from '../../../core/services/commande.service';
import { CommandeAvecLignes } from '../../../core/models/commande.model';
import { TranslateModule } from '@ngx-translate/core';


@Component({
  selector: 'app-commande-detail',
  standalone: true,
  imports: [CommonModule, TranslateModule, CurrencyPipe],
  templateUrl: './commande-detail.html',
  styles: ``
})
export class CommandeDetail implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly commandeService = inject(CommandeService);

  commande: CommandeAvecLignes | null = null;
  loading = true;
  error = false;

  readonly TVA_RATE = 0.03; // 3%

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.commandeService.getCommandeById(id).subscribe({
      next: (data) => {
        this.commande = data;
        this.loading = false;
      },
      error: () => {
        this.error = true;
        this.loading = false;
      }
    });
  }

  getTva(total: number): number {
    return total * this.TVA_RATE;
  }

  getTotalCommande(): number {
    return this.commande?.lignes.reduce((acc, ligne) => acc + ligne.total, 0) ?? 0;
  }

  getTotalHT(): number {
    return this.getTotalCommande() / (1 + this.TVA_RATE);
  }

  getTotalTva(): number {
    return this.getTotalCommande() * this.TVA_RATE / (1 + this.TVA_RATE);
  }
}
