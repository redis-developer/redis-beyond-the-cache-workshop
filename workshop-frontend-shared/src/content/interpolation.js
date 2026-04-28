const PLACEHOLDER_PATTERN = /\{\{\s*([^{}]+?)\s*\}\}/g;
const MARKDOWN_LINK_PATTERN = /\[([^\]]+)\]\(([^)]*)\)/g;
const PLACEHOLDER_PATH_PATTERN = /^[a-zA-Z][a-zA-Z0-9]*(?:\.[a-zA-Z][a-zA-Z0-9]*)*$/;
const UNSAFE_PATH_SEGMENTS = new Set(['__proto__', 'prototype', 'constructor']);

export const DEFAULT_ALLOWED_PLACEHOLDER_PATHS = Object.freeze([
  'session.id',
  'session.previousId',
  'session.changed',
  'links.learnerApp',
  'links.redisInsight',
  'links.hub',
  'links.workshopHub',
  'links.editor',
  'links.backendApi',
  'runtime.status',
  'runtime.state',
  'runtime.message',
  'runtime.backendStatus',
  'runtime.frontendStatus',
  'status.message',
  'status.detail',
  'status.ready',
  'status.healthy',
  'sessionId',
  'previousSessionId',
  'sessionIdChanged'
]);

function isRecord(value) {
  return Boolean(value) && typeof value === 'object' && !Array.isArray(value);
}

function hasOwn(value, key) {
  return Object.prototype.hasOwnProperty.call(Object(value), key);
}

function addUnique(target, value) {
  if (!target.includes(value)) {
    target.push(value);
  }
}

function isSafePlaceholderPath(path) {
  if (!PLACEHOLDER_PATH_PATTERN.test(path)) {
    return false;
  }

  return path.split('.').every(part => !UNSAFE_PATH_SEGMENTS.has(part));
}

function createAllowedPathSet(extraPaths = []) {
  const allowedPaths = new Set(DEFAULT_ALLOWED_PLACEHOLDER_PATHS);

  extraPaths.forEach(path => {
    if (typeof path === 'string' && isSafePlaceholderPath(path)) {
      allowedPaths.add(path);
    }
  });

  return allowedPaths;
}

function getPathValue(context, path) {
  return path.split('.').reduce((value, part) => {
    if (value === null || value === undefined) {
      return undefined;
    }

    if (hasOwn(value, part)) {
      return value[part];
    }

    return undefined;
  }, context);
}

function isScalarValue(value) {
  return ['string', 'number', 'boolean'].includes(typeof value);
}

function extractPlaceholderPaths(value) {
  const paths = [];
  PLACEHOLDER_PATTERN.lastIndex = 0;

  for (const match of String(value).matchAll(PLACEHOLDER_PATTERN)) {
    paths.push(match[1].trim());
  }

  return paths;
}

function collectMarkdownLinks(markdown) {
  const links = [];
  MARKDOWN_LINK_PATTERN.lastIndex = 0;

  for (const match of String(markdown).matchAll(MARKDOWN_LINK_PATTERN)) {
    links.push({
      label: match[1],
      href: match[2]
    });
  }

  return links;
}

function isSafeMarkdownUrl(value) {
  if (typeof value !== 'string' || /[\u0000-\u001F\u007F]/.test(value)) {
    return false;
  }

  const trimmed = value.trim();
  if (trimmed.startsWith('/') && !trimmed.startsWith('//')) {
    return true;
  }

  try {
    const url = new URL(trimmed);
    return url.protocol === 'http:' || url.protocol === 'https:';
  } catch {
    return false;
  }
}

function validateDynamicMarkdownLinks(source, interpolated, issues) {
  const sourceLinks = collectMarkdownLinks(source);
  const interpolatedLinks = collectMarkdownLinks(interpolated);

  sourceLinks.forEach((sourceLink, index) => {
    if (!sourceLink.href.includes('{{')) {
      return;
    }

    const interpolatedHref = interpolatedLinks[index]?.href || '';
    if (interpolatedHref.includes('{{')) {
      return;
    }

    if (!isSafeMarkdownUrl(interpolatedHref)) {
      const placeholderPath = extractPlaceholderPaths(sourceLink.href)[0] || sourceLink.href;
      addUnique(issues.errors, `Invalid dynamic markdown link for placeholder: ${placeholderPath}`);
    }
  });
}

export function createContentInterpolationContext(options = {}) {
  const tokens = isRecord(options.tokens) ? options.tokens : {};
  const context = isRecord(options.context) ? options.context : {};

  const interpolationContext = {
    session: {
      ...(isRecord(tokens.session) ? tokens.session : {}),
      ...(isRecord(context.session) ? context.session : {})
    },
    links: {
      ...(isRecord(tokens.links) ? tokens.links : {}),
      ...(isRecord(context.links) ? context.links : {})
    },
    runtime: {
      ...(isRecord(tokens.runtime) ? tokens.runtime : {}),
      ...(isRecord(context.runtime) ? context.runtime : {})
    },
    status: {
      ...(isRecord(tokens.status) ? tokens.status : {}),
      ...(isRecord(context.status) ? context.status : {})
    }
  };

  if (tokens.sessionId !== undefined) {
    interpolationContext.sessionId = tokens.sessionId;
    if (interpolationContext.session.id === undefined) {
      interpolationContext.session.id = tokens.sessionId;
    }
  }

  if (tokens.previousSessionId !== undefined) {
    interpolationContext.previousSessionId = tokens.previousSessionId;
    if (interpolationContext.session.previousId === undefined) {
      interpolationContext.session.previousId = tokens.previousSessionId;
    }
  }

  if (tokens.sessionIdChanged !== undefined) {
    interpolationContext.sessionIdChanged = tokens.sessionIdChanged;
    if (interpolationContext.session.changed === undefined) {
      interpolationContext.session.changed = tokens.sessionIdChanged;
    }
  }

  return interpolationContext;
}

export function interpolateString(value, options = {}) {
  if (typeof value !== 'string') {
    return value ?? '';
  }

  const issues = options.issues || {
    errors: [],
    missingPlaceholders: [],
    unsupportedPlaceholders: []
  };
  const allowedPaths = options.allowedPaths || createAllowedPathSet(options.allowedPlaceholderPaths);
  const context = options.context || {};

  const interpolated = value.replace(PLACEHOLDER_PATTERN, (match, rawPath) => {
    const path = rawPath.trim();

    if (!isSafePlaceholderPath(path) || !allowedPaths.has(path)) {
      addUnique(issues.unsupportedPlaceholders, path);
      return match;
    }

    const resolvedValue = getPathValue(context, path);
    if (resolvedValue === undefined || resolvedValue === null) {
      addUnique(issues.missingPlaceholders, path);
      return match;
    }

    if (!isScalarValue(resolvedValue)) {
      addUnique(issues.errors, `Placeholder must resolve to a scalar value: ${path}`);
      return match;
    }

    return String(resolvedValue);
  });

  if (options.validateMarkdownLinks) {
    validateDynamicMarkdownLinks(value, interpolated, issues);
  }

  return interpolated;
}

export function createStringInterpolator(context, issues, options = {}) {
  const allowedPaths = createAllowedPathSet(options.allowedPlaceholderPaths || []);

  return (value, stringOptions = {}) => interpolateString(value, {
    context,
    issues,
    allowedPaths,
    validateMarkdownLinks: Boolean(stringOptions.validateMarkdownLinks)
  });
}
