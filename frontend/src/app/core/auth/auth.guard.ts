import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { KeycloakService } from 'keycloak-angular';

export const authGuard: CanActivateFn = async () => {
  const keycloak = inject(KeycloakService);
  const router = inject(Router);

  const isLoggedIn = await keycloak.isLoggedIn();

  if (isLoggedIn) {
    return true;
  }

  await keycloak.login({
    redirectUri: window.location.origin + '/commandes'
  });

  return false;
};

export const adminGuard: CanActivateFn = async () => {
  const keycloak = inject(KeycloakService);
  const router = inject(Router);

  const isLoggedIn = await keycloak.isLoggedIn();

  if (!isLoggedIn) {
    await keycloak.login();
    return false;
  }

  const hasAdminRole = keycloak.isUserInRole('ADMIN', 'haller');

  if (!hasAdminRole) {
    return router.createUrlTree(['/commandes']);
  }

  return true;
};
