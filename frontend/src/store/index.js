import { createStore } from 'vuex'
import WorkshopService from '@/services/WorkshopService'

const ACTIVE_SESSION_STATES = new Set([
  'REQUESTED',
  'ADMITTED',
  'PROVISIONING',
  'INITIALIZING',
  'READY',
  'DEGRADED',
  'TERMINATING',
  'CLEANUP_PENDING'
]);

const TERMINAL_SESSION_STATES = new Set(['TERMINATED', 'FAILED', 'EXPIRED']);

function isActiveSession(session) {
  return session && ACTIVE_SESSION_STATES.has(session.state);
}

function sessionSortValue(session) {
  return Date.parse(session.createdAt || '') || 0;
}

function activeSessionFor(workshopId, sessions) {
  return sessions
    .filter(session => session.workshopId === workshopId && isActiveSession(session))
    .sort((left, right) => sessionSortValue(right) - sessionSortValue(left))[0] || null;
}

function displayStatus(session) {
  if (!session) {
    return 'stopped';
  }
  return String(session.state || 'stopped').toLowerCase();
}

function upsertSession(sessions, updatedSession) {
  const index = sessions.findIndex(session => session.sessionId === updatedSession.sessionId);
  if (index === -1) {
    return [updatedSession, ...sessions];
  }

  return sessions.map(session => (
    session.sessionId === updatedSession.sessionId ? updatedSession : session
  ));
}

export default createStore({
  state: {
    workshops: [],
    sessions: [],
    pendingActions: {},
    loading: false,
    error: null
  },

  getters: {
    allWorkshops: (state) => {
      return state.workshops.map(workshop => {
        const activeSession = activeSessionFor(workshop.workshopId, state.sessions);
        return {
          ...workshop,
          id: workshop.workshopId,
          activeSession,
          status: displayStatus(activeSession),
          url: activeSession?.publicEntryUrl || workshop.path || '#',
          pendingAction: state.pendingActions[workshop.workshopId] || null
        };
      });
    },

    activeSessionByWorkshopId: (state) => (workshopId) => {
      return activeSessionFor(workshopId, state.sessions);
    },

    isWorkshopBusy: (state) => (workshopId) => {
      return Boolean(state.pendingActions[workshopId]);
    }
  },

  mutations: {
    setWorkshops(state, workshops) {
      state.workshops = workshops;
    },

    setSessions(state, sessions) {
      state.sessions = sessions;
    },

    upsertSession(state, session) {
      state.sessions = upsertSession(state.sessions, session);
    },

    setPendingAction(state, { workshopId, action }) {
      state.pendingActions = {
        ...state.pendingActions,
        [workshopId]: action
      };
    },

    clearPendingAction(state, workshopId) {
      const next = { ...state.pendingActions };
      delete next[workshopId];
      state.pendingActions = next;
    },

    setLoading(state, loading) {
      state.loading = loading;
    },

    setError(state, error) {
      state.error = error;
    }
  },

  actions: {
    async refreshAll({ dispatch }) {
      await Promise.all([
        dispatch('fetchWorkshops'),
        dispatch('fetchSessions')
      ]);
    },

    async fetchWorkshops({ commit }) {
      try {
        const workshops = await WorkshopService.getWorkshops();
        commit('setWorkshops', workshops);
        commit('setError', null);
      } catch (error) {
        console.error('Error fetching workshops:', error);
        commit('setError', error.message);
      }
    },

    async fetchSessions({ commit }) {
      try {
        const sessions = await WorkshopService.getSessions();
        commit('setSessions', sessions);
        commit('setError', null);
      } catch (error) {
        console.error('Error fetching sessions:', error);
        commit('setError', error.message);
      }
    },

    async launchWorkshop({ commit, dispatch }, workshop) {
      const workshopId = workshop.workshopId || workshop.id;
      commit('setPendingAction', { workshopId, action: 'launching' });

      try {
        const session = await WorkshopService.createSession(
          workshopId,
          workshop.defaultMode,
          workshop.defaultReleaseVersion
        );
        commit('upsertSession', session);
        await dispatch('pollSessionUntilSettled', session.sessionId);
      } finally {
        commit('clearPendingAction', workshopId);
      }
    },

    async restartWorkshop({ commit, dispatch, getters }, { workshopId, rebuild }) {
      const session = getters.activeSessionByWorkshopId(workshopId);
      if (!session) {
        throw new Error('No active session found');
      }

      commit('setPendingAction', {
        workshopId,
        action: rebuild ? 'rebuilding' : 'restarting'
      });

      try {
        const updatedSession = await WorkshopService.restartSession(session.sessionId, rebuild);
        commit('upsertSession', updatedSession);
        await dispatch('pollSessionUntilSettled', updatedSession.sessionId);
      } finally {
        commit('clearPendingAction', workshopId);
      }
    },

    async terminateWorkshop({ commit, dispatch, getters }, workshopId) {
      const session = getters.activeSessionByWorkshopId(workshopId);
      if (!session) {
        return;
      }

      commit('setPendingAction', { workshopId, action: 'terminating' });

      try {
        const updatedSession = await WorkshopService.terminateSession(session.sessionId);
        commit('upsertSession', updatedSession);
        await dispatch('pollSessionUntilSettled', updatedSession.sessionId);
      } finally {
        commit('clearPendingAction', workshopId);
      }
    },

    async pollSessionUntilSettled({ commit }, sessionId) {
      for (let attempt = 0; attempt < 90; attempt += 1) {
        await new Promise(resolve => setTimeout(resolve, 2000));

        const session = await WorkshopService.getSession(sessionId);
        commit('upsertSession', session);

        if (session.state === 'READY' || TERMINAL_SESSION_STATES.has(session.state)) {
          return session;
        }
      }
    }
  }
})
