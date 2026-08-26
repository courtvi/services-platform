import { Component, computed, inject, OnInit } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { CurrencyPipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { NgxPayPalModule, IPayPalConfig, ICreateOrderRequest } from 'ngx-paypal';
import { PanierService } from '../../../../core/services/panier.service';
import { CommandeService } from '../../../../core/services/commande.service';
import { CommandeRequest } from '../../../../core/models/commande.model';
import { environment } from '../../../../environments/environment';

const BOUTIQUE_ID = 'miel';
const TAUX_TVA = 0.055; // TVA réduite alimentaire (5,5%) — les prix affichés sont TTC

@Component({
  selector: 'app-miel-checkout',
  standalone: true,
  imports: [
    RouterLink, CurrencyPipe, ReactiveFormsModule, TranslateModule,
    MatIconModule, MatButtonModule, MatFormFieldModule, MatInputModule,
    MatSnackBarModule, NgxPayPalModule
  ],
  templateUrl: './checkout.html',
  styleUrl: './checkout.css'
})
export class Checkout implements OnInit {
  private readonly panierService = inject(PanierService);
  private readonly commandeService = inject(CommandeService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly translate = inject(TranslateService);

  readonly boutiqueId = BOUTIQUE_ID;

  lignes = computed(() => this.panierService.lignes(BOUTIQUE_ID));
  total = computed(() => this.panierService.total(BOUTIQUE_ID));
  totalHT = computed(() => this.total() / (1 + TAUX_TVA));
  montantTva = computed(() => this.total() - this.totalHT());

  loading = false;
  payPalConfig?: IPayPalConfig;

  form = this.fb.group({
    dateLivraison: [this.getTomorrow(), Validators.required]
  });

  ngOnInit(): void {
    if (this.lignes().length === 0) {
      this.router.navigate(['/boutiques', BOUTIQUE_ID]);
    }
  }

  getTomorrow(): string {
    const demain = new Date();
    demain.setDate(demain.getDate() + 1);
    return demain.toISOString().split('T')[0];
  }

  generateReference(): string {
    const now = new Date();
    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, '0');
    const day = String(now.getDate()).padStart(2, '0');
    const random = Math.floor(1000 + Math.random() * 9000);
    return `MIEL-${year}${month}${day}-${random}`;
  }

  initPayPal(): void {
    if (this.form.invalid) return;
    const total = this.total().toFixed(2);

    this.payPalConfig = {
      currency: 'EUR',
      clientId: environment.sites['chabeille'].paypal.clientId,
      createOrderOnClient: () => ({
        intent: 'CAPTURE',
        purchase_units: [{
          amount: {
            currency_code: 'EUR',
            value: total
          }
        }]
      } as ICreateOrderRequest),
      advanced: { commit: 'true' },
      style: { label: 'paypal', layout: 'vertical' },
      onApprove: (data, actions) => {
        actions.order.capture().then(() => this.submit());
      },
      onError: err => {
        console.error('PayPal error', err);
        this.snackBar.open(this.translate.instant('boutiqueMiel.erreurPaypal'), 'OK', { duration: 3000 });
      }
    };
  }

  submit(): void {
    this.loading = true;

    const payload: CommandeRequest = {
      reference: this.generateReference(),
      dateCommande: new Date().toISOString().slice(0, 19),
      dateLivraison: this.form.value.dateLivraison + 'T00:00:00',
      // "article" reprend la clé i18n (comme pour le pain, ex. "petit_pain_blanc") — cela permet
      // à l'écran générique /commandes/:id d'afficher le nom traduit via 'articles.' + article,
      // quelle que soit la langue choisie par le client au moment de la commande.
      lignes: this.lignes().map(l => ({
        article: l.produit.cle,
        quantite: l.quantite,
        prixUnitaire: l.produit.prix,
        total: l.produit.prix * l.quantite
      }))
    };

    this.commandeService.createCommande(payload).subscribe({
      next: () => {
        this.panierService.vider(BOUTIQUE_ID);
        this.snackBar.open(this.translate.instant('boutiqueMiel.commandeCreee'), 'OK', { duration: 3000 });
        this.router.navigate(['/commandes']);
      },
      error: err => {
        console.error(err);
        this.snackBar.open(this.translate.instant('boutiqueMiel.erreurCommande'), 'OK', { duration: 3000 });
        this.loading = false;
      }
    });
  }
}
