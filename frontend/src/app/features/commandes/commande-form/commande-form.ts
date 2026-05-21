import { Component, CUSTOM_ELEMENTS_SCHEMA, inject } from '@angular/core';
import { FormArray, FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { CommandeService } from '../../../core/services/commande.service';
import { CommandeRequest } from '../../../core/models/commande.model';
import { TranslateModule } from '@ngx-translate/core';

@Component({
  selector: 'app-commande-form',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatSnackBarModule,
    TranslateModule
  ],
  templateUrl: './commande-form.html',
  schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class CommandeForm {

  private commandeService = inject(CommandeService);
  private router = inject(Router);
  private snackBar = inject(MatSnackBar);
  private fb = inject(FormBuilder);

  form: FormGroup = this.fb.group({
    articles: this.fb.array([this.createArticle()])
  });

  loading = false;

  generateReference(): string {
    const now = new Date();
    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, '0');
    const day = String(now.getDate()).padStart(2, '0');
    const random = Math.floor(1000 + Math.random() * 9000);
    return `CMD-${year}${month}${day}-${random}`;
  }

  get articles(): FormArray {
    return this.form.get('articles') as FormArray;
  }

  createArticle(): FormGroup {
    return this.fb.group({
      article: ['Baguette'],
      quantite: [1, [Validators.required, Validators.min(1)]],
      prixUnitaire: [1.30],
      tva: [5.5],
      total: [1.30]
    });
  }

  addArticle() {
    this.articles.push(this.createArticle());
  }

  removeArticle(index: number) {
    if (this.articles.length > 1) {
      this.articles.removeAt(index);
    }
  }

  submit() {
    if (this.form.invalid) return;
    this.loading = true;

    const payload: CommandeRequest = {
      reference: this.generateReference(),
      lignes: this.form.value.articles.map((p: any) => ({
        article: p.article,
        quantite: p.quantite,
        prixUnitaire: p.prixUnitaire,
        total: p.quantite * p.prixUnitaire
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

  increaseQuantity(index: number) {
    const control = this.articles.at(index);
    const quantite = (control.get('quantite')?.value || 0) + 1;
    control.get('quantite')?.setValue(quantite);
    control.get('total')?.setValue(quantite * control.get('prixUnitaire')?.value);
  }

  decreaseQuantity(index: number) {
    const control = this.articles.at(index);
    const current = control.get('quantite')?.value || 0;
    if (current > 1) {
      const quantite = current - 1;
      control.get('quantite')?.setValue(quantite);
      control.get('total')?.setValue(quantite * control.get('prixUnitaire')?.value);
    }
  }
}