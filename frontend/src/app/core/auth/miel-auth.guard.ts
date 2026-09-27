import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import Keycloak from 'keycloak-js';

export const mielAuthGuard: CanActivateFn = async () => {
  const keycloak = inject(Keycloak) as Keycloak;

  if (keycloak.authenticated) {
    return true;
  }

  await keycloak.login({ redirectUri: window.location.origin + '/boutiques' });
  return false;
};

export const mielAdminGuard: CanActivateFn = async () => {
  const keycloak = inject(Keycloak) as Keycloak;
  const router = inject(Router);

  if (!keycloak.authenticated) {
    await keycloak.login({ redirectUri: window.location.origin + '/commandes' });
    return false;
  }

  if (!keycloak.hasResourceRole('ADMIN', 'chabeille')) {
    return router.createUrlTree(['/boutiques/miel']);
  }

  return true;
};
