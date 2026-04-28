export const SESSION_SHELL_ACTIONS = Object.freeze({
  OPEN_LEARNER_APP: 'openLearnerApp',
  OPEN_REDIS_INSIGHT: 'openRedisInsight',
  RESTART_RUNTIME: 'restartRuntime',
  REBUILD_RUNTIME: 'rebuildRuntime',
  TERMINATE_SESSION: 'terminateSession'
});

export const SESSION_SHELL_COMPONENT_STATES = Object.freeze({
  PENDING: 'PENDING',
  STARTING: 'STARTING',
  READY: 'READY',
  DEGRADED: 'DEGRADED',
  STOPPING: 'STOPPING',
  STOPPED: 'STOPPED',
  FAILED: 'FAILED'
});
