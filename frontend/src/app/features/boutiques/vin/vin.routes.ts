import { Routes } from '@angular/router';
import { vinAuthGuard } from './../../../core/auth/vin-auth.guard';

export const vin_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./vin-shell/vin-shell').then(m => m.vinShell),
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
        canActivate: [vinAuthGuard],
        loadComponent: () =>
          import('./checkout/checkout').then(m => m.Checkout)
      }
    ]
  }
];
