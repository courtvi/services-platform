export const environment = {
  production: false,
  apiUrl: 'http://localhost:31803',
  sites: {
    'lorrconnect': {
      keycloak: { url: 'http://localhost:30090', realm: 'lorrconnect', clientId: 'haller' },
      paypal: { clientId: 'AbVtQq3_8LphF3SZ8TRV4wX7s-lTLfySfkHDhmvxknXULaSAVBgfzUptTH2AAPRo4BxIkHuCZUjCUwZh' }
    },
    'chabeille': {
      keycloak: { url: 'http://localhost:30090', realm: 'chabeille', clientId: 'chabeille' },
      paypal: { clientId: 'AbVtQq3_8LphF3SZ8TRV4wX7s-lTLfySfkHDhmvxknXULaSAVBgfzUptTH2AAPRo4BxIkHuCZUjCUwZh' }
    }
  }
};
