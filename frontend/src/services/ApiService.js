const apiUrl = process.env.VUE_APP_API_URL;

export default {
  async request(url, options = {}) {
    const requestOptions = {
      ...options,
      credentials: 'include',  // Important for authentication cookies
      headers: {
        'Content-Type': 'application/json',
        ...options.headers
      }
    };

    try {
      const response = await fetch(url, requestOptions);

      if (response.status === 401) {
        console.error('Unauthorized request');
        throw new Error('Unauthorized');
      }

      if (!response.ok) {
        const contentType = response.headers.get('content-type');
        let errorMessage = `API error: ${response.status}`;

        if (contentType && contentType.includes('application/json')) {
          const errorData = await response.json();
          errorMessage = errorData.message || errorData.error || errorMessage;
        }

        const error = new Error(errorMessage);
        error.status = response.status;
        throw error;
      }

      const contentType = response.headers.get('content-type');
      if (contentType && contentType.includes('application/json')) {
        return await response.json();
      }
      return await response.text();
    } catch (error) {
      console.error('API request error:', error);
      throw error;
    }
  },

  async get(url, options = {}) {
    return this.request(url, { ...options, method: 'GET' });
  },

  async post(url, data, options = {}) {
    return this.request(url, {
      ...options,
      method: 'POST',
      body: JSON.stringify(data)
    });
  },

  async put(url, data, options = {}) {
    return this.request(url, {
      ...options,
      method: 'PUT',
      body: JSON.stringify(data)
    });
  },

  async delete(url, options = {}) {
    return this.request(url, { ...options, method: 'DELETE' });
  }
}

