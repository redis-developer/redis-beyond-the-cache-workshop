import ApiService from './ApiService';

const apiUrl = process.env.NODE_ENV === 'production'
  ? ''
  : (process.env.VUE_APP_API_URL || 'http://localhost:9001');

function portalUserFrom(response) {
  if (!response) {
    return null;
  }

  const user = response.user || response.portalUser || response;
  const email = user.normalizedEmail || user.email || user.actorId || user.userId;

  if (!email) {
    return null;
  }

  return {
    ...user,
    email
  };
}

export default {
  async login(email, allowMarketingContact = true) {
    const response = await ApiService.post(`${apiUrl}/api/portal/login`, {
      email,
      allowMarketingContact
    });
    return portalUserFrom(response);
  },

  async getCurrentUser() {
    const response = await ApiService.get(`${apiUrl}/api/portal/me`);
    return portalUserFrom(response);
  },

  async logout() {
    return ApiService.post(`${apiUrl}/api/portal/logout`, {});
  }
};
