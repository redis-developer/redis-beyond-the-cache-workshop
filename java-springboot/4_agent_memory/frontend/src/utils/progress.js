/**
 * Workshop progress tracking utility.
 * Stores progress in localStorage to persist across page reloads.
 */

const STORAGE_KEY = 'workshop_progress_agent_memory';

export function loadProgress() {
  const stored = localStorage.getItem(STORAGE_KEY);
  return stored ? JSON.parse(stored) : { currentStage: 1, completedSteps: [] };
}

export function saveProgress(progress) {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(progress));
}

export function clearProgress() {
  localStorage.removeItem(STORAGE_KEY);
}

export function completeStep(step) {
  const progress = loadProgress();
  if (!progress.completedSteps.includes(step)) {
    progress.completedSteps.push(step);
  }
  saveProgress(progress);
  return progress;
}

export function isStepCompleted(step) {
  const progress = loadProgress();
  return progress.completedSteps.includes(step);
}

