import ApiService from './ApiService';

const apiUrl = process.env.NODE_ENV === 'production'
  ? ''
  : (process.env.VUE_APP_API_URL || 'http://localhost:9001');

export default {
  async getWorkshops() {
    return ApiService.get(`${apiUrl}/api/catalog/workshops`);
  },

  async getSessions() {
    return ApiService.get(`${apiUrl}/api/sessions`);
  },

  async getSession(sessionId) {
    return ApiService.get(`${apiUrl}/api/sessions/${sessionId}`);
  },

  async createSession(workshopId, mode, releaseVersion, sessionEnvironment) {
    const hasSessionEnvironment = sessionEnvironment
      && Object.keys(sessionEnvironment).length > 0;
    const payload = {
      workshopId,
      ...(mode ? { mode } : {}),
      ...(releaseVersion ? { releaseVersion } : {}),
      ...(hasSessionEnvironment ? { sessionEnvironment } : {})
    };
    return ApiService.post(`${apiUrl}/api/sessions`, payload);
  },

  async restartSession(sessionId, rebuild) {
    return ApiService.post(`${apiUrl}/api/sessions/${sessionId}/restart`, { rebuild });
  },

  async terminateSession(sessionId) {
    return ApiService.delete(`${apiUrl}/api/sessions/${sessionId}`);
  }
};
