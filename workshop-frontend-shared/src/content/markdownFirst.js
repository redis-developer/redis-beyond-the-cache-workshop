const WIDGET_DECLARATION_PATTERN = /^\s*::widget\s*\{\s*([^}]*)\s*\}\s*$/;
const WIDGET_DIRECTIVE_PATTERN = /^\s*::widget\b/;
const WIDGET_ID_PATTERN = /^[a-zA-Z0-9][a-zA-Z0-9._:-]{0,127}$/;
const UNSAFE_WIDGET_ID_PARTS = new Set(['__proto__', 'prototype', 'constructor']);

function addUnique(target, value) {
  if (!target.includes(value)) {
    target.push(value);
  }
}

function isSafeWidgetId(widgetId) {
  if (!WIDGET_ID_PATTERN.test(widgetId)) {
    return false;
  }

  return widgetId
    .split(/[.:]/)
    .every(part => part && !UNSAFE_WIDGET_ID_PARTS.has(part));
}

function parseAttributes(rawAttributes) {
  const attributes = {};
  let remaining = rawAttributes.trim();

  while (remaining) {
    const match = remaining.match(/^([a-zA-Z][a-zA-Z0-9]*)="([^"]*)"\s*/);
    if (!match) {
      return {
        attributes,
        error: 'Widget declarations only support quoted attributes.'
      };
    }

    const [, name, value] = match;
    if (Object.prototype.hasOwnProperty.call(attributes, name)) {
      return {
        attributes,
        error: `Duplicate widget declaration attribute: ${name}`
      };
    }

    attributes[name] = value;
    remaining = remaining.slice(match[0].length).trimStart();
  }

  return {
    attributes,
    error: ''
  };
}

export function createWidgetBlock(widgetId, issues) {
  if (typeof widgetId !== 'string' || !isSafeWidgetId(widgetId)) {
    addUnique(issues.errors, `Invalid widget id: ${widgetId || 'missing'}`);
    return null;
  }

  return {
    type: 'widget',
    widgetId
  };
}

export function parseWidgetDeclaration(line, issues) {
  if (!WIDGET_DIRECTIVE_PATTERN.test(line)) {
    return null;
  }

  const match = line.match(WIDGET_DECLARATION_PATTERN);
  if (!match) {
    addUnique(issues.errors, 'Invalid widget declaration syntax.');
    return null;
  }

  const { attributes, error } = parseAttributes(match[1]);
  if (error) {
    addUnique(issues.errors, error);
    return null;
  }

  const attributeNames = Object.keys(attributes);
  const unsupportedAttributes = attributeNames.filter(name => name !== 'id');
  unsupportedAttributes.forEach(name => {
    addUnique(issues.errors, `Unsupported widget declaration attribute: ${name}`);
  });

  if (unsupportedAttributes.length) {
    return null;
  }

  if (!attributes.id) {
    addUnique(issues.errors, 'Widget declaration requires an id attribute.');
    return null;
  }

  return createWidgetBlock(attributes.id, issues);
}

export function expandMarkdownFirstBlocks(markdown, issues) {
  const lines = String(markdown || '').replace(/\r\n/g, '\n').split('\n');
  const blocks = [];
  let markdownLines = [];

  const flushMarkdown = () => {
    const body = markdownLines.join('\n').trim();
    if (body) {
      blocks.push({
        type: 'markdown',
        body
      });
    }

    markdownLines = [];
  };

  lines.forEach(line => {
    if (!WIDGET_DIRECTIVE_PATTERN.test(line)) {
      markdownLines.push(line);
      return;
    }

    const widgetBlock = parseWidgetDeclaration(line, issues);
    if (!widgetBlock) {
      markdownLines.push(line);
      return;
    }

    flushMarkdown();
    blocks.push(widgetBlock);
  });

  flushMarkdown();

  return blocks;
}
