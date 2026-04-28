import assert from 'node:assert/strict';
import { execFileSync } from 'node:child_process';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

import {
  createContentRenderModel,
  getStageNavItems
} from '../src/content/renderModel.js';
import { renderMarkdown } from '../src/content-renderer/markdown.js';
import {
  markdownContentFixture,
  markdownContextFixture
} from './fixtures/markdown-content-context.mjs';

const scriptDirectory = path.dirname(fileURLToPath(import.meta.url));
const repositoryRoot = path.resolve(scriptDirectory, '../..');

function loadYamlFixture(relativePath) {
  const filePath = path.join(repositoryRoot, relativePath);
  const rubyProgram = [
    'require "json"',
    'require "yaml"',
    'content = YAML.safe_load(File.read(ARGV[0]), permitted_classes: [], aliases: false)',
    'puts JSON.dump(content)'
  ].join('; ');

  return JSON.parse(
    execFileSync('ruby', ['-e', rubyProgram, filePath], {
      encoding: 'utf8'
    })
  );
}

function verifyStageFlowExample() {
  const sessionHome = loadYamlFixture('java-springboot/1_session_management_frontend/src/main/resources/workshop-content/views/session-home.yaml');
  const problemModel = createContentRenderModel(sessionHome, {
    activeStageId: 'problem',
    tokens: {
      sessionId: 'abc123',
      previousSessionId: 'old456',
      sessionIdChanged: true
    }
  });
  const enableRedisModel = createContentRenderModel(sessionHome, {
    activeStageId: 'enable-redis',
    tokens: {
      sessionId: 'abc123',
      previousSessionId: 'old456',
      sessionIdChanged: true
    }
  });

  assert.equal(problemModel.pageType, 'stage-flow');
  assert.equal(problemModel.stage.stageId, 'problem');
  assert.deepEqual(problemModel.errors, []);
  assert.deepEqual(problemModel.missingTokens, []);
  assert.equal(enableRedisModel.pageType, 'stage-flow');
  assert.equal(enableRedisModel.stage.stageId, 'enable-redis');
  assert.deepEqual(enableRedisModel.errors, []);
  assert.deepEqual(enableRedisModel.missingTokens, []);
  assert.equal(getStageNavItems(sessionHome).length, 3);
  assert.equal(problemModel.sections.length, 1);
  assert.equal(enableRedisModel.sections.length, 1);

  const problemBlocks = problemModel.sections[0].blocks;
  const enableRedisBlocks = enableRedisModel.sections[0].blocks;
  const statusPanel = problemBlocks.find(block => block.type === 'statusPanel');
  const optionCallout = enableRedisBlocks.find(block => block.type === 'callout');
  const orderedSteps = enableRedisBlocks.find(block => block.type === 'stepList');

  assert.ok(statusPanel);
  assert.equal(statusPanel.title, 'Current state');
  assert.ok(optionCallout);
  assert.equal(optionCallout.actions[0].id, 'openEditor');
  assert.ok(orderedSteps);
  assert.equal(orderedSteps.variant, 'ordered');
  assert.equal(orderedSteps.items.length, 4);
  assert.equal(orderedSteps.items[3].actions[0].id, 'rebuildRuntime');
}

function verifyNarrativeExample() {
  const locksImplement = loadYamlFixture('java-springboot/3_distributed_locks_frontend/src/main/resources/workshop-content/views/locks-implement.yaml');
  const renderModel = createContentRenderModel(locksImplement);

  assert.equal(renderModel.pageType, 'narrative');
  assert.deepEqual(renderModel.errors, []);
  assert.equal(renderModel.sections.length, 3);

  const implementationSection = renderModel.sections[1];
  const stepList = implementationSection.blocks.find(block => block.type === 'stepList');
  const verificationSection = renderModel.sections[2];
  const widget = verificationSection.blocks.find(block => block.type === 'widget');

  assert.ok(stepList);
  assert.equal(stepList.items[0].supportingBlocks[0].type, 'codeSnippet');
  assert.equal(stepList.items[4].actions[0].id, 'openHub');
  assert.ok(widget);
  assert.equal(widget.widgetId, 'locks-implement.status');
}

function verifyEditorExample() {
  const memoryEditor = loadYamlFixture('java-springboot/4_agent_memory_frontend/src/main/resources/workshop-content/views/memory-editor.yaml');
  const renderModel = createContentRenderModel(memoryEditor);

  assert.equal(renderModel.pageType, 'editor');
  assert.deepEqual(renderModel.errors, []);
  assert.ok(renderModel.sections.length >= 2);

  const serviceStepsSection = renderModel.sections.find(section => section.sectionId === 'service-steps');
  const firstEditorList = serviceStepsSection.blocks.find(block => block.type === 'editorStepList');

  assert.ok(firstEditorList);
  assert.equal(firstEditorList.title, 'Step 0: Initialize the SDK Client');
  assert.equal(firstEditorList.startAt, 1);
  assert.equal(firstEditorList.items.length, 3);
  assert.equal(firstEditorList.items[0].action.id, 'openFile');
  assert.equal(firstEditorList.items[1].action.id, 'applyEditorStep');
  assert.equal(firstEditorList.items[2].action.id, 'saveFile');
}

function verifyMarkdownRenderer() {
  const html = renderMarkdown('**Option 2:** edit `build.gradle.kts` and review [Redis Docs](https://redis.io/docs).');
  const relativeLinkHtml = renderMarkdown('Open [the learner app](/session/session-123/).');
  const unsafeRelativeLinkHtml = renderMarkdown('Bad [link](//evil.example.test).');

  assert.match(html, /<strong>Option 2:<\/strong>/);
  assert.match(html, /<code>build\.gradle\.kts<\/code>/);
  assert.match(html, /<a href="https:\/\/redis\.io\/docs"/);
  assert.match(relativeLinkHtml, /<a href="\/session\/session-123\/"/);
  assert.doesNotMatch(unsafeRelativeLinkHtml, /href=/);
}

function verifyMarkdownFirstContextFixture() {
  const renderModel = createContentRenderModel(markdownContentFixture, {
    context: markdownContextFixture
  });

  assert.equal(renderModel.pageType, 'narrative');
  assert.equal(renderModel.sections.length, 1);
  assert.deepEqual(renderModel.missingPlaceholders, ['status.detail']);
  assert.deepEqual(renderModel.unsupportedPlaceholders, ['links.adminPanel']);
  assert.deepEqual(renderModel.errors, ['Invalid dynamic markdown link for placeholder: links.redisInsight']);

  const blocks = renderModel.sections[0].blocks;
  assert.equal(blocks.length, 3);
  assert.equal(blocks[0].type, 'markdown');
  assert.equal(blocks[1].type, 'widget');
  assert.equal(blocks[1].widgetId, 'shared.session-status');
  assert.equal(blocks[2].type, 'markdown');

  const firstHtml = renderMarkdown(blocks[0].body);
  assert.match(firstHtml, /session-123&lt;script&gt;/);
  assert.match(firstHtml, /href="https:\/\/learner\.example\.test\/app\/session-123"/);
  assert.match(firstHtml, /<code>running<\/code>/);

  assert.match(blocks[2].body, /\{\{ links\.adminPanel \}\}/);
  assert.match(blocks[2].body, /\{\{ status\.detail \}\}/);
}

verifyStageFlowExample();
verifyNarrativeExample();
verifyEditorExample();
verifyMarkdownRenderer();
verifyMarkdownFirstContextFixture();

console.log('Content renderer smoke verification passed.');
