import { Routes } from '@angular/router';
import { authGuard, adminGuard } from './core/auth/auth.guard';

export const routes: Routes = [

  {
    path: '',
    redirectTo: 'commandes',
    pathMatch: 'full'
  },


  {
    path: 'commandes',
    canActivate: [authGuard],
    children: [
      {
        path: '',

        loadComponent: () =>
          import('./features/commandes/commande-list/commande-list')
            .then(m => m.CommandeList)
      },
      {
        path: 'nouvelle',
        loadComponent: () =>
          import('./features/commandes/commande-form/commande-form')
            .then(m => m.CommandeForm)
      },
      {
        path: ':id',
        loadComponent: () =>
          import('./features/commandes/commande-detail/commande-detail')
            .then(m => m.CommandeDetail)
      }
    ]
  },


  {
    path: 'admin',
    canActivate: [adminGuard],
    children: [
      {
        path: 'commandes',
        loadComponent: () =>
          import('./features/admin/admin-commande-list/admin-commande-list')
            .then(m => m.AdminCommandeList)
      }
    ]
  },

  // ✅ Page 404
  {
    path: '**',
    loadComponent: () =>
      import('./shared/components/not-found/not-found.component')
        .then(m => m.NotFoundComponent)
  }
];
