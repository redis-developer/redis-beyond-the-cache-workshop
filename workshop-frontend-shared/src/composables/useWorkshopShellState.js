const BUSY_STATES = new Set([
  'admitted',
  'admitting',
  'building',
  'child_starting',
  'deploying',
  'initializing',
  'launching',
  'pending',
  'provisioning',
  'rebuilding',
  'restarting',
  'starting'
]);

const READY_STATES = new Set([
  'available',
  'child_ready',
  'ready',
  'running',
  'started'
]);

const BLOCKED_STATES = new Set([
  'cancelled',
  'canceled',
  'error',
  'failed',
  'missing',
  'stopped',
  'terminated',
  'unavailable'
]);

export function normalizeRuntimeState(state) {
  if (state === null || state === undefined || state === '') {
    return 'unknown';
  }

  return String(state).trim().toLowerCase();
}

export function isRuntimeBusy(state) {
  return BUSY_STATES.has(normalizeRuntimeState(state));
}

export function isRuntimeReady(state) {
  return READY_STATES.has(normalizeRuntimeState(state));
}

export function isRuntimeBlocked(state) {
  return BLOCKED_STATES.has(normalizeRuntimeState(state));
}

export function createActionSet(actions) {
  if (Array.isArray(actions)) {
    return new Set(actions
      .filter(action => {
        if (!action) {
          return false;
        }
        if (typeof action === 'object' && action.available === false) {
          return false;
        }
        return true;
      })
      .map(action => canonicalActionName(typeof action === 'object' ? action.name : action))
      .filter(Boolean));
  }

  if (actions && typeof actions === 'object') {
    return new Set(
      Object.entries(actions)
        .filter(([, enabled]) => enabled !== false)
        .map(([action]) => canonicalActionName(action))
        .filter(Boolean)
    );
  }

  return new Set();
}

export function resolveWorkshopShellState({ runtime = {}, learnerApp = {}, links = {}, actions = null } = {}) {
  const runtimeState = normalizeRuntimeState(runtime.state);
  const frontendState = normalizeRuntimeState(runtime.frontendState ?? runtime.frontend?.state ?? learnerApp.state);
  const backendState = normalizeRuntimeState(runtime.backendState ?? runtime.backend?.state);
  const actionSet = createActionSet(actions ?? runtime.availableActions ?? runtime.actions);
  const hasDeclaredActions = actionSet.size > 0;
  const learnerAppUrl = learnerApp.url ?? links.learnerApp ?? '';
  const redisInsightUrl = links.redisInsight ?? '';
  const editorUrl = links.editor ?? links.codeEditor ?? '';
  const hubUrl = links.hub ?? links.workshopHub ?? '/';
  const busy = isRuntimeBusy(runtimeState) || isRuntimeBusy(frontendState) || isRuntimeBusy(backendState);
  const appUnavailable = isRuntimeBlocked(frontendState) || isRuntimeBlocked(runtimeState);

  return {
    runtimeState,
    frontendState,
    backendState,
    learnerAppUrl,
    redisInsightUrl,
    editorUrl,
    hubUrl,
    busy,
    ready: isRuntimeReady(runtimeState),
    learnerAppReady: Boolean(learnerAppUrl) && !busy && !appUnavailable,
    learnerAppUnavailable: !learnerAppUrl || appUnavailable,
    canRestart: supportsAction(actionSet, hasDeclaredActions, 'restart') && !busy,
    canRebuild: runtime.rebuildAvailable !== false && supportsAction(actionSet, hasDeclaredActions, 'rebuild') && !busy,
    canOpenRedisInsight: Boolean(redisInsightUrl) && supportsAction(actionSet, hasDeclaredActions, 'redisInsight'),
    canOpenEditor: Boolean(editorUrl) && supportsAction(actionSet, hasDeclaredActions, 'editor'),
    canOpenHub: Boolean(hubUrl) && supportsAction(actionSet, hasDeclaredActions, 'hub'),
    canRefresh: supportsAction(actionSet, hasDeclaredActions, 'refresh')
  };
}

function supportsAction(actionSet, hasDeclaredActions, action) {
  if (!hasDeclaredActions) {
    return true;
  }

  return actionSet.has(action);
}

function canonicalActionName(action) {
  if (!action) {
    return '';
  }

  const name = String(action);
  const aliases = {
    openHub: 'hub',
    openEditor: 'editor',
    openRedisInsight: 'redisInsight',
    rebuildRuntime: 'rebuild',
    refreshStatus: 'refresh',
    restartRuntime: 'restart',
    terminateSession: 'terminate'
  };

  return aliases[name] || name;
}
