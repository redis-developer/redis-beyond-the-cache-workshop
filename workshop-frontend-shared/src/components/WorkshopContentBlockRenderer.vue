<template>
  <WorkshopMarkdownContent
    v-if="block.type === 'markdown'"
    class="content-block content-block--markdown"
    :body="block.body"
  />

  <article
    v-else-if="block.type === 'callout'"
    class="content-block content-callout"
    :class="`content-callout--${block.tone}`"
  >
    <h4 v-if="block.title" class="content-callout__title">{{ block.title }}</h4>
    <WorkshopMarkdownContent :body="block.body" />
    <div v-if="block.actions.length" class="content-action-row">
      <WorkshopContentAction
        v-for="action in block.actions"
        :key="`${action.id}-${action.label}`"
        :action="action"
        appearance="primary"
        @trigger="emitAction(action)"
      />
    </div>
  </article>

  <article v-else-if="block.type === 'codeSnippet'" class="content-block content-code-snippet">
    <div v-if="block.title" class="content-code-snippet__title">{{ block.title }}</div>
    <div v-if="block.language" class="content-code-snippet__language">{{ block.language }}</div>
    <pre><code>{{ block.code }}</code></pre>
    <WorkshopMarkdownContent v-if="block.caption" :body="block.caption" />
  </article>

  <div
    v-else-if="block.type === 'stepList'"
    class="content-block content-step-list"
    :class="`content-step-list--${block.variant}`"
  >
    <article
      v-for="(item, index) in block.items"
      :key="item.itemId"
      class="content-step-item"
    >
      <div class="content-step-item__header">
        <div
          v-if="block.variant !== 'checklist'"
          class="content-step-item__marker"
          :class="{ 'content-step-item__marker--tests': block.variant === 'tests' }"
        >
          {{ block.variant === 'tests' ? `T${index + 1}` : index + 1 }}
        </div>
        <div class="content-step-item__heading">
          <h4 v-if="item.title">{{ item.title }}</h4>
          <WorkshopMarkdownContent :body="item.body" />
        </div>
      </div>
      <div v-if="item.hint" class="content-step-item__hint">
        <WorkshopMarkdownContent :body="item.hint" />
      </div>
      <div v-if="item.actions.length" class="content-action-row">
        <WorkshopContentAction
          v-for="action in item.actions"
          :key="`${item.itemId}-${action.id}-${action.label}`"
          :action="action"
          appearance="secondary"
          @trigger="emitAction(action, item)"
        />
      </div>
      <div v-if="item.supportingBlocks.length" class="content-supporting-blocks">
        <WorkshopContentBlockRenderer
          v-for="(supportingBlock, supportingIndex) in item.supportingBlocks"
          :key="`${item.itemId}-supporting-${supportingIndex}`"
          :block="supportingBlock"
          :content="content"
          :section="section"
          :stage="stage"
          :widgets="widgets"
          :widget-props="widgetProps"
          @action="$emit('action', $event)"
        />
      </div>
    </article>
  </div>

  <div v-else-if="block.type === 'editorStepList'" class="content-block content-editor-step-list">
    <div class="content-editor-step-list__header">
      <h4 class="content-editor-step-list__title">{{ block.title }}</h4>
      <WorkshopMarkdownContent v-if="block.description" :body="block.description" />
    </div>
    <article
      v-for="(item, index) in block.items"
      :key="item.itemId"
      class="content-editor-step-item"
    >
      <div class="content-editor-step-item__number">
        {{ block.startAt + index }}
      </div>
      <div class="content-editor-step-item__body">
        <WorkshopMarkdownContent :body="item.body" />
        <div v-if="item.hint" class="content-editor-step-item__hint">
          <WorkshopMarkdownContent :body="item.hint" />
        </div>
      </div>
      <WorkshopContentAction
        v-if="item.action"
        :action="item.action"
        appearance="primary"
        @trigger="emitAction(item.action, item)"
      />
    </article>
  </div>

  <WorkshopContentWidget
    v-else-if="block.type === 'widget'"
    class="content-block"
    :widget="widgets[block.widgetId]"
    :widget-id="block.widgetId"
    :widget-props="widgetProps[block.widgetId] || {}"
    :context-props="{
      widgetId: block.widgetId,
      content,
      stage,
      section,
      block
    }"
  />
</template>

<script>
import WorkshopContentAction from './WorkshopContentAction.vue';
import WorkshopContentWidget from './WorkshopContentWidget.vue';
import WorkshopMarkdownContent from './WorkshopMarkdownContent.vue';

export default {
  name: 'WorkshopContentBlockRenderer',
  components: {
    WorkshopContentAction,
    WorkshopContentWidget,
    WorkshopMarkdownContent
  },
  props: {
    block: { type: Object, required: true },
    content: { type: Object, required: true },
    section: { type: Object, required: true },
    stage: { type: Object, default: null },
    widgets: { type: Object, default: () => ({}) },
    widgetProps: { type: Object, default: () => ({}) }
  },
  emits: ['action'],
  methods: {
    emitAction(action, item = null) {
      this.$emit('action', {
        action,
        actionId: action.id,
        args: action.args || {},
        item,
        block: this.block,
        section: this.section,
        stage: this.stage,
        content: this.content
      });
    }
  }
};
</script>
