import {
  createContentInterpolationContext,
  createStringInterpolator
} from './interpolation.js';
import {
  createWidgetBlock,
  expandMarkdownFirstBlocks
} from './markdownFirst.js';

const PAGE_TYPES = new Set(['narrative', 'stage-flow', 'editor']);
const BLOCK_TYPES = new Set([
  'markdown',
  'callout',
  'statusPanel',
  'actionRow',
  'stepList',
  'editorStepList',
  'codeSnippet',
  'widget'
]);

function unique(values) {
  return [...new Set(values)];
}

function resolveMarkdownBody(value, resolveString) {
  return resolveString(value, {
    validateMarkdownLinks: true
  });
}

function mapAction(action, context, resolveString) {
  if (!action || typeof action !== 'object') {
    return null;
  }

  return {
    id: action.id,
    label: resolveString(action.label || action.id || 'Action'),
    args: action.args || {},
    context
  };
}

function mapMarkdownBlocks(body, resolveString, issues) {
  return expandMarkdownFirstBlocks(body, issues).map(block => {
    if (block.type === 'widget') {
      return block;
    }

    return {
      type: 'markdown',
      body: resolveMarkdownBody(block.body, resolveString)
    };
  });
}

function mapBlock(block, context, resolveString, issues) {
  if (!block || !BLOCK_TYPES.has(block.type)) {
    issues.errors.push(`Unsupported block type: ${block?.type || 'unknown'}`);
    return [];
  }

  const blockContext = {
    ...context,
    blockType: block.type
  };

  if (block.type === 'markdown') {
    return mapMarkdownBlocks(block.body, resolveString, issues);
  }

  if (block.type === 'callout') {
    return [{
      type: 'callout',
      tone: block.tone || 'info',
      title: resolveString(block.title || ''),
      body: resolveMarkdownBody(block.body, resolveString),
      actions: (block.actions || [])
        .map(action => mapAction(action, blockContext, resolveString))
        .filter(Boolean)
    }];
  }

  if (block.type === 'statusPanel') {
    return [{
      type: 'statusPanel',
      tone: block.tone || 'info',
      title: resolveString(block.title || ''),
      body: resolveMarkdownBody(block.body, resolveString),
      actions: (block.actions || [])
        .map(action => mapAction(action, blockContext, resolveString))
        .filter(Boolean)
    }];
  }

  if (block.type === 'actionRow') {
    return [{
      type: 'actionRow',
      actions: (block.actions || [])
        .map(action => mapAction(action, blockContext, resolveString))
        .filter(Boolean)
    }];
  }

  if (block.type === 'codeSnippet') {
    return [{
      type: 'codeSnippet',
      language: block.language || 'text',
      title: resolveString(block.title || ''),
      caption: resolveMarkdownBody(block.caption || '', resolveString),
      code: block.code || ''
    }];
  }

  if (block.type === 'widget') {
    const widgetBlock = createWidgetBlock(block.widgetId, issues);
    return widgetBlock ? [widgetBlock] : [];
  }

  if (block.type === 'stepList') {
    return [{
      type: 'stepList',
      listId: block.listId,
      variant: block.variant || 'ordered',
      items: (block.items || []).map(item => ({
        itemId: item.itemId,
        title: resolveString(item.title || ''),
        body: resolveMarkdownBody(item.body, resolveString),
        hint: resolveMarkdownBody(item.hint || '', resolveString),
        actions: (item.actions || [])
          .map(action => mapAction(action, {
            ...blockContext,
            listId: block.listId,
            itemId: item.itemId
          }, resolveString))
          .filter(Boolean),
        supportingBlocks: (item.supportingBlocks || [])
          .flatMap(supportingBlock => mapBlock(
            supportingBlock,
            {
              ...blockContext,
              listId: block.listId,
              itemId: item.itemId,
              parentBlockType: 'stepList'
            },
            resolveString,
            issues
          ))
      }))
    }];
  }

  if (block.type === 'editorStepList') {
    return [{
      type: 'editorStepList',
      listId: block.listId,
      title: resolveString(block.title || ''),
      description: resolveMarkdownBody(block.description || '', resolveString),
      startAt: Number.isFinite(block.startAt) ? block.startAt : 1,
      progressGroup: block.progressGroup || '',
      items: (block.items || []).map(item => ({
        itemId: item.itemId,
        body: resolveMarkdownBody(item.body, resolveString),
        hint: resolveMarkdownBody(item.hint || '', resolveString),
        action: mapAction(item.action, {
          ...blockContext,
          listId: block.listId,
          itemId: item.itemId
        }, resolveString)
      }))
    }];
  }

  issues.errors.push(`Unsupported block type: ${block.type}`);
  return [];
}

function getSectionBlocks(section) {
  const blocks = [];

  if (typeof section.markdown === 'string') {
    blocks.push({
      type: 'markdown',
      body: section.markdown
    });
  }

  if (Array.isArray(section.blocks)) {
    blocks.push(...section.blocks);
  }

  return blocks;
}

function mapSection(section, context, resolveString, issues) {
  return {
    sectionId: section.sectionId,
    title: resolveString(section.title || ''),
    body: resolveMarkdownBody(section.body || '', resolveString),
    blocks: getSectionBlocks(section)
      .flatMap(block => mapBlock(block, {
        ...context,
        sectionId: section.sectionId
      }, resolveString, issues))
  };
}

function getActiveStage(content, activeStageId, issues) {
  const stages = Array.isArray(content.stages) ? content.stages : [];
  if (!stages.length) {
    issues.errors.push('Stage flow content requires at least one stage.');
    return null;
  }

  if (!activeStageId) {
    return stages[0];
  }

  const activeStage = stages.find(stage => stage.stageId === activeStageId);
  if (!activeStage) {
    issues.errors.push(`Unknown stage id: ${activeStageId}`);
    return stages[0];
  }

  return activeStage;
}

function getSectionsSource(content, stage) {
  if (stage) {
    return stage.sections;
  }

  if (Array.isArray(content.sections)) {
    return content.sections;
  }

  if (typeof content.markdown === 'string') {
    return [{
      sectionId: 'content',
      markdown: content.markdown
    }];
  }

  return [];
}

function createEmptyModel(errors) {
  return {
    viewId: '',
    pageType: '',
    title: '',
    summary: '',
    sections: [],
    stage: null,
    stageNavItems: [],
    errors,
    missingPlaceholders: [],
    unsupportedPlaceholders: [],
    missingTokens: []
  };
}

export function getStageNavItems(content) {
  if (content?.header?.stageNav?.steps?.length) {
    return content.header.stageNav.steps.map(step => ({
      stageId: step.stageId,
      label: step.label || step.stageId
    }));
  }

  if (Array.isArray(content?.stages)) {
    return content.stages.map(stage => ({
      stageId: stage.stageId,
      label: stage.title || stage.stageId
    }));
  }

  return [];
}

export function createContentRenderModel(content, options = {}) {
  const issues = {
    errors: [],
    missingPlaceholders: [],
    unsupportedPlaceholders: []
  };
  const context = createContentInterpolationContext({
    tokens: options.tokens,
    context: options.context
  });
  const resolveString = createStringInterpolator(context, issues, {
    allowedPlaceholderPaths: options.allowedPlaceholderPaths || []
  });

  if (!content || typeof content !== 'object') {
    return createEmptyModel(['Content payload must be an object.']);
  }

  if (!PAGE_TYPES.has(content.pageType)) {
    issues.errors.push(`Unsupported page type: ${content.pageType || 'unknown'}`);
  }

  const stage = content.pageType === 'stage-flow'
    ? getActiveStage(content, options.activeStageId, issues)
    : null;
  const sectionsSource = getSectionsSource(content, stage);
  const sections = Array.isArray(sectionsSource)
    ? sectionsSource.map(section => mapSection(
      section,
      {
        viewId: content.viewId,
        pageType: content.pageType,
        stageId: stage?.stageId || null
      },
      resolveString,
      issues
    ))
    : [];

  if (!sections.length) {
    issues.errors.push('No renderable sections were found for this view.');
  }

  const missingPlaceholders = unique(issues.missingPlaceholders);

  return {
    viewId: content.viewId || '',
    route: content.route || '',
    pageType: content.pageType || '',
    title: resolveString(content.title || ''),
    summary: resolveMarkdownBody(content.summary || '', resolveString),
    stage: stage
      ? {
        stageId: stage.stageId,
        title: resolveString(stage.title || '')
      }
      : null,
    sections,
    header: content.header || {},
    stageNavItems: getStageNavItems(content),
    errors: unique(issues.errors),
    missingPlaceholders,
    unsupportedPlaceholders: unique(issues.unsupportedPlaceholders),
    missingTokens: missingPlaceholders
  };
}

export function bindContentAction(actionHandlers, payload) {
  const handler = actionHandlers?.[payload.actionId];
  if (typeof handler === 'function') {
    return handler(payload);
  }

  return undefined;
}
