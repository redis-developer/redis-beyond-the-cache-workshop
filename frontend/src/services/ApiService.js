function actorHeaders() {
  const actorId = process.env.VUE_APP_ACTOR_ID || 'local-learner';
  const actorType = process.env.VUE_APP_ACTOR_TYPE || 'LEARNER';
  const actorRoles = process.env.VUE_APP_ACTOR_ROLES || 'platform:learner';
  const headers = {};

  if (actorId) {
    headers['X-Platform-Actor-Id'] = actorId;
  }
  if (actorType) {
    headers['X-Platform-Actor-Type'] = actorType;
  }
  if (actorRoles) {
    headers['X-Platform-Roles'] = actorRoles;
  }

  return headers;
}

export default {
  async request(url, options = {}) {
    const requestOptions = {
      cache: 'no-store',
      ...options,
      credentials: 'include',
      headers: {
        'Content-Type': 'application/json',
        ...actorHeaders(),
        ...options.headers
      }
    };

    const response = await fetch(url, requestOptions);

    if (!response.ok) {
      const contentType = response.headers.get('content-type');
      let errorMessage = `API error: ${response.status}`;

      if (contentType && contentType.includes('application/json')) {
        const errorData = await response.json();
        errorMessage = errorData.detail || errorData.message || errorData.error || errorMessage;
      }

      const error = new Error(errorMessage);
      error.status = response.status;
      throw error;
    }

    if (response.status === 204) {
      return null;
    }

    const contentType = response.headers.get('content-type');
    if (contentType && contentType.includes('application/json')) {
      return await response.json();
    }
    return await response.text();
  },

  async get(url, options = {}) {
    return this.request(url, { ...options, method: 'GET' });
  },

  async post(url, data = {}, options = {}) {
    return this.request(url, {
      ...options,
      method: 'POST',
      body: JSON.stringify(data)
    });
  },

  async delete(url, options = {}) {
    return this.request(url, { ...options, method: 'DELETE' });
  }
};
