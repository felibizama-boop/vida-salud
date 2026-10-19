export const environment = {
  production: true,
  msalConfig: {
    auth: {
      clientId: 'f3891669-68a7-4f6e-8aa1-e3720f99b126',
      authority: 'https://login.microsoftonline.com/6ba162ab-7029-42ae-9da1-7d41f345ef4c',
      redirectUri: 'http://localhost:4200',
      postLogoutRedirectUri: 'http://localhost:4200',
    },
  },
  apiConfig: {
    uri: 'http://localhost:8080/api/v1/',
    scopes: ['api://4a019171-2fcc-4b5f-8f36-01a031f841f3/access_as_user'],
  },
};
