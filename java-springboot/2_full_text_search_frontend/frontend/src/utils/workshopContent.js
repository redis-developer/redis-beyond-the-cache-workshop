import { getApiUrl } from './basePath';

export async function fetchWorkshopContent(viewId) {
  const response = await fetch(getApiUrl(`/api/content/views/${encodeURIComponent(viewId)}`), {
    credentials: 'include'
  });

  if (!response.ok) {
    let message = `Failed to load workshop content (${response.status})`;

    try {
      const errorBody = await response.json();
      if (errorBody?.message) {
        message = errorBody.message;
      }
    } catch (error) {
      // Ignore parse failures and use the default message.
    }

    throw new Error(message);
  }

  return response.json();
}

export function getContentStageNavSteps(content) {
  if (Array.isArray(content?.header?.stageNav?.steps) && content.header.stageNav.steps.length) {
    return content.header.stageNav.steps;
  }

  if (Array.isArray(content?.stages)) {
    return content.stages.map(stage => ({
      stageId: stage.stageId,
      label: stage.title || stage.stageId
    }));
  }

  return [];
}

export function resolveContentStageId(content, requestedStageId = '') {
  const stages = Array.isArray(content?.stages) ? content.stages : [];
  if (!stages.length) {
    return '';
  }

  if (requestedStageId && stages.some(stage => stage.stageId === requestedStageId)) {
    return requestedStageId;
  }

  return stages[0].stageId;
}

export function getContentStageIndex(content, activeStageId = '') {
  const stageSteps = getContentStageNavSteps(content);
  const stageIndex = stageSteps.findIndex(step => step.stageId === activeStageId);

  return stageIndex >= 0 ? stageIndex : 0;
}
