const BLOCKQUOTE_PATTERN = /^\s*>\s?/;
const ORDERED_LIST_PATTERN = /^\s*\d+\.\s+/;
const UNORDERED_LIST_PATTERN = /^\s*[-*]\s+/;
const CODE_TOKEN = '\u0000CODE';

function escapeHtml(value) {
  return String(value)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

function sanitizeUrl(url) {
  if (typeof url !== 'string') {
    return null;
  }

  const trimmed = url.trim();
  if (!/^https?:\/\//i.test(trimmed)) {
    return null;
  }

  return escapeHtml(trimmed);
}

function renderInlineMarkdown(text) {
  if (!text) {
    return '';
  }

  const codeTokens = [];
  let output = String(text).replace(/`([^`]+)`/g, (_, code) => {
    const token = `${CODE_TOKEN}${codeTokens.length}\u0000`;
    codeTokens.push(`<code>${escapeHtml(code)}</code>`);
    return token;
  });

  output = escapeHtml(output);
  output = output.replace(
    /\[([^\]]+)\]\(([^)\s]+)\)/g,
    (_, label, href) => {
      const safeHref = sanitizeUrl(href);
      if (!safeHref) {
        return escapeHtml(label);
      }

      return `<a href="${safeHref}" target="_blank" rel="noopener noreferrer">${escapeHtml(label)}</a>`;
    }
  );
  output = output.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>');
  output = output.replace(/\*([^*]+)\*/g, '<em>$1</em>');

  codeTokens.forEach((tokenHtml, index) => {
    output = output.replace(`${CODE_TOKEN}${index}\u0000`, tokenHtml);
  });

  return output;
}

function collectParagraph(lines, startIndex) {
  const paragraphLines = [];
  let index = startIndex;

  while (index < lines.length) {
    const line = lines[index];
    if (
      !line.trim() ||
      BLOCKQUOTE_PATTERN.test(line) ||
      ORDERED_LIST_PATTERN.test(line) ||
      UNORDERED_LIST_PATTERN.test(line)
    ) {
      break;
    }

    paragraphLines.push(line.trim());
    index += 1;
  }

  return {
    html: `<p>${renderInlineMarkdown(paragraphLines.join(' '))}</p>`,
    nextIndex: index
  };
}

function collectList(lines, startIndex, ordered) {
  const pattern = ordered ? ORDERED_LIST_PATTERN : UNORDERED_LIST_PATTERN;
  const tag = ordered ? 'ol' : 'ul';
  const items = [];
  let index = startIndex;

  while (index < lines.length) {
    const line = lines[index];
    if (!pattern.test(line)) {
      break;
    }

    items.push(`<li>${renderInlineMarkdown(line.replace(pattern, '').trim())}</li>`);
    index += 1;
  }

  return {
    html: `<${tag}>${items.join('')}</${tag}>`,
    nextIndex: index
  };
}

function collectBlockquote(lines, startIndex) {
  const quoteLines = [];
  let index = startIndex;

  while (index < lines.length) {
    const line = lines[index];
    if (!BLOCKQUOTE_PATTERN.test(line)) {
      break;
    }

    quoteLines.push(line.replace(BLOCKQUOTE_PATTERN, ''));
    index += 1;
  }

  return {
    html: `<blockquote>${renderMarkdown(quoteLines.join('\n'))}</blockquote>`,
    nextIndex: index
  };
}

export function renderMarkdown(markdown) {
  if (!markdown) {
    return '';
  }

  const normalized = String(markdown).replace(/\r\n/g, '\n').trim();
  if (!normalized) {
    return '';
  }

  const lines = normalized.split('\n');
  const blocks = [];
  let index = 0;

  while (index < lines.length) {
    const line = lines[index];
    if (!line.trim()) {
      index += 1;
      continue;
    }

    if (BLOCKQUOTE_PATTERN.test(line)) {
      const blockquote = collectBlockquote(lines, index);
      blocks.push(blockquote.html);
      index = blockquote.nextIndex;
      continue;
    }

    if (ORDERED_LIST_PATTERN.test(line)) {
      const list = collectList(lines, index, true);
      blocks.push(list.html);
      index = list.nextIndex;
      continue;
    }

    if (UNORDERED_LIST_PATTERN.test(line)) {
      const list = collectList(lines, index, false);
      blocks.push(list.html);
      index = list.nextIndex;
      continue;
    }

    const paragraph = collectParagraph(lines, index);
    blocks.push(paragraph.html);
    index = paragraph.nextIndex;
  }

  return blocks.join('');
}
