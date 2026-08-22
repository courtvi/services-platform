export const environment = {
  production: true,
  apiUrl: 'https://91.134.36.197.nip.io',
  sites: {
    'camping-haller': {
      keycloak: {
        url: 'https://91.134.36.197.nip.io',
        realm: 'camping-haller',
        clientId: 'haller'
      },
      paypal: {
        clientId: 'AbVtQq3_8LphF3SZ8TRV4wX7s-lTLfySfkHDhmvxknXULaSAVBgfzUptTH2AAPRo4BxIkHuCZUjCUwZh'
      }
    },
    'chabeille': {
      keycloak: {
        url: 'https://91.134.36.197.nip.io',
        realm: 'chabeille',
        clientId: 'chabeille'
      },
      paypal: {
        // Sandbox: on réutilise le client id de test de camping-haller en attendant un client id dédié à chabeille
        clientId: 'AbVtQq3_8LphF3SZ8TRV4wX7s-lTLfySfkHDhmvxknXULaSAVBgfzUptTH2AAPRo4BxIkHuCZUjCUwZh'
      }
    }
  }
};
