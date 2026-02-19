import ApiService from './ApiService';

// Use relative URLs in production (when served from Spring Boot)
// Use absolute URLs in development (when using Vue dev server)
const apiUrl = process.env.NODE_ENV === 'production' ? '' : (process.env.VUE_APP_API_URL || 'http://localhost:9000');

export default {
  /**
   * Get all service statuses (infrastructure + workshops)
   * Returns: Array of ServiceStatus objects
   */
  async getAllStatus() {
    return ApiService.get(`${apiUrl}/manager/api/status`);
  },

  /**
   * Start infrastructure (Redis + Redis Insight)
   * Returns: CommandResponse
   */
  async startInfrastructure() {
    return ApiService.post(`${apiUrl}/manager/api/infrastructure/start`);
  },

  /**
   * Stop infrastructure (Redis + Redis Insight)
   * Returns: CommandResponse
   */
  async stopInfrastructure() {
    return ApiService.post(`${apiUrl}/manager/api/infrastructure/stop`);
  },

  /**
   * Start a specific workshop
   * @param {string} workshopId - The workshop ID
   * Returns: CommandResponse
   */
  async startWorkshop(workshopId) {
    return ApiService.post(`${apiUrl}/manager/api/workshop/${workshopId}/start`);
  },

  /**
   * Stop a specific workshop
   * @param {string} workshopId - The workshop ID
   * Returns: CommandResponse
   */
  async stopWorkshop(workshopId) {
    return ApiService.post(`${apiUrl}/manager/api/workshop/${workshopId}/stop`);
  },

  /**
   * Restart a specific workshop
   * @param {string} workshopId - The workshop ID
   * Returns: CommandResponse
   */
  async restartWorkshop(workshopId) {
    return ApiService.post(`${apiUrl}/manager/api/workshop/${workshopId}/restart`);
  },

  /**
   * Restart a specific workshop without rebuilding the Docker image.
   * This stops and force-recreates the container using the existing image.
   * @param {string} workshopId - The workshop ID
   * Returns: CommandResponse
   */
  async restartWorkshopNoBuild(workshopId) {
    return ApiService.post(`${apiUrl}/manager/api/workshop/${workshopId}/restart-no-build`);
  }
}

