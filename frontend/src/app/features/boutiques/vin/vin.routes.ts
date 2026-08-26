import { Routes } from '@angular/router';
import { mielAuthGuard } from './../../../core/auth/miel-auth.guard';

export const MIEL_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./miel-shell/miel-shell').then(m => m.MielShell),
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./catalogue/catalogue').then(m => m.Catalogue)
      },
      {
        path: 'produit/:id',
        loadComponent: () =>
          import('./produit-detail/produit-detail').then(m => m.ProduitDetail)
      },
      {
        path: 'panier',
        loadComponent: () =>
          import('./panier/panier').then(m => m.Panier)
      },
      {
        path: 'checkout',
        canActivate: [mielAuthGuard],
        loadComponent: () =>
          import('./checkout/checkout').then(m => m.Checkout)
      }
    ]
  }
];
