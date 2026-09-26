const KC = 'http://localhost:30090';
const PAYPAL = 'AbVtQq3_8LphF3SZ8TRV4wX7s-lTLfySfkHDhmvxknXULaSAVBgfzUptTH2AAPRo4BxIkHuCZUjCUwZh';

export const environment = {
  production: false,
  apiUrl: 'http://localhost:8082',
  sites: {
    vin:  { keycloak: { url: KC, realm: 'lorrconnect', clientId: 'cuvedesaulnes' }, paypal: { clientId: PAYPAL } },
    miel: { keycloak: { url: KC, realm: 'lorrconnect', clientId: 'chabeille' },     paypal: { clientId: PAYPAL } }
  }
};
