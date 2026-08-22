import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import Keycloak from 'keycloak-js';
import { from, of, switchMap, catchError } from 'rxjs';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const keycloak = inject(Keycloak) as Keycloak;

  if (!keycloak.authenticated) {
    return next(req);
  }

  return from(keycloak.updateToken(30)).pipe(
    catchError(() => of(false)),
    switchMap(() => {
      const token = keycloak.token;
      if (token) {
        const authReq = req.clone({
          setHeaders: {
            Authorization: `Bearer ${token}`
          }
        });
        return next(authReq);
      }
      return next(req);
    })
  );
};
