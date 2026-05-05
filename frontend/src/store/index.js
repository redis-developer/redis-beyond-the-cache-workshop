import { createStore } from 'vuex'
import PortalService from '@/services/PortalService'
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
const SETTLED_SESSION_STATES = new Set(['READY', ...TERMINAL_SESSION_STATES]);
const POLL_INTERVAL_MS = 2000;
const DEFAULT_POLL_TIMEOUT_MS = 5 * 60 * 1000;
const REBUILD_POLL_TIMEOUT_MS = 10 * 60 * 1000;
const TERMINATION_POLL_TIMEOUT_MS = 2 * 60 * 1000;

function delay(milliseconds) {
  return new Promise(resolve => setTimeout(resolve, milliseconds));
}

function isActiveSession(session) {
  return session && ACTIVE_SESSION_STATES.has(session.state);
}

function isSettledSession(session) {
  return session && SETTLED_SESSION_STATES.has(session.state);
}

function clearPendingActionsForSettledSessions(pendingActions, sessions) {
  const settledWorkshopIds = new Set(
    sessions
      .filter(isSettledSession)
      .map(session => session.workshopId)
      .filter(Boolean)
  );

  if (settledWorkshopIds.size === 0) {
    return pendingActions;
  }

  let changed = false;
  const nextPendingActions = { ...pendingActions };
  settledWorkshopIds.forEach(workshopId => {
    if (Object.prototype.hasOwnProperty.call(nextPendingActions, workshopId)) {
      delete nextPendingActions[workshopId];
      changed = true;
    }
  });

  return changed ? nextPendingActions : pendingActions;
}

function sessionSortValue(session) {
  return Date.parse(session.createdAt || '') || 0;
}

function activeSessionFor(workshopId, sessions) {
  return sessions
    .filter(session => session.workshopId === workshopId && isActiveSession(session))
    .sort((left, right) => sessionSortValue(right) - sessionSortValue(left))[0] || null;
}

function activeSession(sessions) {
  return sessions
    .filter(isActiveSession)
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
    portalUser: null,
    portalLoaded: false,
    portalLoading: false,
    portalError: null,
    workshops: [],
    sessions: [],
    pendingActions: {},
    loading: false,
    error: null
  },

  getters: {
    allWorkshops: (state) => {
      const currentActiveSession = activeSession(state.sessions);
      return state.workshops.map(workshop => {
        const workshopActiveSession = activeSessionFor(workshop.workshopId, state.sessions);
        const blockingSession = currentActiveSession?.workshopId === workshop.workshopId
          ? null
          : currentActiveSession;
        const blockingWorkshop = blockingSession
          ? state.workshops.find(candidate => candidate.workshopId === blockingSession.workshopId)
          : null;
        return {
          ...workshop,
          id: workshop.workshopId,
          activeSession: workshopActiveSession,
          blockingSession,
          blockingWorkshopTitle: blockingWorkshop?.title || null,
          status: displayStatus(workshopActiveSession),
          url: workshopActiveSession?.publicEntryUrl || workshop.path || '#',
          pendingAction: state.pendingActions[workshop.workshopId] || null
        };
      });
    },

    isPortalAuthenticated: (state) => {
      return Boolean(state.portalUser);
    },

    activeSessionByWorkshopId: (state) => (workshopId) => {
      return activeSessionFor(workshopId, state.sessions);
    },

    activeSession: (state) => {
      return activeSession(state.sessions);
    },

    isWorkshopBusy: (state) => (workshopId) => {
      return Boolean(state.pendingActions[workshopId]);
    }
  },

  mutations: {
    setPortalUser(state, user) {
      state.portalUser = user;
    },

    setPortalLoaded(state, loaded) {
      state.portalLoaded = loaded;
    },

    setPortalLoading(state, loading) {
      state.portalLoading = loading;
    },

    setPortalError(state, error) {
      state.portalError = error;
    },

    setWorkshops(state, workshops) {
      state.workshops = workshops;
    },

    setSessions(state, sessions) {
      state.sessions = sessions;
      state.pendingActions = clearPendingActionsForSettledSessions(state.pendingActions, sessions);
    },

    upsertSession(state, session) {
      state.sessions = upsertSession(state.sessions, session);
      if (isSettledSession(session)) {
        state.pendingActions = clearPendingActionsForSettledSessions(state.pendingActions, [session]);
      }
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
    },

    resetWorkshopState(state) {
      state.workshops = [];
      state.sessions = [];
      state.pendingActions = {};
      state.loading = false;
      state.error = null;
    }
  },

  actions: {
    async loadPortalUser({ commit }) {
      commit('setPortalLoading', true);

      try {
        const user = await PortalService.getCurrentUser();
        commit('setPortalUser', user);
        commit('setPortalError', null);
        return user;
      } catch (error) {
        commit('setPortalUser', null);
        if (error.status === 401 || error.status === 403) {
          commit('setPortalError', null);
        } else {
          commit('setPortalError', error.message);
        }
        return null;
      } finally {
        commit('setPortalLoaded', true);
        commit('setPortalLoading', false);
      }
    },

    async loginToPortal({ commit }, credentials) {
      commit('setPortalLoading', true);
      commit('setPortalError', null);

      try {
        const email = typeof credentials === 'string' ? credentials : credentials.email;
        const marketingAllowed = typeof credentials === 'string'
          ? true
          : credentials.marketingAllowed;
        const user = await PortalService.login(email, marketingAllowed);
        if (!user) {
          throw new Error('Portal login did not return an authenticated user');
        }
        commit('setPortalUser', user);
        commit('setPortalLoaded', true);
        return user;
      } catch (error) {
        commit('setPortalUser', null);
        commit('setPortalError', error.message);
        throw error;
      } finally {
        commit('setPortalLoading', false);
      }
    },

    async logoutFromPortal({ commit }) {
      try {
        await PortalService.logout();
      } finally {
        commit('setPortalUser', null);
        commit('setPortalLoaded', true);
        commit('setPortalError', null);
        commit('resetWorkshopState');
      }
    },

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

    async launchWorkshop({ commit, dispatch, state }, payload) {
      const workshop = payload?.workshop || payload;
      const sessionEnvironment = payload?.workshop ? payload.sessionEnvironment : {};
      const workshopId = workshop.workshopId || workshop.id;
      const blockingSession = activeSession(state.sessions);
      if (blockingSession && blockingSession.workshopId !== workshopId) {
        const message = 'Stop your active workshop before deploying another one.';
        const error = new Error(message);
        error.code = 'active_session_exists';
        error.blockingSession = blockingSession;
        throw error;
      }

      commit('setPendingAction', { workshopId, action: 'launching' });

      try {
        const session = await WorkshopService.createSession(
          workshopId,
          workshop.defaultMode,
          workshop.defaultReleaseVersion,
          sessionEnvironment
        );
        commit('upsertSession', session);
        await dispatch('pollSessionUntilSettled', {
          sessionId: session.sessionId,
          timeoutMs: DEFAULT_POLL_TIMEOUT_MS
        });
        commit('setError', null);
      } catch (error) {
        if (error.data?.code !== 'active_session_exists') {
          commit('setError', error.message);
        }
        throw error;
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
        await dispatch('pollSessionUntilSettled', {
          sessionId: updatedSession.sessionId,
          timeoutMs: rebuild ? REBUILD_POLL_TIMEOUT_MS : DEFAULT_POLL_TIMEOUT_MS
        });
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
        await dispatch('pollSessionUntilSettled', {
          sessionId: updatedSession.sessionId,
          timeoutMs: TERMINATION_POLL_TIMEOUT_MS
        });
      } finally {
        commit('clearPendingAction', workshopId);
      }
    },

    async pollSessionUntilSettled({ commit }, payload) {
      const sessionId = typeof payload === 'string' ? payload : payload.sessionId;
      const timeoutMs = typeof payload === 'string'
        ? DEFAULT_POLL_TIMEOUT_MS
        : (payload.timeoutMs || DEFAULT_POLL_TIMEOUT_MS);
      const deadline = Date.now() + timeoutMs;

      while (Date.now() < deadline) {
        await delay(POLL_INTERVAL_MS);

        const session = await WorkshopService.getSession(sessionId);
        commit('upsertSession', session);

        if (session.state === 'READY' || TERMINAL_SESSION_STATES.has(session.state)) {
          return session;
        }
      }

      const session = await WorkshopService.getSession(sessionId);
      commit('upsertSession', session);
      return session;
    }
  }
})
