import { createStore } from 'vuex'
import WorkshopService from '@/services/WorkshopService'

export default createStore({
  state: {
    workshops: [
      {
        id: '1_session_management',
        title: 'Distributed Session Management',
        description: 'Learn how to implement distributed session management using Redis and Spring Session. ' +
          'Experience the problem of in-memory sessions, then solve it with Redis-backed sessions ' +
          'that persist across application restarts and enable horizontal scaling.',
        difficulty: 'Beginner',
        estimatedMinutes: 30,
        url: '/workshop/session-management/',
        topics: ['Sessions', 'Spring Session', 'Distributed Systems', 'Scalability']
      }
    ],
    services: [],
    restartingWorkshops: new Set(),
    // Track restart progress: { workshopId: { stage, startTime, isRebuild } }
    restartProgress: {},
    loading: false,
    error: null
  },

  getters: {
    allWorkshops: (state) => state.workshops,
    
    getWorkshopById: (state) => (id) => {
      return state.workshops.find(w => w.id === id);
    },

    getServiceByName: (state) => (name) => {
      return state.services.find(s => s.name === name);
    },

    isWorkshopRestarting: (state) => (workshopId) => {
      return state.restartingWorkshops.has(workshopId);
    },

    getRestartProgress: (state) => (workshopId) => {
      return state.restartProgress[workshopId] || null;
    },

    infrastructureStatus: (state) => {
      const redis = state.services.find(s => s.name === 'redis');
      const redisInsight = state.services.find(s => s.name === 'redis-insight');
      return {
        redis: redis?.status || 'stopped',
        redisInsight: redisInsight?.status || 'stopped',
        allRunning: redis?.status === 'running' && redisInsight?.status === 'running'
      };
    },

    workshopStatus: (state) => (workshopId) => {
      const service = state.services.find(s => s.name === workshopId);
      return service?.status || 'stopped';
    }
  },

  mutations: {
    setServices(state, services) {
      state.services = services;
    },

    setLoading(state, loading) {
      state.loading = loading;
    },

    setError(state, error) {
      state.error = error;
    },

    addRestartingWorkshop(state, workshopId) {
      state.restartingWorkshops.add(workshopId);
    },

    removeRestartingWorkshop(state, workshopId) {
      state.restartingWorkshops.delete(workshopId);
    },

    setRestartProgress(state, { workshopId, stage, isRebuild }) {
      state.restartProgress[workshopId] = {
        stage,
        isRebuild,
        startTime: state.restartProgress[workshopId]?.startTime || Date.now()
      };
    },

    clearRestartProgress(state, workshopId) {
      delete state.restartProgress[workshopId];
    }
  },

  actions: {
    async fetchStatus({ commit }) {
      try {
        const services = await WorkshopService.getAllStatus();
        commit('setServices', services);
      } catch (error) {
        console.error('Error fetching status:', error);
        commit('setError', error.message);
      }
    },

    async startInfrastructure({ dispatch }) {
      try {
        await WorkshopService.startInfrastructure();
        await dispatch('fetchStatus');
      } catch (error) {
        console.error('Error starting infrastructure:', error);
        throw error;
      }
    },

    async stopInfrastructure({ dispatch }) {
      try {
        await WorkshopService.stopInfrastructure();
        await dispatch('fetchStatus');
      } catch (error) {
        console.error('Error stopping infrastructure:', error);
        throw error;
      }
    },

    async startWorkshop({ commit, dispatch }, workshopId) {
      try {
        commit('addRestartingWorkshop', workshopId);
        commit('setRestartProgress', { workshopId, stage: 'building', isRebuild: true, isDeploy: true });

        // Fire and forget - don't wait for backend
        WorkshopService.startWorkshop(workshopId).catch(error => {
          console.error('Start workshop request failed:', error);
        });

        // Wait for workshop to start
        await dispatch('waitForWorkshopStart', { workshopId });

        commit('clearRestartProgress', workshopId);
        commit('removeRestartingWorkshop', workshopId);
      } catch (error) {
        console.error('Error starting workshop:', error);
        commit('clearRestartProgress', workshopId);
        commit('removeRestartingWorkshop', workshopId);
        throw error;
      }
    },

    async stopWorkshop({ dispatch }, workshopId) {
      try {
        await WorkshopService.stopWorkshop(workshopId);
        await dispatch('fetchStatus');
      } catch (error) {
        console.error('Error stopping workshop:', error);
        throw error;
      }
    },

    async restartWorkshop({ commit, dispatch }, workshopId) {
      try {
        commit('addRestartingWorkshop', workshopId);
        commit('setRestartProgress', { workshopId, stage: 'stopping', isRebuild: true });

        // Fire and forget - don't wait for backend
        WorkshopService.restartWorkshop(workshopId).catch(error => {
          console.error('Restart request failed:', error);
        });

        // Wait for workshop to go down then come back up
        await dispatch('waitForWorkshopReady', { workshopId, isRebuild: true });

        commit('clearRestartProgress', workshopId);
        commit('removeRestartingWorkshop', workshopId);
      } catch (error) {
        console.error('Error restarting workshop:', error);
        commit('clearRestartProgress', workshopId);
        commit('removeRestartingWorkshop', workshopId);
        throw error;
      }
    },

    async restartWorkshopNoBuild({ commit, dispatch }, workshopId) {
      try {
        commit('addRestartingWorkshop', workshopId);
        commit('setRestartProgress', { workshopId, stage: 'stopping', isRebuild: false });

        // Fire and forget - don't wait for backend
        WorkshopService.restartWorkshopNoBuild(workshopId).catch(error => {
          console.error('Restart (no-build) request failed:', error);
        });

        // Wait for workshop to go down then come back up
        await dispatch('waitForWorkshopReady', { workshopId, isRebuild: false });

        commit('clearRestartProgress', workshopId);
        commit('removeRestartingWorkshop', workshopId);
      } catch (error) {
        console.error('Error restarting workshop without rebuild:', error);
        commit('clearRestartProgress', workshopId);
        commit('removeRestartingWorkshop', workshopId);
        throw error;
      }
    },

    async waitForWorkshopReady({ commit, dispatch, getters }, { workshopId, isRebuild }) {
      const startTime = Date.now();

      // Stage timing estimates (in ms)
      const stages = isRebuild
        ? [
            { name: 'stopping', until: 3000 },
            { name: 'building', until: 8000 },
            { name: 'starting', until: 12000 },
            { name: 'initializing', until: 999999 }
          ]
        : [
            { name: 'stopping', until: 3000 },
            { name: 'starting', until: 6000 },
            { name: 'initializing', until: 999999 }
          ];

      // Wait for workshop to go down (max 20 seconds)
      for (let i = 0; i < 10; i++) {
        await new Promise(resolve => setTimeout(resolve, 2000));

        // Update stage based on elapsed time
        const elapsed = Date.now() - startTime;
        const currentStage = stages.find(s => elapsed < s.until)?.name || 'initializing';
        commit('setRestartProgress', { workshopId, stage: currentStage, isRebuild });

        await dispatch('fetchStatus');
        const status = getters.workshopStatus(workshopId);
        if (status !== 'running') break;
      }

      // Wait for workshop to come back up (max 120 seconds)
      for (let i = 0; i < 60; i++) {
        await new Promise(resolve => setTimeout(resolve, 2000));

        // Update stage based on elapsed time
        const elapsed = Date.now() - startTime;
        const currentStage = stages.find(s => elapsed < s.until)?.name || 'initializing';
        commit('setRestartProgress', { workshopId, stage: currentStage, isRebuild });

        await dispatch('fetchStatus');
        const status = getters.workshopStatus(workshopId);
        if (status === 'running') {
          commit('setRestartProgress', { workshopId, stage: 'ready', isRebuild });
          break;
        }
      }
    },

    async waitForWorkshopStart({ commit, dispatch, getters }, { workshopId }) {
      const startTime = Date.now();

      // Stage timing estimates for deploy (no stopping phase)
      const stages = [
        { name: 'building', until: 5000 },
        { name: 'starting', until: 10000 },
        { name: 'initializing', until: 999999 }
      ];

      // Wait for workshop to come up (max 120 seconds)
      for (let i = 0; i < 60; i++) {
        await new Promise(resolve => setTimeout(resolve, 2000));

        // Update stage based on elapsed time
        const elapsed = Date.now() - startTime;
        const currentStage = stages.find(s => elapsed < s.until)?.name || 'initializing';
        commit('setRestartProgress', { workshopId, stage: currentStage, isRebuild: true, isDeploy: true });

        await dispatch('fetchStatus');
        const status = getters.workshopStatus(workshopId);
        if (status === 'running') {
          commit('setRestartProgress', { workshopId, stage: 'ready', isRebuild: true, isDeploy: true });
          break;
        }
      }
    }
  }
})

