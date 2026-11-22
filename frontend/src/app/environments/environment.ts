// Adresse publique fournie au démarrage du conteneur nginx (PUBLIC_URL → assets/env.js).
// Absente en local (ng serve, k3s sans PUBLIC_URL) : on garde les ports localhost.
const RUNTIME = (globalThis as { __env?: { publicUrl?: string; keycloakUrl?: string } }).__env ?? {};
const PUBLIC_URL: string | undefined = RUNTIME.publicUrl || undefined;

// L'API (/api) est servie sur le même domaine que le front ; Keycloak peut avoir son propre
// sous-domaine (ex. https://keycloak.lorrconnect.com), sinon il est sur le domaine du site.
const KC = RUNTIME.keycloakUrl || PUBLIC_URL || 'http://localhost:30090';
const PAYPAL = 'AbVtQq3_8LphF3SZ8TRV4wX7s-lTLfySfkHDhmvxknXULaSAVBgfzUptTH2AAPRo4BxIkHuCZUjCUwZh';

export const environment = {
  production: false,
  apiUrl: PUBLIC_URL ?? 'http://localhost:31803',   // API Gateway (les services sont en mTLS derrière elle)
  sites: {
    vin:  { keycloak: { url: KC, realm: 'lorrconnect', clientId: 'cuvedesaulnes' }, paypal: { clientId: PAYPAL } },
    miel: { keycloak: { url: KC, realm: 'lorrconnect', clientId: 'chabeille' },     paypal: { clientId: PAYPAL } }
  }
};
