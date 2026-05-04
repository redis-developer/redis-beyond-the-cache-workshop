export default {
  async request(url, options = {}) {
    const requestOptions = {
      cache: 'no-store',
      ...options,
      credentials: 'include',
      headers: {
        'Content-Type': 'application/json',
        ...options.headers
      }
    };

    const response = await fetch(url, requestOptions);

    if (!response.ok) {
      const contentType = response.headers.get('content-type');
      let errorMessage = `API error: ${response.status}`;
      let errorData = null;

      if (contentType && (contentType.includes('application/json') || contentType.includes('+json'))) {
        errorData = await response.json();
        errorMessage = errorData.detail || errorData.message || errorData.error || errorMessage;
      }

      const error = new Error(errorMessage);
      error.status = response.status;
      error.data = errorData;
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
