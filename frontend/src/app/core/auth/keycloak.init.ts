import { provideKeycloak, withAutoRefreshToken } from 'keycloak-angular';
import { environment } from '../../environments/environment';

export const provideKeycloakAngular = () =>
  provideKeycloak({
    config: {
      url: environment.keycloak.url,
      realm: environment.keycloak.realm,
      clientId: environment.keycloak.clientId
    },
    initOptions: {
      onLoad: 'login-required',
      pkceMethod: 'S256',
      checkLoginIframe: false
    }
  /*,
    features: [
      withAutoRefreshToken({
        onInactivityTimeout: 'logout',
        sessionTimeout: 60000
      })
    ]
  */
  });
