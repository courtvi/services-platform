import { Component, CUSTOM_ELEMENTS_SCHEMA, inject, ViewEncapsulation } from '@angular/core';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { CommandeService } from '../../../core/services/commande.service';
import { CommandeRequest } from '../../../core/models/commande.model';
import { TranslateModule } from '@ngx-translate/core';
import { AbstractControl, FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { CurrencyPipe } from '@angular/common';
import { NgxPayPalModule, IPayPalConfig, ICreateOrderRequest } from 'ngx-paypal';
import { environment } from '../../../environments/environment';


interface ArticleGroupe {
  key: string;
  prix: number;          // prix TTC affiché (le prix facturé est lu par le serveur dans produit-service)
  id: number;            // id du produit dans produit-service
  idCoupe?: number;      // id de la variante « coupé » (+0,10 €), pour les pains qui la proposent
}

interface Groupe {
  key: string;
  label: string;
  coupe: boolean;
  articles: ArticleGroupe[];
}

@Component({
  selector: 'app-commande-form',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatIconModule,
    MatSlideToggleModule,
    CurrencyPipe,
    MatFormFieldModule,
    MatInputModule,
    MatSnackBarModule,
    TranslateModule,
    NgxPayPalModule
  ],
  templateUrl: './commande-form.html',
  styleUrl: './commande-form.css',
  encapsulation: ViewEncapsulation.None,
  schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class CommandeForm {

  private commandeService = inject(CommandeService);
  private router = inject(Router);
  private snackBar = inject(MatSnackBar);
  private fb = inject(FormBuilder);

  payPalConfig?: IPayPalConfig;

  initPayPal() {
    const total = this.form.value.articles
      .reduce((acc: number, a: any) => acc + (a.quantite * a.prixUnitaire), 0)
      .toFixed(2);

    this.payPalConfig = {
      currency: 'EUR',
      clientId: environment.sites.vin.paypal.clientId,
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
        actions.order.capture().then(() => {
          this.submit();
        });
      },
      onError: err => {
        console.error('PayPal error', err);
        this.snackBar.open('Erreur PayPal', 'Fermer', { duration: 3000 });
      }
    };
  }

  getTomorrow(): string {
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    return tomorrow.toISOString().split('T')[0];
  }

  readonly GROUPES: Groupe[] = [
    {
      key: 'petits_pains',
      label: 'groupe.petits_pains',
      coupe: false,
      articles: [
        { key: 'petit_pain_blanc', prix: 0.65, id: 201 },
        { key: 'petit_pain_cereales', prix: 0.65, id: 202 }
      ]
    },
    {
      key: 'baguettes',
      label: 'groupe.baguettes',
      coupe: false,
      articles: [
        { key: 'baguette_blanche', prix: 1.90, id: 203 },
        { key: 'baguette_cereale', prix: 3.00, id: 204 }
      ]
    },
    {
      key: 'pains',
      label: 'groupe.pains',
      coupe: true,
      articles: [
        { key: 'pain_6_cereales', prix: 3.75, id: 205, idCoupe: 215 },
        { key: 'pain_chef', prix: 3.50, id: 206, idCoupe: 216 },
        { key: 'pain_campagne', prix: 3.00, id: 207, idCoupe: 217 },
        { key: 'pain_blanc', prix: 2.90, id: 208, idCoupe: 218 }
      ]
    },
    {
      key: 'viennoiseries',
      label: 'groupe.viennoiseries',
      coupe: false,
      articles: [
        { key: 'croissant', prix: 1.50, id: 209 },
        { key: 'viennoiserie', prix: 1.70, id: 210 }
      ]
    }
  ];

  form: FormGroup = this.fb.group({
    dateLivraison: [this.getTomorrow(), Validators.required],
    articles: this.fb.array(
      this.GROUPES.flatMap(groupe =>
        groupe.articles.map(a =>
          this.fb.group({
            article: [a.key],
            groupe: [groupe.key],
            quantite: [0, [Validators.required, Validators.min(0)]],
            prixUnitaire: [a.prix],
            produitId: [a.id],
            produitIdCoupe: [a.idCoupe ?? null],
            coupe: [false],
            total: [0]
          })
        )
      )
    )
  });

  loading = false;

  get articles(): FormArray<FormGroup> {
    return this.form.get('articles') as FormArray<FormGroup>;
  }

  getArticlesByGroupe(groupeKey: string): { control: AbstractControl; index: number }[] {
    return this.articles.controls
      .map((control, index) => ({ control, index }))
      .filter(({ control }) => control.get('groupe')?.value === groupeKey);
  }

  onCoupeToggle(index: number) {
    const control = this.articles.at(index);
    const coupe = control.get('coupe')?.value;
    const prixBase = control.get('prixUnitaire')?.value;
    const quantite = control.get('quantite')?.value;
    const prixFinal = coupe ? prixBase + 0.10 : prixBase;
    control.get('total')?.setValue(quantite * prixFinal);
  }

  generateReference(): string {
    const now = new Date();
    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, '0');
    const day = String(now.getDate()).padStart(2, '0');
    const random = Math.floor(1000 + Math.random() * 9000);
    return `CMD-${year}${month}${day}-${random}`;
  }

  increaseQuantity(index: number) {
    this.updateQuantite(index, 1);
  }

  decreaseQuantity(index: number) {
    this.updateQuantite(index, -1);
  }

  private updateQuantite(index: number, delta: number) {
    const control = this.articles.at(index);
    const current = control.get('quantite')?.value || 0;
    const quantite = current + delta;
    if (quantite < 0) return;

    const prixBase = control.get('prixUnitaire')?.value || 0;
    const coupe = control.get('coupe')?.value || false;
    const prixFinal = coupe ? prixBase + 0.10 : prixBase;

    control.get('quantite')?.setValue(quantite);
    control.get('total')?.setValue(quantite * prixFinal);
  }

  submit() {
    if (this.form.invalid) return;
    this.loading = true;

    const payload: CommandeRequest = {
      reference: this.generateReference(),
      dateCommande: new Date().toISOString().slice(0, 19),
      dateLivraison: this.form.value.dateLivraison + 'T00:00:00',
      lignes: this.form.value.articles
        .filter((p: any) => p.quantite > 0)
        .map((p: any) => ({
          // Le serveur lit le nom et le prix dans produit-service : on envoie seulement
          // le produit (variante « coupé » si cochée) et la quantité.
          produitId: p.coupe && p.produitIdCoupe ? p.produitIdCoupe : p.produitId,
          quantite: p.quantite
        }))
    };

    this.commandeService.createCommande(payload).subscribe({
      next: () => {
        this.snackBar.open('Commande créée !', 'Fermer', { duration: 3000 });
        this.router.navigate(['/commandes']);
      },
      error: err => {
        console.error(err);
        this.snackBar.open('Erreur lors de la création', 'Fermer', { duration: 3000 });
        this.loading = false;
      }
    });
  }
}
