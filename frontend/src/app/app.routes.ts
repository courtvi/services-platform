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
      },
      {
        path: 'boutiques',
        children: [
          {
            path: '',
            loadComponent: () =>
              import('./features/admin/admin-boutiques/admin-boutiques-accueil/admin-boutiques-accueil')
                .then(m => m.AdminBoutiquesAccueil)
          },
          {
            path: ':id',
            loadComponent: () =>
              import('./features/admin/admin-boutiques/admin-boutique-commandes/admin-boutique-commandes')
                .then(m => m.AdminBoutiqueCommandes)
          }
        ]
      }
    ]
  },


  {
    path: 'boutiques',
    children: [
      {
        path: '',
        loadComponent: () =>
          import('./features/boutiques/boutiques-accueil/boutiques-accueil')
            .then(m => m.BoutiquesAccueil)
      },
      {
        path: 'miel',
        loadChildren: () =>
          import('./features/boutiques/miel/miel.routes')
            .then(m => m.MIEL_ROUTES)
      }
      // Prochaine boutique : ajouter un bloc similaire ici, ex.
      // { path: 'fromage', loadChildren: () => import('./features/boutiques/fromage/fromage.routes').then(m => m.FROMAGE_ROUTES) }
    ]
  },

  {
    path: '**',
    loadComponent: () =>
      import('./shared/components/not-found/not-found.component')
        .then(m => m.NotFoundComponent)
  }
];
