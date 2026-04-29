import { getApiUrl } from './basePath.js';

export async function loadEditorFilePathMap(options = {}) {
  const metadata = await loadEditorWorkspaceMetadata(options);

  return metadata.filePathMap;
}

export async function loadEditorWorkspaceMetadata(options = {}) {
  const fetchImpl = options.fetch || globalThis.fetch;
  if (!fetchImpl) {
    return emptyEditorWorkspaceMetadata();
  }

  const endpoint = options.endpoint || '/api/editor/files';
  const response = await fetchImpl(getApiUrl(endpoint), {
    credentials: 'include'
  });

  if (!response.ok) {
    throw new Error(`Failed to load editor files: ${response.status}`);
  }

  const metadata = await response.json();
  const workspaceRoot = normalizeAbsolutePath(metadata?.workspaceRoot);
  const codeEditorWorkspaceRoot = normalizeAbsolutePath(metadata?.codeEditorWorkspaceRoot) || workspaceRoot;
  const files = Array.isArray(metadata?.files) ? metadata.files : [];
  const filePathMap = {};

  files.forEach(file => {
    const name = typeof file?.name === 'string' ? file.name : '';
    const path = resolveEditorFilePath(file, codeEditorWorkspaceRoot || workspaceRoot);

    if (name && path) {
      filePathMap[name] = path;
    }
  });

  return {
    workspaceRoot,
    codeEditorWorkspaceRoot,
    filePathMap
  };
}

export function resolveEditorFilePath(file, workspaceRoot = '') {
  const codeEditorWorkspacePath = normalizeAbsolutePath(file?.codeEditorWorkspacePath);
  if (codeEditorWorkspacePath) {
    return codeEditorWorkspacePath;
  }

  const workspacePath = normalizeAbsolutePath(file?.workspacePath);
  if (workspacePath) {
    return workspacePath;
  }

  const relativePath = normalizeRelativePath(file?.path);
  if (workspaceRoot && relativePath) {
    return `${workspaceRoot}/${relativePath}`;
  }

  return '';
}

function normalizeAbsolutePath(path) {
  if (typeof path !== 'string') {
    return '';
  }

  const trimmed = path.trim();
  if (!trimmed.startsWith('/')) {
    return '';
  }

  return trimmed.replace(/\/+/g, '/').replace(/\/$/, '');
}

function normalizeRelativePath(path) {
  if (typeof path !== 'string') {
    return '';
  }

  const trimmed = path.trim();
  if (!trimmed || trimmed.startsWith('/')) {
    return '';
  }

  const segments = trimmed.split('/').filter(segment => segment && segment !== '.');
  if (segments.some(segment => segment === '..')) {
    return '';
  }

  return segments.join('/');
}

function emptyEditorWorkspaceMetadata() {
  return {
    workspaceRoot: '',
    codeEditorWorkspaceRoot: '',
    filePathMap: {}
  };
}
