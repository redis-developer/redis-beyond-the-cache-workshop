import ApiService from '../services/ApiService';

const apiUrl = process.env.NODE_ENV === 'production'
  ? ''
  : (process.env.VUE_APP_API_URL || 'http://localhost:9001');

export async function getSessionShell(sessionId) {
  return ApiService.get(`${apiUrl}/api/sessions/${encodeURIComponent(sessionId)}/shell`);
}

export default {
  getSessionShell
};
