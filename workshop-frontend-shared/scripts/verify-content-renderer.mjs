import assert from 'node:assert/strict';
import { execFileSync } from 'node:child_process';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

import {
  createContentRenderModel,
  getStageNavItems
} from '../src/content-renderer/renderModel.js';
import { renderMarkdown } from '../src/content-renderer/markdown.js';

const scriptDirectory = path.dirname(fileURLToPath(import.meta.url));
const repositoryRoot = path.resolve(scriptDirectory, '../..');
const examplesDirectory = path.join(repositoryRoot, 'plans/content-driven-views/examples');

function loadYamlFixture(fileName) {
  const filePath = path.join(examplesDirectory, fileName);
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
  const sessionHome = loadYamlFixture('session-home.yaml');
  const renderModel = createContentRenderModel(sessionHome, {
    activeStageId: 'enable-redis',
    tokens: {
      sessionId: 'abc123',
      previousSessionId: 'old456',
      sessionIdChanged: true
    }
  });

  assert.equal(renderModel.pageType, 'stage-flow');
  assert.equal(renderModel.stage.stageId, 'enable-redis');
  assert.deepEqual(renderModel.errors, []);
  assert.deepEqual(renderModel.missingTokens, []);
  assert.equal(getStageNavItems(sessionHome).length, 3);
  assert.equal(renderModel.sections.length, 1);

  const stageBlocks = renderModel.sections[0].blocks;
  const optionCallout = stageBlocks.find(block => block.type === 'callout');
  const orderedSteps = stageBlocks.find(block => block.type === 'stepList');

  assert.ok(optionCallout);
  assert.equal(optionCallout.actions[0].id, 'openEditor');
  assert.ok(orderedSteps);
  assert.equal(orderedSteps.variant, 'ordered');
  assert.equal(orderedSteps.items.length, 4);
  assert.equal(orderedSteps.items[3].actions[0].args.service, 'backend');
}

function verifyNarrativeExample() {
  const locksImplement = loadYamlFixture('locks-implement.yaml');
  const renderModel = createContentRenderModel(locksImplement);

  assert.equal(renderModel.pageType, 'narrative');
  assert.deepEqual(renderModel.errors, []);
  assert.equal(renderModel.sections.length, 2);

  const implementationSection = renderModel.sections[1];
  const stepList = implementationSection.blocks.find(block => block.type === 'stepList');
  const widget = implementationSection.blocks.find(block => block.type === 'widget');

  assert.ok(stepList);
  assert.equal(stepList.items[0].supportingBlocks[0].type, 'codeSnippet');
  assert.equal(stepList.items[3].actions[0].id, 'openReference');
  assert.ok(widget);
  assert.equal(widget.widgetId, 'locks-implement.reentrant-status');
}

function verifyEditorExample() {
  const memoryEditor = loadYamlFixture('memory-editor.yaml');
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

  assert.match(html, /<strong>Option 2:<\/strong>/);
  assert.match(html, /<code>build\.gradle\.kts<\/code>/);
  assert.match(html, /<a href="https:\/\/redis\.io\/docs"/);
}

verifyStageFlowExample();
verifyNarrativeExample();
verifyEditorExample();
verifyMarkdownRenderer();

console.log('Content renderer smoke verification passed.');
