export const environment = {
  production: true,
  apiUrl: 'http://localhost:31803',
  sites: {
      'camping-haller': {
        keycloak: { url: 'http://localhost:30090', realm: 'camping-haller', clientId: 'haller' },
        paypal: { clientId: 'AbVtQq3_8LphF3SZ8TRV4wX7s-lTLfySfkHDhmvxknXULaSAVBgfzUptTH2AAPRo4BxIkHuCZUjCUwZh' }
      },
      'chabeille': {
        keycloak: { url: 'http://localhost:30090', realm: 'chabeille', clientId: 'chabeille' },
        paypal: { clientId: 'AbVtQq3_8LphF3SZ8TRV4wX7s-lTLfySfkHDhmvxknXULaSAVBgfzUptTH2AAPRo4BxIkHuCZUjCUwZh' }
      }
    }
  };
