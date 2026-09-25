export const environment = {
  production: true,
  apiUrl: '__OVH_URL__',
  sites: {
    'lorrconnect': {
      keycloak: {
        url: '__OVH_URL__',
        realm: 'lorrconnect',
        clientId: 'cuvedesaulnes'
      },
      paypal: {
        clientId: 'AbVtQq3_8LphF3SZ8TRV4wX7s-lTLfySfkHDhmvxknXULaSAVBgfzUptTH2AAPRo4BxIkHuCZUjCUwZh'
      }
    },
    'lorrconnect': {
      keycloak: {
        url: '__OVH_URL__',
        realm: 'chabeille',
        clientId: 'chabeille'
      },
      paypal: {
        clientId: 'AbVtQq3_8LphF3SZ8TRV4wX7s-lTLfySfkHDhmvxknXULaSAVBgfzUptTH2AAPRo4BxIkHuCZUjCUwZh'
      }
    }
  }
};
