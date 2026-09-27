import { provideKeycloak, withAutoRefreshToken, AutoRefreshTokenService, UserActivityService } from 'keycloak-angular';
import { environment } from '../../environments/environment';

function resolveSite() {
  return window.location.pathname.startsWith('/boutiques/miel')
    ? environment.sites.miel
    : environment.sites.vin;
}

export const provideKeycloakAngular = () => {
  const cfg = resolveSite().keycloak;
  return provideKeycloak({
    config: { url: cfg.url, realm: cfg.realm, clientId: cfg.clientId },
    initOptions: {
      onLoad: 'check-sso',
      pkceMethod: 'S256',
      checkLoginIframe: false,
      silentCheckSsoRedirectUri: window.location.origin + '/assets/silent-check-sso.html'
    },
    features: [withAutoRefreshToken({ onInactivityTimeout: 'logout', sessionTimeout: 30 * 60 * 1000 })],
    providers: [AutoRefreshTokenService, UserActivityService]
  });
};
