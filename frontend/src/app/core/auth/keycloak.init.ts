import { provideKeycloak, withAutoRefreshToken } from 'keycloak-angular';
import { environment } from '../../environments/environment';

function resolveSite() {
  const isMiel = window.location.pathname.startsWith('/boutiques/miel');
  return isMiel ? environment.sites['chabeille'] : environment.sites['lorrconnect'];
}

export const provideKeycloakAngular = () => {
  const cfg = resolveSite().keycloak;
  return provideKeycloak({
    config: {
      url: cfg.url,
      realm: cfg.realm,
      clientId: cfg.clientId
    },
    initOptions: {
      onLoad: 'check-sso',
      pkceMethod: 'S256',
      checkLoginIframe: false
    }
  });
};
